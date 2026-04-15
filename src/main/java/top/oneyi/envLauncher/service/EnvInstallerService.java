package top.oneyi.envLauncher.service;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import top.oneyi.envLauncher.MainApp;
import top.oneyi.envLauncher.callback.JdkDownloadCallback;
import top.oneyi.envLauncher.config.DownloadSourceConfig;
import top.oneyi.envLauncher.controller.DownloadProgressDialogController;
import top.oneyi.envLauncher.utils.EnvUtil;
import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;

public class EnvInstallerService {

    private Stage dialogStage;

    public void onSetupNode(String version) {
        onSetupNode(version, PathUtils.getCurrentDrive() + "environment", null);
    }

    public void onSetupNode(String version, Consumer<Boolean> doneCallback) {
        onSetupNode(version, PathUtils.getCurrentDrive() + "environment", doneCallback);
    }

    public void onSetupNode(String version, String baseDir, Consumer<Boolean> doneCallback) {
        String nodeUrl = DownloadSourceConfig.buildNodeUrl(version);
        String destinationPath = PathUtils.getNodeDownloadPath(baseDir, version);
        String extractedDir = PathUtils.getNodeExtractDir(baseDir, version);

        LoggerUtil.info("Node source: " + DownloadSourceConfig.getNodeBaseUrl());
        LoggerUtil.info("Start Node setup: " + version);

        DownloadProgressDialogController controller = createDialog("Node Download Progress");
        if (controller == null) {
            notifyDone(doneCallback, false);
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    boolean success = downloadFileWithProgress(nodeUrl, destinationPath, extractedDir, controller, dialogStage, null);
                    if (!success) {
                        return false;
                    }

                    File nodeRoot = findNodeHome(new File(extractedDir));
                    if (nodeRoot == null) {
                        LoggerUtil.info("node.exe not found after unzip.");
                        return false;
                    }

                    String nodeHome = nodeRoot.getAbsolutePath();
                    LoggerUtil.info("Node home found: " + nodeHome);
                    EnvUtil.setNodeEnvironmentVariables(nodeHome, "%NODE_HOME%");
                    return true;
                } catch (Exception e) {
                    LoggerUtil.info("Node setup failed: " + safeError(e));
                    return false;
                }
            }
        };

        hookTaskDone(task, doneCallback);
        new Thread(task, "setup-node-thread").start();
    }

    public void onSetupMaven(String version) {
        onSetupMaven(version, PathUtils.getCurrentDrive() + "environment", null);
    }

    public void onSetupMaven(String version, Consumer<Boolean> doneCallback) {
        onSetupMaven(version, PathUtils.getCurrentDrive() + "environment", doneCallback);
    }

    public void onSetupMaven(String version, String baseDir, Consumer<Boolean> doneCallback) {
        String mavenUrl = DownloadSourceConfig.buildMavenUrl(version);
        String destinationPath = PathUtils.getMavenDownloadPath(baseDir, version);
        String extractedDir = PathUtils.getMavenExtractDir(baseDir, version);

        LoggerUtil.info("Maven source: " + DownloadSourceConfig.getMavenBaseUrl());
        LoggerUtil.info("Start Maven setup: " + version);

        DownloadProgressDialogController controller = createDialog("Maven Download Progress");
        if (controller == null) {
            notifyDone(doneCallback, false);
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    boolean success = downloadFileWithProgress(mavenUrl, destinationPath, extractedDir, controller, dialogStage, null);
                    if (!success) {
                        return false;
                    }

                    String mavenHome = findMavenHome(new File(extractedDir));
                    if (mavenHome == null) {
                        LoggerUtil.info("Valid Maven root not found.");
                        return false;
                    }

                    createMavenRepository(mavenHome);
                    configureMavenSettings(mavenHome);
                    EnvUtil.setMavenEnvironmentVariables(mavenHome, mavenHome + "\\bin");
                    return true;
                } catch (Exception e) {
                    LoggerUtil.info("Maven setup failed: " + safeError(e));
                    return false;
                }
            }
        };

        hookTaskDone(task, doneCallback);
        new Thread(task, "setup-maven-thread").start();
    }

    public void onDownloadJdk(String version, JdkDownloadCallback callback) {
        onDownloadJdk(version, PathUtils.getCurrentDrive() + "environment", callback, null);
    }

    public void onDownloadJdk(String version,
                              String baseDir,
                              JdkDownloadCallback callback,
                              Consumer<Boolean> doneCallback) {
        String jdkDownloadUrl;
        try {
            jdkDownloadUrl = DownloadSourceConfig.buildJdkUrl(version);
        } catch (IllegalArgumentException e) {
            LoggerUtil.info("Unsupported JDK version: " + version);
            notifyDone(doneCallback, false);
            return;
        }

        String destinationPath = PathUtils.getDownloadPath(baseDir, version);
        String extractedDir = PathUtils.getJdkExtractDir(baseDir, version);

        LoggerUtil.info("JDK source: " + DownloadSourceConfig.getJdkBaseUrl());
        LoggerUtil.info("JDK resolved url: " + jdkDownloadUrl);
        LoggerUtil.info("Start JDK install: " + version);

        DownloadProgressDialogController controller = createDialog("JDK Download Progress");
        if (controller == null) {
            notifyDone(doneCallback, false);
            return;
        }

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return downloadFileWithProgress(jdkDownloadUrl, destinationPath, extractedDir, controller, dialogStage, callback);
            }
        };

        hookTaskDone(task, doneCallback);
        new Thread(task, "download-jdk-thread").start();
    }

    private void hookTaskDone(Task<Boolean> task, Consumer<Boolean> doneCallback) {
        task.setOnSucceeded(event -> notifyDone(doneCallback, Boolean.TRUE.equals(task.getValue())));
        task.setOnFailed(event -> {
            LoggerUtil.info("Background task failed: " + safeError(task.getException()));
            notifyDone(doneCallback, false);
        });
    }

    private void notifyDone(Consumer<Boolean> doneCallback, boolean success) {
        if (doneCallback != null) {
            doneCallback.accept(success);
        }
    }

    private File findNodeHome(File extractedDir) {
        File nodeExe = new File(extractedDir, "node.exe");
        if (nodeExe.exists()) {
            return extractedDir;
        }

        File[] subDirs = extractedDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                nodeExe = new File(subDir, "node.exe");
                if (nodeExe.exists()) {
                    return subDir;
                }
            }
        }
        return null;
    }

    private String findMavenHome(File extractedDir) {
        if (isValidMavenRoot(extractedDir)) {
            return extractedDir.getAbsolutePath();
        }

        File[] subDirs = extractedDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                if (isValidMavenRoot(subDir)) {
                    return subDir.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private boolean isValidMavenRoot(File dir) {
        return new File(dir, "conf").exists() && new File(dir, "bin").exists();
    }

    private void createMavenRepository(String mavenHome) {
        File repoDir = new File(mavenHome, "maven-repository");
        if (!repoDir.exists() && repoDir.mkdirs()) {
            LoggerUtil.info("Maven local repository created: " + repoDir.getAbsolutePath());
        } else if (repoDir.exists()) {
            LoggerUtil.info("Maven local repository already exists: " + repoDir.getAbsolutePath());
        } else {
            LoggerUtil.info("Create Maven local repository failed.");
        }
    }

    private void configureMavenSettings(String mavenHome) {
        File settingsFile = new File(mavenHome, "conf" + File.separator + "settings.xml");
        if (!settingsFile.exists()) {
            LoggerUtil.info("settings.xml not found.");
            return;
        }

        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
            }

            String localRepoPath = new File(mavenHome, "maven-repository").getAbsolutePath().replace("\\", "\\\\");
            String updated = content.toString();

            if (!updated.contains("<localRepository>")) {
                updated = updated.replace("</settings>",
                        "  <localRepository>" + localRepoPath + "</localRepository>" + System.lineSeparator() + "</settings>");
            }

            String aliyunMirror =
                    "    <mirror>" + System.lineSeparator() +
                    "      <id>aliyunmaven</id>" + System.lineSeparator() +
                    "      <mirrorOf>*</mirrorOf>" + System.lineSeparator() +
                    "      <name>aliyun maven</name>" + System.lineSeparator() +
                    "      <url>https://maven.aliyun.com/repository/public</url>" + System.lineSeparator() +
                    "    </mirror>" + System.lineSeparator();

            if (!updated.contains("<id>aliyunmaven</id>")) {
                if (updated.contains("<mirrors>")) {
                    updated = updated.replace("</mirrors>", aliyunMirror + "  </mirrors>");
                } else {
                    String mirrorsBlock = "  <mirrors>" + System.lineSeparator() + aliyunMirror + "  </mirrors>" + System.lineSeparator();
                    updated = updated.replace("</settings>", mirrorsBlock + "</settings>");
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile))) {
                writer.write(updated);
            }

            LoggerUtil.info("Maven settings updated. Local repo: " + localRepoPath);
        } catch (IOException e) {
            LoggerUtil.info("Update settings.xml failed: " + safeError(e));
        }
    }

    private boolean downloadFileWithProgress(String url,
                                             String destinationPath,
                                             String extractedDir,
                                             DownloadProgressDialogController controller,
                                             Stage stage,
                                             JdkDownloadCallback callback) {
        try {
            long contentLength = PathUtils.getFileSize(url);
            updateFileSizeLabel(controller, contentLength);

            boolean downloadSuccess = downloadToFile(url, destinationPath, contentLength, controller);
            if (!downloadSuccess || controller.isCancelRequested()) {
                LoggerUtil.info("Download canceled or failed.");
                Platform.runLater(stage::close);
                return false;
            }

            boolean unzipSuccess = unzipAndNotify(destinationPath, extractedDir, controller);
            if (!unzipSuccess) {
                Platform.runLater(stage::close);
                return false;
            }

            autoDeleteZipFile(destinationPath);

            if (callback != null) {
                callback.onDownloadComplete(extractedDir);
            }

            Platform.runLater(stage::close);
            return true;
        } catch (Exception e) {
            handleDownloadError(e, controller, stage);
            return false;
        }
    }

    private void updateFileSizeLabel(DownloadProgressDialogController controller, long contentLength) {
        if (contentLength <= 0) {
            Platform.runLater(() -> controller.sizeLabel.setText("File size unknown"));
            return;
        }

        String sizeText = String.format("File size: %.2f MB", contentLength / (1024.0 * 1024.0));
        Platform.runLater(() -> controller.sizeLabel.setText(sizeText));
    }

    private boolean downloadToFile(String url,
                                   String destinationPath,
                                   long contentLength,
                                   DownloadProgressDialogController controller) throws IOException {
        URL downloadUrl = new URL(url);
        byte[] buffer = new byte[1024];
        long totalBytesRead = 0;

        Platform.runLater(() -> controller.statusLabel.setText("Downloading..."));

        try (InputStream inputStream = new BufferedInputStream(downloadUrl.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destinationPath)) {

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (controller.isCancelRequested()) {
                    fileOutputStream.flush();
                    File file = new File(destinationPath);
                    if (file.exists() && !file.delete()) {
                        LoggerUtil.info("Delete temp file after cancel failed: " + destinationPath);
                    }
                    return false;
                }

                fileOutputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                String status;
                double progress;
                if (contentLength > 0) {
                    progress = (double) totalBytesRead / contentLength;
                    status = String.format("Downloaded %.2f MB / %.2f MB",
                            totalBytesRead / (1024.0 * 1024.0),
                            contentLength / (1024.0 * 1024.0));
                } else {
                    progress = -1;
                    status = String.format("Downloaded %.2f MB", totalBytesRead / (1024.0 * 1024.0));
                }

                double finalProgress = progress;
                Platform.runLater(() -> {
                    controller.progressBar.setProgress(finalProgress);
                    controller.statusLabel.setText(status);
                });
            }
        }

        Platform.runLater(() -> {
            controller.progressBar.setProgress(1.0);
            controller.statusLabel.setText("Download complete");
        });
        LoggerUtil.info("File downloaded: " + destinationPath);
        return true;
    }

    private boolean unzipAndNotify(String zipPath,
                                   String extractDir,
                                   DownloadProgressDialogController controller) {
        try {
            Platform.runLater(() -> controller.statusLabel.setText("Unzipping..."));
            PathUtils.unzipFile(zipPath, extractDir);
            Platform.runLater(() -> controller.statusLabel.setText("Unzip complete"));
            LoggerUtil.info("Unzipped to: " + extractDir);
            return true;
        } catch (IOException e) {
            Platform.runLater(() -> controller.statusLabel.setText("Unzip failed: " + safeError(e)));
            LoggerUtil.info("Unzip failed: " + safeError(e));
            return false;
        }
    }

    private void autoDeleteZipFile(String zipPath) {
        File zipFile = new File(zipPath);
        if (zipFile.exists() && zipFile.isFile()) {
            if (zipFile.delete()) {
                LoggerUtil.info("ZIP deleted: " + zipPath);
            } else {
                LoggerUtil.info("Delete ZIP failed: " + zipPath);
            }
        }
    }

    private void handleDownloadError(Exception e,
                                     DownloadProgressDialogController controller,
                                     Stage stage) {
        Platform.runLater(() -> {
            controller.progressBar.setProgress(0);
            controller.statusLabel.setText("Download failed: " + safeError(e));
            stage.close();
        });
        LoggerUtil.info("Download failed: " + safeError(e));
    }

    private DownloadProgressDialogController createDialog(String title) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle(title);
        URL fxmlUrl = MainApp.class.getResource("download-progress-dialog.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);

        try {
            Scene scene = new Scene(loader.load(), 450, 180);
            dialogStage.setScene(scene);
            dialogStage.show();
        } catch (IOException e) {
            LoggerUtil.info("Create progress dialog failed: " + safeError(e));
            return null;
        }

        return loader.getController();
    }

    private String safeError(Throwable t) {
        return t == null ? "unknown" : String.valueOf(t.getMessage());
    }
}
