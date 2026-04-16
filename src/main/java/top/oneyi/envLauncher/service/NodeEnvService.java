package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class NodeEnvService extends AbstractEnvSetupService {
    private static final String NODE_HOME = "NODE_HOME";

    public NodeEnvService() {
        super();
    }

    NodeEnvService(WindowsEnvCommandService windowsEnvCommandService) {
        super(windowsEnvCommandService);
    }

    public EnvironmentSetupResult configureNodeEnvironment(String nodeHome, String nodePathEntry) throws Exception {
        String globalInstallPath = nodeHome + "\\node_global";
        String cachePath = nodeHome + "\\node_cache";
        if (!new File(globalInstallPath).exists()) {
            new File(globalInstallPath).mkdirs();
        }
        if (!new File(cachePath).exists()) {
            new File(cachePath).mkdirs();
        }

        EnvironmentSetupResult envResult = configureEnvironment(
                "Node",
                Map.of(NODE_HOME, nodeHome),
                List.of(nodePathEntry, globalInstallPath, cachePath)
        );
        if (!envResult.isCompleted()) {
            LoggerUtil.info("Node environment setup incomplete. Skip npm configuration.");
            return envResult;
        }

        configureNpmPaths(cachePath, globalInstallPath);
        return envResult;
    }

    public void configureNpmPaths(String cachePath, String globalInstallPath) throws IOException {
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
            // Keep failures visible in the unified log because npm config runs after PATH changes.
            LoggerUtil.info("Node NPM config failed: " + e.getMessage());
            throw new IOException("Failed to configure npm.", e);
        }
    }

    @Override
    protected void applyMachineEnvironment(Map<String, String> machineVariables, String updatedPath) throws Exception {
        windowsEnvCommandService.setMachineEnvironmentVariable(NODE_HOME, machineVariables.get(NODE_HOME));
        windowsEnvCommandService.updateMachinePath(updatedPath);
    }

    @Override
    protected WindowsEnvCommandService.ElevationResult applyMachineEnvironmentWithElevation(
            Map<String, String> machineVariables,
            String updatedPath
    ) throws Exception {
        return windowsEnvCommandService.applyMachineEnvironmentWithElevation(machineVariables, updatedPath);
    }
}
