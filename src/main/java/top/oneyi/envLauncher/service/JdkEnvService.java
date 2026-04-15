package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;

import java.io.IOException;

public class JdkEnvService extends AbstractPathEnvService {

    public String getConfiguredJdkPath() throws IOException {
        return windowsEnvCommandService.findPathEntryContaining("jdk", "java");
    }

    public void configureJdkEnvironment(String javaHome, String jdkBinPath) throws Exception {
        EnvScope variableScope = updateEnvironmentVariable("JAVA_HOME", javaHome);
        EnvScope pathScope = updatePath(jdkBinPath, "jdk", "java");
        LoggerUtil.info(buildScopeMessage("JAVA_HOME", variableScope, pathScope));
    }

    private EnvScope updateEnvironmentVariable(String variableName, String variableValue) throws Exception {
        try {
            windowsEnvCommandService.setMachineEnvironmentVariable(variableName, variableValue);
            windowsEnvCommandService.setUserRegistryEnvironmentVariable(variableName, variableValue);
            return EnvScope.MACHINE;
        } catch (IOException e) {
            LoggerUtil.info("System " + variableName + " update failed, fallback to current user scope: " + e.getMessage());
            windowsEnvCommandService.setUserRegistryEnvironmentVariable(variableName, variableValue);
            return EnvScope.USER;
        }
    }

    private String buildScopeMessage(String variableName, EnvScope variableScope, EnvScope pathScope) {
        if (variableScope == EnvScope.MACHINE && pathScope == EnvScope.MACHINE) {
            return "[" + variableName + "] and PATH were updated in system environment variables. Restart terminal or IDE to apply changes.";
        }
        return "[" + variableName + "] and PATH were updated in current user environment variables. Restart terminal or IDE to apply changes.";
    }
}
