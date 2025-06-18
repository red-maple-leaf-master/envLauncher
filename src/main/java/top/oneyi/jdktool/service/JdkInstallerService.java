package top.oneyi.jdktool.service;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import top.oneyi.jdktool.MainApp;
import top.oneyi.jdktool.callback.JdkDownloadCallback;
import top.oneyi.jdktool.config.JDKVersionConfig;
import top.oneyi.jdktool.controller.DownloadProgressDialogController;
import top.oneyi.jdktool.utils.PathUtils;
import java.io.*;
import java.net.URL;

/**
 * @author W
 * @date 2025/6/17
 * @description JDK安装服务业务层
 */
public class JdkInstallerService {

    private Stage dialogStage;


    /**
     * 下载指定版本的 Maven 并解压
     *
     * @param outputArea 输出日志区域
     * @param version    Maven 版本（如 "3.8.8"）
     * @param callback   下载完成回调
     */
    public void onSetupMaven(TextArea outputArea, String version, JdkDownloadCallback callback) {
        String baseUrl = "https://archive.apache.org/dist/maven/maven-3/";
        String mavenUrl = baseUrl + version + "/binaries/apache-maven-" + version + "-bin.zip";
        String destinationPath = PathUtils.getMavenDownloadPath(version);

        outputArea.appendText("📥 开始从官方地址下载 Maven: " + version + "\n");
        // 创建下载进度对话框
        DownloadProgressDialogController controller = createDialog("Maven 下载进度");

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                downloadFileWithProgress(mavenUrl, destinationPath, outputArea, controller, dialogStage, callback);
                // 开始设置 maven 配置文件和 maven 仓库
                String extractedDir = destinationPath.replace(".zip", "");
                // ✅ 创建 Maven 仓库目录
                createMavenRepository(extractedDir, outputArea);
                // ✅ 配置 settings.xml 文件
                configureMavenSettings(extractedDir, outputArea);
                return null;
            }
        };

        new Thread(downloadTask).start();
    }
    /**
     * 查找解压后的 Maven 实际根目录（可能嵌套一层）
     *
     * @param extractedDir 解压后的根目录
     * @return 实际包含 bin/conf 的 Maven 根目录，找不到返回 null
     */
    private File findMavenHome(File extractedDir) {
        // 先检查当前目录是否是有效的 Maven 根目录（含有 conf 和 bin）
        if (isValidMavenRoot(extractedDir)) {
            return extractedDir;
        }

        // 如果不是，则尝试进入下一级目录查找
        File[] subDirs = extractedDir.listFiles((file) -> file.isDirectory());
        if (subDirs != null && subDirs.length > 0) {
            for (File subDir : subDirs) {
                if (isValidMavenRoot(subDir)) {
                    return subDir; // 找到嵌套的 Maven 根目录
                }
            }
        }

        return null; // 没有找到有效目录
    }

    /**
     * 判断给定目录是否为 Maven 的安装根目录（包含 conf 和 bin 目录）
     *
     * @param dir 要检查的目录
     * @return 是否为有效 Maven 根目录
     */
    private boolean isValidMavenRoot(File dir) {
        File confDir = new File(dir, "conf");
        File binDir = new File(dir, "bin");
        return confDir.exists() && binDir.exists();
    }

    private void createMavenRepository(String mavenHome, TextArea outputArea) {
        File repoDir = new File(mavenHome, "maven-repository");
        if (!repoDir.exists()) {
            boolean success = repoDir.mkdirs();
            if (success) {
                Platform.runLater(() -> outputArea.appendText("📁 已创建 Maven 本地仓库目录: " + repoDir.getAbsolutePath() + "\n"));
            } else {
                Platform.runLater(() -> outputArea.appendText("❌ 创建 Maven 仓库失败\n"));
            }
        } else {
            Platform.runLater(() -> outputArea.appendText("📁 Maven 仓库已存在: " + repoDir.getAbsolutePath() + "\n"));
        }
    }
    private void configureMavenSettings(String mavenHome, TextArea outputArea) {
        File settingsFile = new File(mavenHome, "conf" + File.separator + "settings.xml");

        if (!settingsFile.exists()) {
            Platform.runLater(() -> outputArea.appendText("❌ 找不到 settings.xml 文件\n"));
            return;
        }

        try {
            // 读取文件内容
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(settingsFile));
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            reader.close();

            // 设置本地仓库路径
            String localRepoPath = new File(mavenHome, "maven-repository").getAbsolutePath();
            String updatedContent = content.toString().replaceAll(
                    "<localRepository>.*?</localRepository>",
                    "<localRepository>" + localRepoPath + "</localRepository>"
            );

            // 如果没有找到 <localRepository> 标签，则插入进去
            if (!content.toString().contains("<localRepository>")) {
                updatedContent = content.toString().replaceFirst("</settings>",
                        "  <localRepository>" + localRepoPath + "</localRepository>\n</settings>");
            }

            // ✅ 添加阿里云镜像源（如果还没有 mirror 配置）
            String aliyunMirror = "<mirror>\n" +
                    "  <id>aliyunmaven</id>\n" +
                    "  <mirrorOf>*</mirrorOf>\n" +
                    "  <name>阿里云公共仓库</name>\n" +
                    "  <url>https://maven.aliyun.com/repository/public</url>\n" +
                    "</mirror>\n" +
                    "    ";

            // 检查是否已有 <mirrors> 节点
            if (updatedContent.contains("<mirrors>")) {
                updatedContent = updatedContent.replaceFirst("</settings>",
                        aliyunMirror + "\n</settings>");
                outputArea.setText("⚠ 已有 <mirrors> 节点，请手动添加阿里云镜像源\n");
            }

            // 写回文件
            BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile));
            writer.write(updatedContent);
            writer.close();

            Platform.runLater(() -> outputArea.appendText("✅ 已配置 Maven 本地仓库路径: " + localRepoPath + "\n"));

        } catch (IOException e) {
            Platform.runLater(() -> outputArea.appendText("❌ 修改 settings.xml 失败: " + e.getMessage() + "\n"));
            e.printStackTrace();
        }
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
        // 创建下载进度对话框
        DownloadProgressDialogController controller = createDialog("JDK 下载进度");

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


    /**
     * 创建下载进度对话框的 Controller
     * @param title 对话框的标题
     * @return 对话框的 Controller
     */
    private DownloadProgressDialogController createDialog(String  title) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);
        URL fxmlUrl = MainApp.class.getResource("download-progress-dialog.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);

        try {
            Scene scene = new Scene(loader.load(), 450, 180);
            dialogStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        DownloadProgressDialogController controller = loader.getController();
        dialogStage.show();
        return controller;
    }


}
