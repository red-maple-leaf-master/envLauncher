package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;

import java.io.IOException;

public class MavenEnvService extends AbstractPathEnvService {

    public String getConfiguredMavenPath() throws IOException {
        return windowsEnvCommandService.findPathEntryContaining("maven");
    }

    public void configureMavenEnvironment(String mavenHome, String mavenBinPath) throws Exception {
        updateEnvironmentVariable("MAVEN_HOME", mavenHome);
        updateMachinePath(mavenBinPath, "maven");
        LoggerUtil.info("[MAVEN_HOME] related environment variables updated. Restart terminal or IDE to apply changes.");
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
