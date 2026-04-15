package top.oneyi.envLauncher.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Controls the download progress dialog shown during archive downloads.
 */
public class DownloadProgressDialogController {
    @FXML
    public ProgressBar progressBar;
    @FXML
    public Label statusLabel;
    @FXML
    public Label sizeLabel;

    private volatile boolean cancelRequested = false;

    public void onCancelDownload() {
        cancelRequested = true;
        statusLabel.setText("Download canceled by user");
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }
}
