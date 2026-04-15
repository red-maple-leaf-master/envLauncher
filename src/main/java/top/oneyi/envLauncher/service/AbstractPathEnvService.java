package top.oneyi.envLauncher.service;

import top.oneyi.envLauncher.utils.PathUtils;

import java.io.IOException;

/**
 * Shares PATH update behavior across environment services so PATH filtering
 * rules stay consistent when adding or replacing tool entries.
 */
public abstract class AbstractPathEnvService {

    protected final WindowsEnvCommandService windowsEnvCommandService = new WindowsEnvCommandService();
    private String cachedPath;

    protected void updateMachinePath(String pathHome, String... excludeKeywords) throws IOException {
        // Keep using the current process PATH as the merge base to preserve existing behavior.
        if (cachedPath == null) {
            cachedPath = System.getenv("PATH");
        }

        cachedPath = PathUtils.filterAndInsertPath(pathHome, cachedPath, excludeKeywords);
        windowsEnvCommandService.updateMachinePath(cachedPath);
    }
}
