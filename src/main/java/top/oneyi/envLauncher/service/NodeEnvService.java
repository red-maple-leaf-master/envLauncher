package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.File;
import java.io.IOException;

public class NodeEnvService {

    private final WindowsEnvCommandService windowsEnvCommandService = new WindowsEnvCommandService();
    private String globalPath;

    public void setNodeEnvironmentVariables(String nodePath, String nodeHome) throws Exception {
        String globalInstallPath = nodePath + "\\node_global";
        String cachePath = nodePath + "\\node_cache";
        if (!new File(globalInstallPath).exists()) {
            new File(globalInstallPath).mkdirs();
        }
        if (!new File(cachePath).exists()) {
            new File(cachePath).mkdirs();
        }

        windowsEnvCommandService.setMachineEnvironmentVariable("NODE_HOME", nodePath);
        windowsEnvCommandService.setUserRegistryEnvironmentVariable("NODE_HOME", nodePath);

        setPath(nodeHome);
        setPath(globalInstallPath);
        setPath(cachePath);
        setNpmConfig(cachePath, globalInstallPath);

        LoggerUtil.info("Node related environment variables updated. Restart terminal or IDE to apply changes.");
    }

    public void setNpmConfig(String cachePath, String globalInstallPath) throws IOException {
        try {
            String cacheCommand = "npm config set cache \"" + cachePath + "\" --location=global";
            String cacheResult = windowsEnvCommandService.executeCommand(cacheCommand);
            LoggerUtil.info("NPM cache path updated.\n" + cacheResult);

            String prefixCommand = "npm config set prefix \"" + globalInstallPath + "\" --location=global";
            String prefixResult = windowsEnvCommandService.executeCommand(prefixCommand);
            LoggerUtil.info("NPM global install path updated.\n" + prefixResult);

            String registryCommand = "npm config set registry https://registry.npm.taobao.org/";
            String registryResult = windowsEnvCommandService.executeCommand(registryCommand);
            LoggerUtil.info("NPM registry updated.\n" + registryResult);

            windowsEnvCommandService.executeCommand("npm config set strict-ssl false");
            LoggerUtil.info("NPM strict SSL disabled. Installing cnpm...");
            windowsEnvCommandService.executeCommand("npm install cnpm@7.1.1 -g");
            LoggerUtil.info("NPM cnpm install completed.");
        } catch (Exception e) {
            System.err.println("Node NPM config failed: " + e.getMessage());
            e.printStackTrace();
        }
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
