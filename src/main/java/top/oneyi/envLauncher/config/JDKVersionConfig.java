package top.oneyi.envLauncher.config;

import java.util.HashMap;
import java.util.Map;

/**
 *  JDK版本配置
 */
public class JDKVersionConfig {
    private static final Map<String, String> VERSION_MAP = new HashMap<>();

    static {
        VERSION_MAP.put("8", "8/jdk/x64/windows/OpenJDK8U-jdk_x64_windows_hotspot_8u452b09.zip");
        VERSION_MAP.put("11", "11/jdk/x64/windows/OpenJDK11U-jdk_x64_windows_hotspot_11.0.27_6.zip");
        VERSION_MAP.put("17", "17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.15_6.zip");
        VERSION_MAP.put("21", "21/jdk/x64/windows/OpenJDK21U-jdk_x64_windows_hotspot_21.0.7_6.zip");
    }

    public static String getUrl(String version) {
        return VERSION_MAP.get(version);
    }
}
