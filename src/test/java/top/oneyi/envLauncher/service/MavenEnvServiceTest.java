package top.oneyi.envLauncher.service;

import javafx.application.Platform;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenEnvServiceTest {

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
    public void requestsElevationWhenMachineEnvironmentNeedsAdminRights() throws Exception {
        FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
        commandService.elevated = false;
        commandService.elevationResult = WindowsEnvCommandService.ElevationResult.success();
        MavenEnvService service = new MavenEnvService(commandService);

        EnvironmentSetupResult result = service.configureMavenEnvironment("C:\\env\\maven-3.9.10", "C:\\env\\maven-3.9.10\\bin");

        assertTrue(result.isCompleted());
        assertTrue(result.usedElevation());
        assertEquals("completed", result.status());
        assertEquals("C:\\env\\maven-3.9.10", commandService.elevatedMachineVariables.get("MAVEN_HOME"));
        assertTrue(commandService.userVariableUpdated);
        assertTrue(commandService.userPathUpdated);
    }

    @Test
    public void reportsCancellationWhenElevationIsDeclined() throws Exception {
        FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
        commandService.elevated = false;
        commandService.elevationResult = WindowsEnvCommandService.ElevationResult.cancelled("UAC cancelled");
        MavenEnvService service = new MavenEnvService(commandService);

        EnvironmentSetupResult result = service.configureMavenEnvironment("C:\\env\\maven-3.9.10", "C:\\env\\maven-3.9.10\\bin");

        assertFalse(result.isCompleted());
        assertEquals("cancelled", result.status());
        assertFalse(commandService.userVariableUpdated);
        assertFalse(commandService.userPathUpdated);
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
