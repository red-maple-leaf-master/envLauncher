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
import top.oneyi.jdktool.callback.JdkDownloadCallback;
import top.oneyi.jdktool.config.JDKVersionConfig;
import top.oneyi.jdktool.controller.DownloadProgressDialogController;
import top.oneyi.jdktool.model.DownloadProgressDialog;
import top.oneyi.jdktool.utils.PathUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author W
 * @date 2025/6/17
 * @description JDK安装服务业务层
 */
public class JdkInstallerService {


    public void onSetupMaven(TextArea outputArea) {
        outputArea.appendText("⚠ 暂不支持 Maven 设置功能\n");
    }

    public void onDownloadJdk(TextArea outputArea, String version, JdkDownloadCallback callback) {
        String baseUrl = "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/";
        String jdkDownloadUrl = baseUrl + JDKVersionConfig.getUrl(version);
        String destinationPath = PathUtils.getDownloadPath(version);
        outputArea.appendText("📥 开始从清华大学镜像下载 JDK: " + version + "\n");

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("下载进度");

        URL fxmlUrl = MainApp.class.getResource("download-progress-dialog.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        try {
            Scene scene = new Scene(loader.load(), 450, 180);
            dialogStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        DownloadProgressDialogController controller = loader.getController();
        dialogStage.show();

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                downloadFileWithProgress(jdkDownloadUrl, destinationPath, outputArea, controller, dialogStage, callback);
                return null;
            }
        };

        new Thread(downloadTask).start();
    }


    private void downloadFileWithProgress(String url, String destinationPath, TextArea outputArea,
                                          DownloadProgressDialogController controller, Stage dialogStage,
                                          JdkDownloadCallback callback) {
        try {
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

            // ✅ 开始解压
            Platform.runLater(() -> controller.statusLabel.setText("📦 开始解压文件..."));

            String extractedDir = destinationPath.replace(".zip", "");

            unzipFile(destinationPath, extractedDir);

            Platform.runLater(() -> {
                controller.statusLabel.setText("✅ 解压完成");
                outputArea.appendText("✅ 文件已解压至: " + extractedDir + "\n");

                // 可选：回调通知控制器更新输入框
                if (callback != null) {
                    callback.onDownloadComplete(extractedDir);
                }
            });


            Platform.runLater(dialogStage::close);

        } catch (IOException e) {
            Platform.runLater(() -> {
                controller.progressBar.setProgress(0);
                controller.statusLabel.setText("❌ 下载失败: " + e.getMessage());
                outputArea.appendText("❌ 下载失败: " + e.getMessage() + "\n");
            });

            e.printStackTrace();
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

    private void unzipFile(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            // 缓存大小
            byte[] buffer = new byte[1024];
            int len;

            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                File newFile = new File(filePath);

                // 创建父目录
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // 确保父目录存在
                    new File(newFile.getParent()).mkdirs();

                    // 写入文件
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int read;
                    while ((read = zipIn.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                    }
                    fos.close();
                }
                entry = zipIn.getNextEntry();
            }
        }
    }

}
