package top.oneyi.jdktool.model;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DownloadProgressDialog {

    private Stage stage;
    private ProgressBar progressBar;
    private Label statusLabel;

    public DownloadProgressDialog() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("正在下载 JDK");

        progressBar = new ProgressBar(0);
        statusLabel = new Label("准备中...");

        VBox layout = new VBox(10, statusLabel, progressBar);
        layout.setPrefWidth(400);
        layout.setPrefHeight(150);

        Scene scene = new Scene(layout);
        stage.setScene(scene);
    }

    public void show() {
        Platform.runLater(() -> stage.show());
    }

    public void updateProgress(double progress, String status) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            statusLabel.setText(status);
        });
    }

    public void close() {
        Platform.runLater(() -> stage.close());
    }
}
