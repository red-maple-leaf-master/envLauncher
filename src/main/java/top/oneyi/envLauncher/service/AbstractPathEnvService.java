package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.IOException;

/**
 * Shares PATH update behavior across environment services so PATH filtering
 * rules stay consistent when adding or replacing tool entries.
 */
public abstract class AbstractPathEnvService {

    protected final WindowsEnvCommandService windowsEnvCommandService = new WindowsEnvCommandService();
    private String cachedPath;

    protected void updateMachinePath(String pathEntry, String... excludeKeywords) throws IOException {
        // Keep using the current process PATH as the merge base to preserve existing behavior.
        if (cachedPath == null) {
            cachedPath = System.getenv("PATH");
        }

        cachedPath = PathUtils.filterAndInsertPath(pathEntry, cachedPath, excludeKeywords);
        try {
            windowsEnvCommandService.updateMachinePath(cachedPath);
        } catch (IOException e) {
            // Fall back to the current user when the app is not running with machine-level registry privileges.
            LoggerUtil.info("Machine PATH update failed, fallback to user PATH: " + e.getMessage());
            windowsEnvCommandService.updateUserPath(cachedPath);
        }
    }
}
