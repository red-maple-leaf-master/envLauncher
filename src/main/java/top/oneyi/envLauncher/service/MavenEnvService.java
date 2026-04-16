package top.oneyi.envLauncher.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MavenEnvService extends AbstractEnvSetupService {

    private static final String MAVEN_HOME = "MAVEN_HOME";

    public MavenEnvService() {
        super();
    }

    MavenEnvService(WindowsEnvCommandService windowsEnvCommandService) {
        super(windowsEnvCommandService);
    }

    public String getConfiguredMavenPath() throws IOException {
        return windowsEnvCommandService.findPathEntryContaining("maven");
    }

    public EnvironmentSetupResult configureMavenEnvironment(String mavenHome, String mavenBinPath) throws Exception {
        return configureEnvironment("Maven", Map.of(MAVEN_HOME, mavenHome), List.of(mavenBinPath), "maven");
    }

    @Override
    protected void applyMachineEnvironment(Map<String, String> machineVariables, String updatedPath) throws Exception {
        windowsEnvCommandService.setMachineEnvironmentVariable(MAVEN_HOME, machineVariables.get(MAVEN_HOME));
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
