package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.IOException;

/**
 * Shares PATH update behavior across environment services so PATH filtering
 * rules stay consistent when adding or replacing tool entries.
 */
public abstract class AbstractPathEnvService {

    protected enum EnvScope {
        MACHINE,
        USER
    }

    protected final WindowsEnvCommandService windowsEnvCommandService;
    private String cachedPath;

    protected AbstractPathEnvService() {
        this(new WindowsEnvCommandService());
    }

    protected AbstractPathEnvService(WindowsEnvCommandService windowsEnvCommandService) {
        this.windowsEnvCommandService = windowsEnvCommandService;
    }

    protected String buildUpdatedPathValue(String pathEntry, String... excludeKeywords) {
        // Keep using the current process PATH as the merge base to preserve existing behavior.
        if (cachedPath == null) {
            cachedPath = System.getenv("PATH");
        }

        cachedPath = PathUtils.filterAndInsertPath(pathEntry, cachedPath, excludeKeywords);
        return cachedPath;
    }

    protected EnvScope updatePath(String pathEntry, String... excludeKeywords) throws IOException {
        String updatedPath = buildUpdatedPathValue(pathEntry, excludeKeywords);
        try {
            windowsEnvCommandService.updateMachinePath(updatedPath);
            return EnvScope.MACHINE;
        } catch (IOException e) {
            // Fall back to the current user when the app is not running with machine-level registry privileges.
            LoggerUtil.info("System PATH update failed, fallback to current user PATH: " + e.getMessage());
            windowsEnvCommandService.updateUserPath(updatedPath);
            return EnvScope.USER;
        }
    }
}
