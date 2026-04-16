package top.oneyi.envLauncher.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DownloadSourceConfigTest {

    @Test
    public void resolvesDefaultJdkTemplateToMirrorArtifactUrl() {
        String resolvedUrl = DownloadSourceConfig.resolveJdkUrl(
                "17",
                "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/",
                "https://api.adoptium.net/v3/binary/latest/{version}/ga/windows/x64/jdk/hotspot/normal/eclipse"
        );

        assertEquals(
                "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.18_8.zip",
                resolvedUrl
        );
    }

    @Test
    public void keepsCustomJdkTemplateWhenUserOverridesSource() {
        String resolvedUrl = DownloadSourceConfig.resolveJdkUrl(
                "17",
                "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/",
                "https://example.com/jdk/{version}/custom.zip"
        );

        assertEquals("https://example.com/jdk/17/custom.zip", resolvedUrl);
    }
}
