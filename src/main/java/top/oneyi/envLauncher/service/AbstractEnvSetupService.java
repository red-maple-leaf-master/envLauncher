package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Shares the "prompt for elevation, then keep machine and user env aligned" flow
 * so each tool service only defines its own variables and path entries.
 */
public abstract class AbstractEnvSetupService extends AbstractPathEnvService {

    protected AbstractEnvSetupService() {
        super();
    }

    protected AbstractEnvSetupService(WindowsEnvCommandService windowsEnvCommandService) {
        super(windowsEnvCommandService);
    }

    protected EnvironmentSetupResult configureEnvironment(String toolName,
                                                          Map<String, String> machineVariables,
                                                          List<String> pathEntries,
                                                          String... excludeKeywords) throws Exception {
        String updatedPath = null;
        for (String pathEntry : pathEntries) {
            updatedPath = buildUpdatedPathValue(pathEntry, excludeKeywords);
        }
        if (updatedPath == null) {
            updatedPath = buildUpdatedPathValue("", excludeKeywords);
        }

        if (windowsEnvCommandService.isProcessElevated()) {
            applyMachineAndUserEnvironment(machineVariables, updatedPath);
            String message = buildSuccessMessage(machineVariables, false);
            LoggerUtil.info(message);
            return EnvironmentSetupResult.completed("completed", false, message);
        }

        LoggerUtil.info(toolName + " environment update requires administrator permission. Requesting elevation...");
        WindowsEnvCommandService.ElevationResult elevationResult =
                applyMachineEnvironmentWithElevation(machineVariables, updatedPath);
        if (elevationResult.successful()) {
            syncUserEnvironment(machineVariables, updatedPath);
            String message = buildSuccessMessage(machineVariables, true);
            LoggerUtil.info(message);
            return EnvironmentSetupResult.completed("completed", true, message);
        }

        if (elevationResult.cancelled()) {
            String message = "Administrator permission was cancelled. " + toolName
                    + " files are installed, but environment variables were not updated.";
            LoggerUtil.info(message);
            return EnvironmentSetupResult.incomplete("cancelled", true, message);
        }

        String message = "Administrator environment update failed: " + elevationResult.message();
        LoggerUtil.info(message);
        return EnvironmentSetupResult.incomplete("failed", true, message);
    }

    protected void applyMachineAndUserEnvironment(Map<String, String> machineVariables, String updatedPath) throws Exception {
        // Keep machine and current-user registry values aligned so future shells resolve the same tool path.
        applyMachineEnvironment(machineVariables, updatedPath);
        syncUserEnvironment(machineVariables, updatedPath);
    }

    protected void syncUserEnvironment(Map<String, String> machineVariables, String updatedPath) throws Exception {
        for (Map.Entry<String, String> entry : machineVariables.entrySet()) {
            windowsEnvCommandService.setUserRegistryEnvironmentVariable(entry.getKey(), entry.getValue());
        }
        windowsEnvCommandService.updateUserPath(updatedPath);
    }

    protected String buildSuccessMessage(Map<String, String> machineVariables, boolean usedElevation) {
        String variableSummary = formatVariableSummary(machineVariables);
        if (usedElevation) {
            return variableSummary + " and PATH were updated after administrator approval. Restart terminal or IDE to apply changes.";
        }
        return variableSummary + " and PATH were updated in system environment variables. Restart terminal or IDE to apply changes.";
    }

    private String formatVariableSummary(Map<String, String> machineVariables) {
        StringJoiner joiner = new StringJoiner("], [", "[", "]");
        for (String variableName : machineVariables.keySet()) {
            joiner.add(variableName);
        }
        return joiner.toString();
    }

    protected abstract void applyMachineEnvironment(Map<String, String> machineVariables, String updatedPath) throws Exception;

    protected abstract WindowsEnvCommandService.ElevationResult applyMachineEnvironmentWithElevation(
            Map<String, String> machineVariables,
            String updatedPath
    ) throws Exception;
}
