package top.oneyi.envLauncher.service;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import top.oneyi.envLauncher.MainApp;
import top.oneyi.envLauncher.callback.JdkDownloadCallback;
import top.oneyi.envLauncher.config.JDKVersionConfig;
import top.oneyi.envLauncher.controller.DownloadProgressDialogController;
import top.oneyi.envLauncher.utils.EnvUtil;
import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.*;
import java.net.URL;

/**
 * @author W
 * @date 2025/6/17
 * @description JDK安装服务业务层
 */
public class EnvInstallerService {

    private Stage dialogStage;

    /**
     * 设置 node
     *
     *
     * @param version
     */
    public void onSetupNode(String version) {
        String baseUrl = "https://npmmirror.com/mirrors/node/";
        String nodeUrl = baseUrl + version + "/node-" + version + "-win-x64.zip";
        LoggerUtil.info("📥 获取 Node.js 官方下载地址: " + nodeUrl);
        // 创建下载进度对话框
        DownloadProgressDialogController controller = createDialog("Node 下载进度");
        String destinationPath = PathUtils.getNodeDownloadPath(version);

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                downloadFileWithProgress(nodeUrl, destinationPath, controller, dialogStage, null);


                // 解压完成，开始查找 Node 根目录
                String extractedDir = destinationPath.replace(".zip", "");
                File nodeRoot = findNodeHome(new File(extractedDir));

                if (nodeRoot != null) {
                    String nodeHome = nodeRoot.getAbsolutePath();
                    Platform.runLater(() -> LoggerUtil.info("✅ 找到 Node 安装目录: " + nodeHome));

                    // 设置环境变量
                    EnvUtil.setNodeEnvironmentVariables(nodeHome,"%NODE_HOME%"); // 假设 bin 在当前目录
                } else {
                    Platform.runLater(() -> LoggerUtil.info("❌ 未找到 node.exe，请检查解压目录"));
                }

                return null;
            }
        };

        new Thread(downloadTask).start();
    }

    /**
     * 查找解压后的 Node 实际根目录（可能嵌套一层）
     *
     * @param extractedDir 解压后的根目录
     * @return 实际包含 node.exe 的目录，找不到返回 null
     */
    private File findNodeHome(File extractedDir) {
        // 先检查当前目录是否包含 node.exe
        File nodeExe = new File(extractedDir, "node.exe");
        if (nodeExe.exists()) {
            return extractedDir;
        }

        // 否则尝试进入下一级目录查找
        File[] subDirs = extractedDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                nodeExe = new File(subDir, "node.exe");
                if (nodeExe.exists()) {
                    return subDir; // 找到嵌套的 Node 目录
                }
            }
        }

        return null; // 没有找到有效目录
    }


    /**
     * 下载指定版本的 Maven 并解压
     *
     * @param version Maven 版本（如 "3.8.8"）
     */
    public void onSetupMaven(String version) {
        String baseUrl = "https://archive.apache.org/dist/maven/maven-3/";
        String mavenUrl = baseUrl + version + "/binaries/apache-maven-" + version + "-bin.zip";
        String destinationPath = PathUtils.getMavenDownloadPath(version);

        LoggerUtil.info("📥 开始从官方地址下载 Maven: " + version);
        // 创建下载进度对话框
        DownloadProgressDialogController controller = createDialog("Maven 下载进度");

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                downloadFileWithProgress(mavenUrl, destinationPath, controller, dialogStage, null);
                // 开始设置 maven 配置文件和 maven 仓库
                String extractedDir = destinationPath.replace(".zip", "");
                // ✅ 创建 Maven 仓库目录
                createMavenRepository(extractedDir);
                // ✅ 配置 settings.xml 文件
                extractedDir = findMavenHome(new File(extractedDir));
                configureMavenSettings(extractedDir);
                // ✅ 设置 Maven 环境变量
                EnvUtil.setMavenEnvironmentVariables(extractedDir, extractedDir + "\\bin");
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
    private String findMavenHome(File extractedDir) {
        // 先检查当前目录是否是有效的 Maven 根目录（含有 conf 和 bin）
        if (isValidMavenRoot(extractedDir)) {
            return extractedDir.getAbsolutePath();
        }

        // 如果不是，则尝试进入下一级目录查找
        File[] subDirs = extractedDir.listFiles((file) -> file.isDirectory());
        if (subDirs != null && subDirs.length > 0) {
            for (File subDir : subDirs) {
                if (isValidMavenRoot(subDir)) {
                    return subDir.getAbsolutePath(); // 找到嵌套的 Maven 根目录
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


    /**
     * 创建 Maven 仓库
     *
     * @param mavenHome Maven 下载目录
     */
    private void createMavenRepository(String mavenHome) {
        File repoDir = new File(mavenHome, "maven-repository");
        if (!repoDir.exists()) {
            boolean success = repoDir.mkdirs();
            if (success) {
                Platform.runLater(() -> LoggerUtil.info("📁 已创建 Maven 本地仓库目录: " + repoDir.getAbsolutePath()));
            } else {
                Platform.runLater(() -> LoggerUtil.info("❌ 创建 Maven 仓库失败"));
            }
        } else {
            Platform.runLater(() -> LoggerUtil.info("📁 Maven 仓库已存在: " + repoDir.getAbsolutePath()));
        }
    }


    /**
     * 配置 Maven 设置
     *
     * @param mavenHome maven 下载目录
     */
    private void configureMavenSettings(String mavenHome) {
        File settingsFile = new File(mavenHome, "conf" + File.separator + "settings.xml");

        if (!settingsFile.exists()) {
            Platform.runLater(() -> LoggerUtil.info("❌ 找不到 settings.xml 文件"));
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
            //  \ 会被转义掉 所以需要转义一下  \
            localRepoPath = localRepoPath.replace("\\", "\\\\");
            String updatedContent = content.toString().replaceFirst("</settings>",
                    "  <localRepository>" + localRepoPath + "</localRepository>\n</settings>");


            // ✅ 添加阿里云镜像源（如果还没有 mirror 配置）
            String aliyunMirror = """
                    <mirror>
                      <id>aliyunmaven</id>
                      <mirrorOf>*</mirrorOf>
                      <name>阿里云公共仓库</name>
                      <url>https://maven.aliyun.com/repository/public</url>
                    </mirror>""";

            // 检查是否已有 <mirrors> 节点
            if (updatedContent.contains("<mirrors>")) {
                updatedContent = updatedContent.replaceFirst("</mirrors>",
                        aliyunMirror + "\n</mirrors>");
            }

            // 写回文件
            BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile));
            writer.write(updatedContent);
            writer.close();

            String finalLocalRepoPath = localRepoPath;
            Platform.runLater(() -> LoggerUtil.info("✅ 已配置 Maven 本地仓库路径: " + finalLocalRepoPath));

        } catch (IOException e) {
            Platform.runLater(() -> LoggerUtil.info("❌ 修改 settings.xml 失败: " + e.getMessage()));
            e.printStackTrace();
        }
    }


    /**
     * 下载JDK
     *
     * @param version  JDK版本
     * @param callback 回调
     */
    public void onDownloadJdk(String version, JdkDownloadCallback callback) {
        String baseUrl = "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/";
        String jdkDownloadUrl = baseUrl + JDKVersionConfig.getUrl(version);
        String destinationPath = PathUtils.getDownloadPath(version);

        LoggerUtil.info("📥 开始从清华大学镜像下载 JDK: " + version);
        // 创建下载进度对话框
        DownloadProgressDialogController controller = createDialog("JDK 下载进度");

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                downloadFileWithProgress(jdkDownloadUrl, destinationPath, controller, dialogStage, callback);
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
     * @param controller      进度条控制器
     * @param dialogStage     弹窗
     * @param callback        下载完成回调
     */
    private void downloadFileWithProgress(String url,
                                          String destinationPath,
                                          DownloadProgressDialogController controller,
                                          Stage dialogStage,
                                          JdkDownloadCallback callback) {
        try {
            // 1️⃣ 获取文件大小并更新 UI
            long contentLength = PathUtils.getFileSize(url);
            updateFileSizeLabel(controller, contentLength);

            // 2️⃣ 开始下载
            boolean downloadSuccess = downloadToFile(url, destinationPath, contentLength, controller);
            if (!downloadSuccess || controller.isCancelRequested()) {
                Platform.runLater(() -> LoggerUtil.info("❌ 下载已取消"));
                return;
            }

            // 3️⃣ 解压 ZIP 文件
            String extractedDir = destinationPath.replace(".zip", "");
            boolean unzipSuccess = unzipAndNotify(destinationPath, extractedDir, controller);
            if (!unzipSuccess) {
                return;
            }

            // 4️⃣ 自动删除 ZIP 文件
            autoDeleteZipFile(destinationPath);

            // 5️⃣ 回调通知
            if (callback != null) {
                callback.onDownloadComplete(extractedDir);
            }

            // 6️⃣ 关闭弹窗
            Platform.runLater(dialogStage::close);

        } catch (Exception e) {
            handleDownloadError(e, controller, dialogStage);
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
     * @return 下载成功返回 true，否则返回 false
     * @throws IOException
     */
    private boolean downloadToFile(String url, String destinationPath, long contentLength,
                                   DownloadProgressDialogController controller) throws IOException {
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
                LoggerUtil.info("✅ 文件下载完成: " + destinationPath);
            });

            return true;
        } catch (IOException e) {
            if (controller.isCancelRequested()) {
                Platform.runLater(() -> LoggerUtil.info("❌ 下载已取消"));
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
     * @return 是否成功
     */
    private boolean unzipAndNotify(String zipPath, String extractDir,
                                   DownloadProgressDialogController controller) {
        try {
            Platform.runLater(() -> controller.statusLabel.setText("📦 开始解压文件..."));

            PathUtils.unzipFile(zipPath, extractDir);

            Platform.runLater(() -> {
                controller.statusLabel.setText("✅ 解压完成");
                LoggerUtil.info("✅ 文件已解压至: " + extractDir);
            });

            return true;
        } catch (IOException e) {
            Platform.runLater(() -> {
                controller.statusLabel.setText("❌ 解压失败: " + e.getMessage());
                LoggerUtil.info("❌ 解压失败: " + e.getMessage());
            });
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 🗑️ 自动删除 ZIP 文件
     *
     * @param zipPath ZIP 文件路径
     */
    private void autoDeleteZipFile(String zipPath) {
        File zipFile = new File(zipPath);
        if (zipFile.exists() && zipFile.isFile()) {
            if (zipFile.delete()) {
                Platform.runLater(() -> LoggerUtil.info("🗑 已自动删除 ZIP 文件: " + zipPath));
            } else {
                Platform.runLater(() -> LoggerUtil.info("⚠ 删除 ZIP 文件失败: " + zipPath));
            }
        }
    }

    /**
     * ❌ 统一错误处理
     *
     * @param e           错误
     * @param controller  弹窗控制器
     * @param dialogStage 弹窗窗口
     */
    private void handleDownloadError(Exception e,
                                     DownloadProgressDialogController controller,
                                     Stage dialogStage) {
        Platform.runLater(() -> {
            controller.progressBar.setProgress(0);
            controller.statusLabel.setText("❌ 下载失败: " + e.getMessage());
            LoggerUtil.info("❌ 下载失败: " + e.getMessage());
        });

        e.printStackTrace();

        Platform.runLater(dialogStage::close);
    }


    /**
     * 创建下载进度对话框的 Controller
     *
     * @param title 对话框的标题
     * @return 对话框的 Controller
     */
    private DownloadProgressDialogController createDialog(String title) {
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
