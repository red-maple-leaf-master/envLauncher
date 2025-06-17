package top.oneyi.jdktool.service;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import top.oneyi.jdktool.MainApp;
import top.oneyi.jdktool.controller.DownloadProgressDialogController;
import top.oneyi.jdktool.model.DownloadProgressDialog;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author W
 * @date 2025/6/17
 * @description JDK安装服务业务层
 */
public class JdkInstallerService {


    public void onSetupMaven(TextArea outputArea) {
        outputArea.appendText("⚠ 暂不支持 Maven 设置功能\n");
    }

    public void onDownloadJdk(TextArea outputArea) {
        String jdkDownloadUrl = "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.15_6.zip";
        String destinationPath = System.getProperty("user.home") + "/Downloads/openjdk-17.0.15.zip";

        outputArea.appendText("📥 开始从清华大学镜像下载 JDK...\n");

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("下载进度");
        // 加载 FXML
        URL fxmlUrl = MainApp.class.getResource("download-progress-dialog.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        try {

            Scene scene = new Scene(loader.load(), 450, 180);
            dialogStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // 获取控制器
        DownloadProgressDialogController controller = loader.getController();

        // 显示弹窗
        dialogStage.show();

        // 启动后台下载任务
        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                downloadFileWithProgress(jdkDownloadUrl, destinationPath, outputArea, controller, dialogStage);
                return null;
            }
        };

        new Thread(downloadTask).start();
    }


    private void downloadFileWithProgress(String url, String destinationPath, TextArea outputArea,
                                          DownloadProgressDialogController controller, Stage dialogStage) {
        try {
            // 获取文件大小（更准确的方式）
            long contentLength = getFileSize(url);

            if (contentLength <= 0) {
                Platform.runLater(() -> controller.statusLabel.setText("⚠ 无法获取文件大小"));
            } else {
                Platform.runLater(() -> controller.sizeLabel.setText("文件大小: " + contentLength / (1024 * 1024) + " MB"));
            }

            BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(destinationPath);

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;

            Platform.runLater(() -> controller.statusLabel.setText("开始下载..."));

            while ((bytesRead = in.read(dataBuffer)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                final double progress = (double) totalBytesRead / contentLength;
                final String status = String.format("已下载 %.2f MB / %.2f MB",
                        totalBytesRead / (1024.0 * 1024.0), contentLength / (1024.0 * 1024.0));

                Platform.runLater(() -> {
                    controller.progressBar.setProgress(progress);
                    controller.statusLabel.setText(status);
                });
            }

            fileOutputStream.close();

            Platform.runLater(() -> {
                controller.progressBar.setProgress(1.0);
                controller.statusLabel.setText("✅ 文件下载完成");
                outputArea.appendText("✅ 文件下载完成: " + destinationPath + "\n");
            });

            // 关闭弹窗
            Platform.runLater(dialogStage::close);

        } catch (IOException e) {
            Platform.runLater(() -> {
                controller.progressBar.setProgress(0);
                controller.statusLabel.setText("❌ 下载失败: " + e.getMessage());
                outputArea.appendText("❌ 下载失败: " + e.getMessage() + "\n");
            });

            e.printStackTrace();

            // 关闭弹窗
            Platform.runLater(dialogStage::close);
        }
    }

    private long getFileSize(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        long contentLength = connection.getContentLengthLong();
        connection.disconnect();
        return contentLength;
    }

}
