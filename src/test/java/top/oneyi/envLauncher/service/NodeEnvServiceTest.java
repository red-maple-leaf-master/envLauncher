package top.oneyi.envLauncher.service;

import javafx.application.Platform;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeEnvServiceTest {

    @BeforeClass
    public static void initToolkit() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit may already be initialized by another test.
        }
    }

    @Test
    public void skipsNpmConfigurationWhenElevationIsCancelled() throws Exception {
        FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
        commandService.elevated = false;
        commandService.elevationResult = WindowsEnvCommandService.ElevationResult.cancelled("UAC cancelled");
        RecordingNodeEnvService service = new RecordingNodeEnvService(commandService);

        EnvironmentSetupResult result = service.configureNodeEnvironment("C:\\env\\node-v20", "%NODE_HOME%");

        assertFalse(result.isCompleted());
        assertEquals("cancelled", result.status());
        assertFalse(service.npmConfigured);
        assertFalse(commandService.userVariableUpdated);
        assertFalse(commandService.userPathUpdated);
    }

    @Test
    public void runsNpmConfigurationAfterElevationSucceeds() throws Exception {
        FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
        commandService.elevated = false;
        commandService.elevationResult = WindowsEnvCommandService.ElevationResult.success();
        RecordingNodeEnvService service = new RecordingNodeEnvService(commandService);

        EnvironmentSetupResult result = service.configureNodeEnvironment("C:\\env\\node-v20", "%NODE_HOME%");

        assertTrue(result.isCompleted());
        assertTrue(result.usedElevation());
        assertTrue(service.npmConfigured);
        assertEquals("C:\\env\\node-v20", commandService.elevatedMachineVariables.get("NODE_HOME"));
    }

    private static final class RecordingNodeEnvService extends NodeEnvService {
        private boolean npmConfigured;

        private RecordingNodeEnvService(WindowsEnvCommandService windowsEnvCommandService) {
            super(windowsEnvCommandService);
        }

        @Override
        public void configureNpmPaths(String cachePath, String globalInstallPath) {
            npmConfigured = true;
        }
    }

    static final class FakeWindowsEnvCommandService extends WindowsEnvCommandService {
        boolean elevated;
        boolean userVariableUpdated;
        boolean userPathUpdated;
        Map<String, String> elevatedMachineVariables = new LinkedHashMap<>();
        ElevationResult elevationResult = ElevationResult.success();

        @Override
        public boolean isProcessElevated() {
            return elevated;
        }

        @Override
        public String setMachineEnvironmentVariable(String variableName, String variableValue) {
            elevatedMachineVariables.put(variableName, variableValue);
            return "ok";
        }

        @Override
        public void setUserRegistryEnvironmentVariable(String variableName, String variableValue) {
            userVariableUpdated = true;
        }

        @Override
        public void updateMachinePath(String pathValue) {
        }

        @Override
        public void updateUserPath(String pathValue) {
            userPathUpdated = true;
        }

        @Override
        public ElevationResult applyMachineEnvironmentWithElevation(Map<String, String> variables, String pathValue) {
            elevatedMachineVariables.putAll(variables);
            return elevationResult;
        }
    }
}
