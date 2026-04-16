package top.oneyi.envLauncher.service;

import javafx.application.Platform;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JdkEnvServiceTest {

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
    public void requestsElevationWhenMachineScopeNeedsAdminRights() throws Exception {
        FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
        commandService.elevated = false;
        commandService.elevationResult = WindowsEnvCommandService.ElevationResult.success();
        JdkEnvService service = new JdkEnvService(commandService);

        EnvironmentSetupResult result = service.configureJdkEnvironment("C:\\env\\jdk-17", "%JAVA_HOME%\\bin");

        assertTrue("environment setup should complete after elevation succeeds", result.isCompleted());
        assertTrue("result should record that UAC elevation was used", result.usedElevation());
        assertEquals("completed", result.status());
        assertEquals("machine JAVA_HOME should be written by elevated process", "C:\\env\\jdk-17",
                commandService.elevatedJavaHome);
        assertTrue("current user JAVA_HOME should also be synced after elevation",
                commandService.userVariableUpdated);
        assertTrue("current user PATH should also be synced after elevation",
                commandService.userPathUpdated);
    }

    @Test
    public void broadcastsEnvironmentRefreshAfterSuccessfulSetup() throws Exception {
        FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
        commandService.elevated = false;
        commandService.elevationResult = WindowsEnvCommandService.ElevationResult.success();
        JdkEnvService service = new JdkEnvService(commandService);

        EnvironmentSetupResult result = service.configureJdkEnvironment("C:\\env\\jdk-17", "%JAVA_HOME%\\bin");

        assertTrue(result.isCompleted());
        assertTrue("successful environment setup should notify Windows about the updated environment",
                commandService.environmentRefreshBroadcasted);
    }

    @Test
    public void reportsCancellationWhenElevationIsDeclined() throws Exception {
        FakeWindowsEnvCommandService commandService = new FakeWindowsEnvCommandService();
        commandService.elevated = false;
        commandService.elevationResult = WindowsEnvCommandService.ElevationResult.cancelled("UAC cancelled");
        JdkEnvService service = new JdkEnvService(commandService);

        EnvironmentSetupResult result = service.configureJdkEnvironment("C:\\env\\jdk-17", "%JAVA_HOME%\\bin");

        assertFalse("environment setup should stay incomplete when UAC is cancelled", result.isCompleted());
        assertEquals("cancelled", result.status());
        assertFalse("user-scope fallback should not happen after UAC cancellation", commandService.userVariableUpdated);
        assertFalse("user-scope PATH fallback should not happen after UAC cancellation", commandService.userPathUpdated);
    }

    private static final class FakeWindowsEnvCommandService extends WindowsEnvCommandService {
        private boolean elevated;
        private boolean userVariableUpdated;
        private boolean userPathUpdated;
        private boolean environmentRefreshBroadcasted;
        private String elevatedJavaHome;
        private final Map<String, String> elevatedMachineVariables = new LinkedHashMap<>();
        private ElevationResult elevationResult = ElevationResult.success();

        @Override
        public boolean isProcessElevated() {
            return elevated;
        }

        @Override
        public String setMachineEnvironmentVariable(String variableName, String variableValue) {
            elevatedMachineVariables.put(variableName, variableValue);
            elevatedJavaHome = variableValue;
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
        public void broadcastEnvironmentChange() {
            environmentRefreshBroadcasted = true;
        }

        @Override
        public ElevationResult applyMachineEnvironmentWithElevation(Map<String, String> variables, String pathValue) {
            elevatedMachineVariables.putAll(variables);
            elevatedJavaHome = variables.get("JAVA_HOME");
            return elevationResult;
        }
    }
}
