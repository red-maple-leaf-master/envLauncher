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


    /**
     * 设置Maven
     *
     * @param outputArea 输出区域
     */
    public void onSetupMaven(TextArea outputArea) {
        outputArea.appendText("⚠ 暂不支持 Maven 设置功能\n");
    }


    /**
     * 下载JDK
     *
     * @param outputArea 输出区域
     * @param version    JDK版本
     * @param callback   回调
     */
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

    /**
     * 下载文件并显示进度
     *
     * @param url             下载链接
     * @param destinationPath 保存路径
     * @param outputArea      输出文本框
     * @param controller      进度条控制器
     * @param dialogStage     弹窗
     * @param callback        下载完成回调
     */
    private void downloadFileWithProgress(String url,
                                          String destinationPath,
                                          TextArea outputArea,
                                          DownloadProgressDialogController controller,
                                          Stage dialogStage,
                                          JdkDownloadCallback callback) {
        try {
            // 1️⃣ 获取文件大小并更新 UI
            long contentLength = PathUtils.getFileSize(url);
            updateFileSizeLabel(controller, contentLength);

            // 2️⃣ 开始下载
            boolean downloadSuccess = downloadToFile(url, destinationPath, contentLength, controller, outputArea);
            if (!downloadSuccess || controller.isCancelRequested()) {
                Platform.runLater(() -> outputArea.appendText("❌ 下载已取消\n"));
                return;
            }

            // 3️⃣ 解压 ZIP 文件
            String extractedDir = destinationPath.replace(".zip", "");
            boolean unzipSuccess = unzipAndNotify(destinationPath, extractedDir, controller, outputArea);
            if (!unzipSuccess) {
                return;
            }

            // 4️⃣ 自动删除 ZIP 文件
            autoDeleteZipFile(destinationPath, outputArea);

            // 5️⃣ 回调通知
            if (callback != null) {
                callback.onDownloadComplete(extractedDir);
            }

            // 6️⃣ 关闭弹窗
            Platform.runLater(dialogStage::close);

        } catch (Exception e) {
            handleDownloadError(e, controller, outputArea, dialogStage);
        }
    }

    /**
     * 📦 更新文件大小提示
     *
     * @param controller    弹窗控制器
     * @param contentLength 文件大小
     */
    private void updateFileSizeLabel(DownloadProgressDialogController controller, long contentLength) {
        if (contentLength <= 0) {
            Platform.runLater(() -> controller.sizeLabel.setText("⚠ 无法获取文件大小"));
        } else {
            String sizeText = String.format("文件大小: %.2f MB", contentLength / (1024.0 * 1024.0));
            Platform.runLater(() -> controller.sizeLabel.setText(sizeText));
        }
    }

    /**
     * 📥 执行下载逻辑
     *
     * @param url             下载链接
     * @param destinationPath 下载路径
     * @param contentLength   文件大小
     * @param controller      弹窗控制器
     * @param outputArea      输出文本框
     * @return 下载成功返回 true，否则返回 false
     * @throws IOException
     */
    private boolean downloadToFile(String url, String destinationPath, long contentLength,
                                   DownloadProgressDialogController controller, TextArea outputArea) throws IOException {
        URL downloadUrl = new URL(url);
        InputStream inputStream = new BufferedInputStream(downloadUrl.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(destinationPath);

        byte[] dataBuffer = new byte[1024];
        int bytesRead;
        long totalBytesRead = 0;

        Platform.runLater(() -> controller.statusLabel.setText("开始下载..."));

        try {
            while ((bytesRead = inputStream.read(dataBuffer)) != -1) {
                // ✅ 检查是否用户点击了取消按钮
                if (controller.isCancelRequested()) {
                    // 关闭输入流和输出流 并删除已经下载的 文件
                    controller.closeStream(inputStream, fileOutputStream, destinationPath);
                    return false;
                }

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

            return true;
        } catch (IOException e) {
            if (controller.isCancelRequested()) {
                Platform.runLater(() -> outputArea.appendText("❌ 下载已取消\n"));
                return false;
            }
            throw e;
        }
    }

    /**
     * 📦 解压 ZIP 文件
     *
     * @param zipPath    ZIP 文件路径
     * @param extractDir 提取目录
     * @param controller 控制器
     * @param outputArea 输出区域
     * @return 是否成功
     */
    private boolean unzipAndNotify(String zipPath, String extractDir,
                                   DownloadProgressDialogController controller,
                                   TextArea outputArea) {
        try {
            Platform.runLater(() -> controller.statusLabel.setText("📦 开始解压文件..."));

            PathUtils.unzipFile(zipPath, extractDir);

            Platform.runLater(() -> {
                controller.statusLabel.setText("✅ 解压完成");
                outputArea.appendText("✅ 文件已解压至: " + extractDir + "\n");
            });

            return true;
        } catch (IOException e) {
            Platform.runLater(() -> {
                controller.statusLabel.setText("❌ 解压失败: " + e.getMessage());
                outputArea.appendText("❌ 解压失败: " + e.getMessage() + "\n");
            });
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 🗑️ 自动删除 ZIP 文件
     *
     * @param zipPath    ZIP 文件路径
     * @param outputArea 输出区域
     */
    private void autoDeleteZipFile(String zipPath, TextArea outputArea) {
        File zipFile = new File(zipPath);
        if (zipFile.exists() && zipFile.isFile()) {
            if (zipFile.delete()) {
                Platform.runLater(() -> outputArea.appendText("🗑️ 已自动删除 ZIP 文件: " + zipPath + "\n"));
            } else {
                Platform.runLater(() -> outputArea.appendText("⚠️ 删除 ZIP 文件失败: " + zipPath + "\n"));
            }
        }
    }

    /**
     * ❌ 统一错误处理
     *
     * @param e           错误
     * @param controller  弹窗控制器
     * @param outputArea  输出文本框
     * @param dialogStage 弹窗窗口
     */
    private void handleDownloadError(Exception e,
                                     DownloadProgressDialogController controller,
                                     TextArea outputArea,
                                     Stage dialogStage) {
        Platform.runLater(() -> {
            controller.progressBar.setProgress(0);
            controller.statusLabel.setText("❌ 下载失败: " + e.getMessage());
            outputArea.appendText("❌ 下载失败: " + e.getMessage() + "\n");
        });

        e.printStackTrace();

        Platform.runLater(dialogStage::close);
    }


}
