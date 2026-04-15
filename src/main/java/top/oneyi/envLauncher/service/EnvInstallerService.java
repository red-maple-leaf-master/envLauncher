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
import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class EnvInstallerService {

    private static final String DEFAULT_INSTALL_ROOT = PathUtils.getCurrentDrive() + "environment";

    private Stage dialogStage;
    private final MavenEnvService mavenEnvService = new MavenEnvService();
    private final NodeEnvService nodeEnvService = new NodeEnvService();

    public void installNode(String version) {
        installNode(version, DEFAULT_INSTALL_ROOT, null);
    }

    public void installNode(String version, Consumer<Boolean> doneCallback) {
        installNode(version, DEFAULT_INSTALL_ROOT, doneCallback);
    }

    public void installNode(String version, String baseDir, Consumer<Boolean> doneCallback) {
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
                    nodeEnvService.configureNodeEnvironment(nodeHome, "%NODE_HOME%");
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

    public void installMaven(String version) {
        installMaven(version, DEFAULT_INSTALL_ROOT, null);
    }

    public void installMaven(String version, Consumer<Boolean> doneCallback) {
        installMaven(version, DEFAULT_INSTALL_ROOT, doneCallback);
    }

    public void installMaven(String version, String baseDir, Consumer<Boolean> doneCallback) {
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
                    mavenEnvService.configureMavenEnvironment(mavenHome, mavenHome + "\\bin");
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

    public void downloadJdk(String version, JdkDownloadCallback callback) {
        downloadJdk(version, DEFAULT_INSTALL_ROOT, callback, null);
    }

    public void downloadJdk(String version,
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

        String destinationPath = PathUtils.getJdkDownloadPath(baseDir, version);
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
            String original = Files.readString(settingsFile.toPath(), StandardCharsets.UTF_8);
            String updated = updateMavenSettingsContent(original, mavenHome);
            Files.writeString(settingsFile.toPath(), updated, StandardCharsets.UTF_8);
            String localRepoPath = new File(mavenHome, "maven-repository").getAbsolutePath();
            LoggerUtil.info("Maven settings updated. Local repo: " + localRepoPath);
        } catch (Exception e) {
            LoggerUtil.info("Update settings.xml failed: " + safeError(e));
        }
    }

    static String updateMavenSettingsContent(String originalContent, String mavenHome) throws Exception {
        String localRepoPath = new File(mavenHome, "maven-repository").getAbsolutePath();
        Document document = parseXml(originalContent);
        Element settings = document.getDocumentElement();

        upsertLocalRepository(document, settings, localRepoPath);
        ensureAliyunMirror(document, settings);

        return toXml(document);
    }

    private static Document parseXml(String content) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(false);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(content)));
    }

    private static void upsertLocalRepository(Document document, Element settings, String localRepoPath) {
        NodeList repositoryNodes = settings.getElementsByTagName("localRepository");
        if (repositoryNodes.getLength() > 0) {
            repositoryNodes.item(0).setTextContent(localRepoPath);
            return;
        }

        Element localRepository = document.createElement("localRepository");
        localRepository.setTextContent(localRepoPath);
        settings.appendChild(localRepository);
    }

    private static void ensureAliyunMirror(Document document, Element settings) {
        Element mirrors = findOrCreateChild(document, settings, "mirrors");
        removeMirrorById(mirrors, "maven-default-http-blocker");
        if (containsMirrorId(mirrors, "aliyunmaven")) {
            return;
        }

        Element mirror = document.createElement("mirror");
        appendChildWithText(document, mirror, "id", "aliyunmaven");
        appendChildWithText(document, mirror, "mirrorOf", "*");
        appendChildWithText(document, mirror, "name", "aliyun maven");
        appendChildWithText(document, mirror, "url", "https://maven.aliyun.com/repository/public");
        mirrors.appendChild(mirror);
    }

    private static void removeMirrorById(Element mirrors, String mirrorId) {
        NodeList mirrorNodes = mirrors.getElementsByTagName("mirror");
        for (int i = mirrorNodes.getLength() - 1; i >= 0; i--) {
            Element mirror = (Element) mirrorNodes.item(i);
            NodeList idNodes = mirror.getElementsByTagName("id");
            if (idNodes.getLength() > 0 && mirrorId.equals(idNodes.item(0).getTextContent().trim())) {
                mirrors.removeChild(mirror);
            }
        }
    }

    private static Element findOrCreateChild(Document document, Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }

        Element child = document.createElement(tagName);
        parent.appendChild(child);
        return child;
    }

    private static boolean containsMirrorId(Element mirrors, String mirrorId) {
        NodeList mirrorNodes = mirrors.getElementsByTagName("mirror");
        for (int i = 0; i < mirrorNodes.getLength(); i++) {
            Element mirror = (Element) mirrorNodes.item(i);
            NodeList idNodes = mirror.getElementsByTagName("id");
            if (idNodes.getLength() > 0 && mirrorId.equals(idNodes.item(0).getTextContent().trim())) {
                return true;
            }
        }
        return false;
    }

    private static void appendChildWithText(Document document, Element parent, String tagName, String value) {
        Element child = document.createElement(tagName);
        child.setTextContent(value);
        parent.appendChild(child);
    }

    private static String toXml(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
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
