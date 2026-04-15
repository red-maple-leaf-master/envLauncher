package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.IOException;

public class JdkEnvService {

    private final WindowsEnvCommandService windowsEnvCommandService = new WindowsEnvCommandService();
    private String globalPath;

    public String getJdkEnvironmentVariables() throws IOException {
        return windowsEnvCommandService.readPathContaining("jdk", "java");
    }

    public void setJdkEnvironmentVariables(String javaHome, String jdkBinPath) throws Exception {
        windowsEnvCommandService.setMachineEnvironmentVariable("JAVA_HOME", javaHome);
        windowsEnvCommandService.setUserRegistryEnvironmentVariable("JAVA_HOME", javaHome);
        setPath(jdkBinPath, "jdk", "java");
        LoggerUtil.info("[JAVA_HOME] related environment variables updated. Restart terminal or IDE to apply changes.");
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
