package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;

import java.io.IOException;

public class JdkEnvService extends AbstractPathEnvService {

    public String getJdkEnvironmentVariables() throws IOException {
        return windowsEnvCommandService.readPathContaining("jdk", "java");
    }

    public void setJdkEnvironmentVariables(String javaHome, String jdkBinPath) throws Exception {
        windowsEnvCommandService.setMachineEnvironmentVariable("JAVA_HOME", javaHome);
        windowsEnvCommandService.setUserRegistryEnvironmentVariable("JAVA_HOME", javaHome);
        updateMachinePath(jdkBinPath, "jdk", "java");
        LoggerUtil.info("[JAVA_HOME] related environment variables updated. Restart terminal or IDE to apply changes.");
    }
}
