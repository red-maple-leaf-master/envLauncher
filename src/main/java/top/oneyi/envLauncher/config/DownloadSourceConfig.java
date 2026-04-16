package top.oneyi.envLauncher.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Centralized download source configuration.
 * Priority:
 * 1) JVM property envlauncher.sources (external file path)
 * 2) ./download-sources.properties (project/app current directory)
 * 3) classpath /download-sources.properties
 */
public final class DownloadSourceConfig {

    private static final String KEY_JDK_BASE = "jdk.base-url";
    private static final String KEY_JDK_URL_TEMPLATE = "jdk.url-template";
    private static final String KEY_MAVEN_BASE = "maven.base-url";
    private static final String KEY_NODE_BASE = "node.base-url";

    private static final String DEFAULT_JDK_BASE = "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/";
    private static final String DEFAULT_JDK_URL_TEMPLATE =
            "https://api.adoptium.net/v3/binary/latest/{version}/ga/windows/x64/jdk/hotspot/normal/eclipse";
    private static final String DEFAULT_MAVEN_BASE = "https://archive.apache.org/dist/maven/maven-3/";
    private static final String DEFAULT_NODE_BASE = "https://npmmirror.com/mirrors/node/";

    private static final String LOCAL_FILE_NAME = "download-sources.properties";
    private static final String SYS_PROP_PATH = "envlauncher.sources";

    private static volatile Properties cached;

    private DownloadSourceConfig() {
    }

    public static synchronized void reload() {
        cached = loadProperties();
    }

    public static Path getLocalOverridePath() {
        return new File(System.getProperty("user.dir"), LOCAL_FILE_NAME).toPath();
    }

    public static String getJdkBaseUrl() {
        return normalizeBaseUrl(get(KEY_JDK_BASE, DEFAULT_JDK_BASE));
    }

    public static String getMavenBaseUrl() {
        return normalizeBaseUrl(get(KEY_MAVEN_BASE, DEFAULT_MAVEN_BASE));
    }

    public static String getNodeBaseUrl() {
        return normalizeBaseUrl(get(KEY_NODE_BASE, DEFAULT_NODE_BASE));
    }

    public static String getJdkUrlTemplate() {
        return get(KEY_JDK_URL_TEMPLATE, DEFAULT_JDK_URL_TEMPLATE);
    }

    public static String buildJdkUrl(String version) {
        return resolveJdkUrl(version, getJdkBaseUrl(), getJdkUrlTemplate());
    }

    static String resolveJdkUrl(String version, String jdkBaseUrl, String jdkUrlTemplate) {
        // The default Adoptium endpoint redirects to GitHub, which is less stable than the mirrored zip path here.
        if (DEFAULT_JDK_URL_TEMPLATE.equals(jdkUrlTemplate)) {
            return buildJdkMirrorUrl(version, jdkBaseUrl);
        }

        if (jdkUrlTemplate != null && jdkUrlTemplate.contains("{version}")) {
            return jdkUrlTemplate.replace("{version}", version);
        }

        return buildJdkMirrorUrl(version, jdkBaseUrl);
    }

    private static String buildJdkMirrorUrl(String version, String jdkBaseUrl) {
        String artifactPath = JdkVersionConfig.getArtifactPath(version);
        if (artifactPath == null || artifactPath.isBlank()) {
            throw new IllegalArgumentException("Unsupported JDK version: " + version);
        }
        return normalizeBaseUrl(jdkBaseUrl) + artifactPath;
    }

    public static String buildMavenUrl(String version) {
        return getMavenBaseUrl() + version + "/binaries/apache-maven-" + version + "-bin.zip";
    }

    public static String buildNodeUrl(String version) {
        return getNodeBaseUrl() + version + "/node-" + version + "-win-x64.zip";
    }

    public static synchronized void saveLocalOverrides(String jdkBaseUrl,
                                                       String mavenBaseUrl,
                                                       String nodeBaseUrl) throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_JDK_BASE, normalizeBaseUrl(jdkBaseUrl));
        props.setProperty(KEY_JDK_URL_TEMPLATE, getJdkUrlTemplate());
        props.setProperty(KEY_MAVEN_BASE, normalizeBaseUrl(mavenBaseUrl));
        props.setProperty(KEY_NODE_BASE, normalizeBaseUrl(nodeBaseUrl));

        Path path = getLocalOverridePath();
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, "Local override download sources");
        }

        reload();
    }

    private static String get(String key, String defaultValue) {
        Properties props = cached;
        if (props == null) {
            synchronized (DownloadSourceConfig.class) {
                if (cached == null) {
                    cached = loadProperties();
                }
                props = cached;
            }
        }
        return props.getProperty(key, defaultValue);
    }

    private static Properties loadProperties() {
        Properties defaults = new Properties();
        defaults.setProperty(KEY_JDK_BASE, DEFAULT_JDK_BASE);
        defaults.setProperty(KEY_JDK_URL_TEMPLATE, DEFAULT_JDK_URL_TEMPLATE);
        defaults.setProperty(KEY_MAVEN_BASE, DEFAULT_MAVEN_BASE);
        defaults.setProperty(KEY_NODE_BASE, DEFAULT_NODE_BASE);

        Properties merged = new Properties(defaults);

        // Load bundled defaults first so external overrides only need to provide changed keys.
        try (InputStream in = DownloadSourceConfig.class.getClassLoader().getResourceAsStream(LOCAL_FILE_NAME)) {
            if (in != null) {
                merged.load(in);
            }
        } catch (IOException ignored) {
            // Ignore and keep defaults.
        }

        // Override from local file in working directory.
        File local = new File(System.getProperty("user.dir"), LOCAL_FILE_NAME);
        if (local.exists() && local.isFile()) {
            try (FileInputStream in = new FileInputStream(local)) {
                merged.load(in);
                return merged;
            } catch (IOException ignored) {
                // Ignore and continue to system-property file path.
            }
        }

        // Override from explicit system property file path.
        String overridePath = System.getProperty(SYS_PROP_PATH);
        if (overridePath != null && !overridePath.isBlank()) {
            File override = new File(overridePath);
            if (override.exists() && override.isFile()) {
                try (FileInputStream in = new FileInputStream(override)) {
                    merged.load(in);
                } catch (IOException ignored) {
                    // Ignore and keep merged values.
                }
            }
        }

        return merged;
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url : url + "/";
    }
}
