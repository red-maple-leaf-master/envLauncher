package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.CmdUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Centralizes Windows environment mutation commands so component services do not
 * need to know about setx or registry details.
 */
public class WindowsEnvCommandService {
    private static final Charset WINDOWS_CHARSET = Charset.forName("GBK");
    private static final int UAC_CANCELLED_EXIT_CODE = 1223;

    public String findPathEntryContaining(String... keywords) throws IOException {
        String pathValue = CmdUtil.executeCmdCommand("echo %PATH%");
        for (String path : pathValue.split(";")) {
            for (String keyword : keywords) {
                if (path.toLowerCase().contains(keyword.toLowerCase())) {
                    return path;
                }
            }
        }
        return null;
    }

    public String setMachineEnvironmentVariable(String variableName, String variableValue) throws IOException {
        return CmdUtil.executeCmdCommand("setx " + variableName + " \"" + variableValue + "\" /M");
    }

    public String executeCommand(String command) throws IOException {
        return CmdUtil.executeCmdCommand(command);
    }

    public void setUserRegistryEnvironmentVariable(String variableName, String variableValue) throws Exception {
        CmdUtil.executeCommand(
                new String[]{"reg", "add", "HKCU\\Environment", "/v", variableName, "/d", variableValue, "/f"},
                WINDOWS_CHARSET
        );
    }

    public void updateMachinePath(String pathValue) throws IOException {
        // Use REG_EXPAND_SZ so entries like %JAVA_HOME%\bin remain expandable after being written.
        CmdUtil.executeCommand(
                new String[]{
                        "cmd.exe", "/c", "reg", "add",
                        "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment",
                        "/v", "Path", "/t", "REG_EXPAND_SZ", "/d", "\"" + pathValue + "\"", "/f"
                },
                WINDOWS_CHARSET
        );
    }

    public void updateUserPath(String pathValue) throws IOException {
        CmdUtil.executeCommand(
                new String[]{
                        "cmd.exe", "/c", "reg", "add",
                        "HKCU\\Environment",
                        "/v", "Path", "/t", "REG_EXPAND_SZ", "/d", "\"" + pathValue + "\"", "/f"
                },
                WINDOWS_CHARSET
        );
    }

    public void broadcastEnvironmentChange() throws IOException {
        CmdUtil.executeCommand(
                new String[]{"rundll32.exe", "user32.dll,UpdatePerUserSystemParameters"},
                WINDOWS_CHARSET
        );
    }

    public boolean isProcessElevated() throws IOException {
        String output = CmdUtil.executeCommand(
                new String[]{
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-Command",
                        "[bool](([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator))"
                },
                WINDOWS_CHARSET
        );
        return "true".equalsIgnoreCase(output.trim());
    }

    public ElevationResult applyMachineEnvironmentWithElevation(Map<String, String> variables, String pathValue) throws IOException {
        StringBuilder writeVariablesScript = new StringBuilder();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            writeVariablesScript
                    .append("$envKey.SetValue(")
                    .append(toPowerShellSingleQuotedLiteral(entry.getKey()))
                    .append(", ")
                    .append(toPowerShellSingleQuotedLiteral(entry.getValue()))
                    .append(", [Microsoft.Win32.RegistryValueKind]::String); ");
        }
        String innerScript = "& { "
                + "$envKey = [Microsoft.Win32.Registry]::LocalMachine.OpenSubKey('SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment', $true); "
                + "if ($null -eq $envKey) { throw 'Failed to open machine environment registry key.' } "
                + "try { "
                + writeVariablesScript
                + "$envKey.SetValue('Path', " + toPowerShellSingleQuotedLiteral(pathValue) + ", [Microsoft.Win32.RegistryValueKind]::ExpandString); "
                + "} finally { $envKey.Close() } "
                + "}";
        String outerScript = "$ErrorActionPreference='Stop'; "
                + "try { "
                + "$process = Start-Process -FilePath 'powershell.exe' "
                + "-ArgumentList @('-NoProfile','-NonInteractive','-Command',"
                + toPowerShellSingleQuotedLiteral(innerScript)
                + ") -Verb RunAs -WindowStyle Hidden -Wait -PassThru; "
                + "exit $process.ExitCode "
                + "} catch { "
                + "$message = $_.Exception.Message; "
                + "if ($message -like '*cancelled by the user*' -or $message -like '*已被用户取消*' -or $message -like '*由于用户取消了操作*' -or $message -like '*The operation was canceled by the user*') { exit "
                + UAC_CANCELLED_EXIT_CODE
                + " } "
                + "Write-Error $message; "
                + "exit 1 "
                + "}";

        CmdUtil.ProcessResult result = CmdUtil.executeCommandForResult(
                new String[]{"powershell.exe", "-NoProfile", "-NonInteractive", "-Command", outerScript},
                WINDOWS_CHARSET
        );
        if (result.exitCode() == 0) {
            return ElevationResult.success();
        }
        if (result.exitCode() == UAC_CANCELLED_EXIT_CODE) {
            return ElevationResult.cancelled("Administrator permission was cancelled.");
        }
        String output = result.output().trim();
        if (output.isBlank()) {
            output = "Administrator command exited with code " + result.exitCode() + ".";
        }
        return ElevationResult.failed(output);
    }

    public ElevationResult applyMachineJdkEnvironmentWithElevation(String javaHome, String pathValue) throws IOException {
        return applyMachineEnvironmentWithElevation(Map.of("JAVA_HOME", javaHome), pathValue);
    }

    private String toPowerShellSingleQuotedLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    public static final class ElevationResult {
        private final boolean successful;
        private final boolean cancelled;
        private final String message;

        private ElevationResult(boolean successful, boolean cancelled, String message) {
            this.successful = successful;
            this.cancelled = cancelled;
            this.message = message;
        }

        public static ElevationResult success() {
            return new ElevationResult(true, false, "Administrator environment update succeeded.");
        }

        public static ElevationResult cancelled(String message) {
            return new ElevationResult(false, true, message);
        }

        public static ElevationResult failed(String message) {
            return new ElevationResult(false, false, message);
        }

        public boolean successful() {
            return successful;
        }

        public boolean cancelled() {
            return cancelled;
        }

        public String message() {
            return message;
        }
    }
}
