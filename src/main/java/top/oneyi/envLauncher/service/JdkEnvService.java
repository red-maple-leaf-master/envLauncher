package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;

import java.io.IOException;

public class JdkEnvService extends AbstractPathEnvService {

    public String getConfiguredJdkPath() throws IOException {
        return windowsEnvCommandService.findPathEntryContaining("jdk", "java");
    }

    public void configureJdkEnvironment(String javaHome, String jdkBinPath) throws Exception {
        updateEnvironmentVariable("JAVA_HOME", javaHome);
        updateMachinePath(jdkBinPath, "jdk", "java");
        LoggerUtil.info("[JAVA_HOME] related environment variables updated. Restart terminal or IDE to apply changes.");
    }

    private void updateEnvironmentVariable(String variableName, String variableValue) throws Exception {
        try {
            windowsEnvCommandService.setMachineEnvironmentVariable(variableName, variableValue);
        } catch (IOException e) {
            LoggerUtil.info("Machine " + variableName + " update failed, fallback to user scope: " + e.getMessage());
        }
        windowsEnvCommandService.setUserRegistryEnvironmentVariable(variableName, variableValue);
    }
}
