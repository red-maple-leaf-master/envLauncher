package top.oneyi.envLauncher;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class EnvInstallerLayoutTest {

    @Test
    public void removesHeroPanelAndOneClickInstallEntry() throws IOException {
        String fxml = readEnvInstallerFxml();

        assertFalse("top hero panel should be removed", fxml.contains("styleClass=\"hero-panel\""));
        assertFalse("hero title should be removed", fxml.contains("Web Dev Environment Setup"));
        assertFalse("one click install button should be removed", fxml.contains("fx:id=\"oneClickInstallButton\""));
        assertFalse("one click install action should be removed", fxml.contains("onAction=\"#onOneClickInstall\""));
    }

    @Test
    public void envInstallerLayoutContainsLocalPathFieldsAndApplyButtons() throws IOException {
        String fxml = readEnvInstallerFxml();

        assertTrue(fxml.contains("fx:id=\"localJdkPathField\""));
        assertTrue(fxml.contains("fx:id=\"localMavenPathField\""));
        assertTrue(fxml.contains("fx:id=\"localNodePathField\""));
        assertTrue(fxml.contains("fx:id=\"selectLocalJdkButton\""));
        assertTrue(fxml.contains("fx:id=\"applyLocalJdkButton\""));
        assertTrue(fxml.contains("fx:id=\"selectLocalMavenButton\""));
        assertTrue(fxml.contains("fx:id=\"applyLocalMavenButton\""));
        assertTrue(fxml.contains("fx:id=\"selectLocalNodeButton\""));
        assertTrue(fxml.contains("fx:id=\"applyLocalNodeButton\""));
    }

    private String readEnvInstallerFxml() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/top/oneyi/envLauncher/env-installer.fxml")) {
            if (inputStream == null) {
                throw new IOException("env-installer.fxml not found");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
