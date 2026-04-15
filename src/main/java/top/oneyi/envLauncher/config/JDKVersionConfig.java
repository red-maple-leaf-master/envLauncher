package top.oneyi.envLauncher.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps supported JDK versions to their archive paths when the template-based URL is not used.
 */
public class JdkVersionConfig {
    private static final Map<String, String> VERSION_TO_ARTIFACT_PATH = new HashMap<>();

    static {
        VERSION_TO_ARTIFACT_PATH.put("8", "8/jdk/x64/windows/OpenJDK8U-jdk_x64_windows_hotspot_8u452b09.zip");
        VERSION_TO_ARTIFACT_PATH.put("11", "11/jdk/x64/windows/OpenJDK11U-jdk_x64_windows_hotspot_11.0.27_6.zip");
        VERSION_TO_ARTIFACT_PATH.put("17", "17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.15_6.zip");
        VERSION_TO_ARTIFACT_PATH.put("21", "21/jdk/x64/windows/OpenJDK21U-jdk_x64_windows_hotspot_21.0.7_6.zip");
    }

    public static String getArtifactPath(String version) {
        return VERSION_TO_ARTIFACT_PATH.get(version);
    }

    /**
     * Keep backward compatibility for old callers.
     */
    public static String getUrl(String version) {
        return getArtifactPath(version);
    }
}
