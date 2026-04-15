package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.IOException;

public class MavenEnvService {

    private final WindowsEnvCommandService windowsEnvCommandService = new WindowsEnvCommandService();
    private String globalPath;

    public String getMavenEnvironmentVariables() throws IOException {
        return windowsEnvCommandService.readPathContaining("maven");
    }

    public void setMavenEnvironmentVariables(String mavenHome, String mavenBinPath) throws Exception {
        windowsEnvCommandService.setMachineEnvironmentVariable("MAVEN_HOME", mavenHome);
        windowsEnvCommandService.setUserRegistryEnvironmentVariable("MAVEN_HOME", mavenHome);
        setPath(mavenBinPath, "maven");
        LoggerUtil.info("[MAVEN_HOME] related environment variables updated. Restart terminal or IDE to apply changes.");
    }

    private void setPath(String pathHome, String... excludeKeywords) throws IOException {
        // Keep using the current process PATH as the merge base to preserve the previous behavior.
        if (globalPath == null) {
            globalPath = System.getenv("PATH");
        }

        globalPath = PathUtils.filterAndInsertPath(pathHome, globalPath, excludeKeywords);
        windowsEnvCommandService.updateMachinePath(globalPath);
    }
}
