package top.oneyi.envLauncher.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JdkEnvService extends AbstractEnvSetupService {
    private static final String JAVA_HOME = "JAVA_HOME";

    public JdkEnvService() {
        super();
    }

    JdkEnvService(WindowsEnvCommandService windowsEnvCommandService) {
        super(windowsEnvCommandService);
    }

    public String getConfiguredJdkPath() throws IOException {
        return windowsEnvCommandService.findPathEntryContaining("jdk", "java");
    }

    public EnvironmentSetupResult configureJdkEnvironment(String javaHome, String jdkBinPath) throws Exception {
        return configureEnvironment("JDK", Map.of(JAVA_HOME, javaHome), List.of(jdkBinPath), "jdk", "java");
    }

    @Override
    protected void applyMachineEnvironment(Map<String, String> machineVariables, String updatedPath) throws Exception {
        windowsEnvCommandService.setMachineEnvironmentVariable(JAVA_HOME, machineVariables.get(JAVA_HOME));
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
