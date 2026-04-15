package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;

import java.io.IOException;

public class MavenEnvService extends AbstractPathEnvService {

    public String getConfiguredMavenPath() throws IOException {
        return windowsEnvCommandService.findPathEntryContaining("maven");
    }

    public void configureMavenEnvironment(String mavenHome, String mavenBinPath) throws Exception {
        windowsEnvCommandService.setMachineEnvironmentVariable("MAVEN_HOME", mavenHome);
        windowsEnvCommandService.setUserRegistryEnvironmentVariable("MAVEN_HOME", mavenHome);
        updateMachinePath(mavenBinPath, "maven");
        LoggerUtil.info("[MAVEN_HOME] related environment variables updated. Restart terminal or IDE to apply changes.");
    }
}
