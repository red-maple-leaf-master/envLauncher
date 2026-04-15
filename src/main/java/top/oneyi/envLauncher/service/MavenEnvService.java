package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;

import java.io.IOException;

public class MavenEnvService extends AbstractPathEnvService {

    public String getConfiguredMavenPath() throws IOException {
        return windowsEnvCommandService.findPathEntryContaining("maven");
    }

    public void configureMavenEnvironment(String mavenHome, String mavenBinPath) throws Exception {
        EnvScope variableScope = updateEnvironmentVariable("MAVEN_HOME", mavenHome);
        EnvScope pathScope = updatePath(mavenBinPath, "maven");
        LoggerUtil.info(buildScopeMessage("MAVEN_HOME", variableScope, pathScope));
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
