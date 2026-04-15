package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.CmdUtil;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Centralizes Windows environment mutation commands so component services do not
 * need to know about setx or registry details.
 */
public class WindowsEnvCommandService {

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
                Charset.forName("GBK")
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
                Charset.forName("GBK")
        );
    }

    public void updateUserPath(String pathValue) throws IOException {
        CmdUtil.executeCommand(
                new String[]{
                        "cmd.exe", "/c", "reg", "add",
                        "HKCU\\Environment",
                        "/v", "Path", "/t", "REG_EXPAND_SZ", "/d", "\"" + pathValue + "\"", "/f"
                },
                Charset.forName("GBK")
        );
    }
}
