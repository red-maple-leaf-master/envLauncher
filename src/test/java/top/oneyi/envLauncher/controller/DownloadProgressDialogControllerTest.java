package top.oneyi.envLauncher.controller;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DownloadProgressDialogControllerTest {

    @BeforeClass
    public static void initToolkit() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit may already be initialized by another test.
        }
    }

    @Test
    public void marksCancelRequestedWhenDialogIsClosedDirectly() {
        DownloadProgressDialogController controller = new DownloadProgressDialogController();
        controller.statusLabel = new Label("Downloading...");

        controller.onDialogCloseRequest();

        assertTrue("closing the dialog window should cancel the download", controller.isCancelRequested());
        assertEquals("Download canceled by user", controller.statusLabel.getText());
    }
}
