package top.oneyi.envLauncher.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import top.oneyi.envLauncher.config.DownloadSourceConfig;
import top.oneyi.envLauncher.service.EnvInstallerService;
import top.oneyi.envLauncher.service.JdkEnvService;
import top.oneyi.envLauncher.service.MavenEnvService;
import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.File;

public class EnvInstallerController {

    @FXML
    private TextField installDirField;
    @FXML
    private TextArea outputArea;

    @FXML
    private ComboBox<String> jdkVersionCombo;
    @FXML
    private ComboBox<String> mavenVersionCombo;
    @FXML
    private ComboBox<String> nodeVersionCombo;

    @FXML
    private TextField jdkSourceField;
    @FXML
    private TextField mavenSourceField;
    @FXML
    private TextField nodeSourceField;

    @FXML
    private Button chooseInstallDirButton;
    @FXML
    private Button installJdkButton;
    @FXML
    private Button installMavenButton;
    @FXML
    private Button installNodeButton;
    @FXML
    private Button showConfigButton;
    @FXML
    private Button reloadSourcesButton;
    @FXML
    private Button saveSourcesButton;

    private final EnvInstallerService service = new EnvInstallerService();
    private final JdkEnvService jdkEnvService = new JdkEnvService();
    private final MavenEnvService mavenEnvService = new MavenEnvService();

    private boolean busy;

    public void onChooseInstallDirectory() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select install directory");
        File selectedDir = chooser.showDialog(null);
        if (selectedDir != null) {
            installDirField.setText(selectedDir.getAbsolutePath());
            LoggerUtil.info("Install directory selected: " + selectedDir.getAbsolutePath());
            refreshUiState();
        }
    }

    public void onSetEnvironmentVariables() {
        applyJdkEnvironment();
    }

    public void onShowCurrentConfig() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        try {
            String jdkEnv = jdkEnvService.getConfiguredJdkPath();
            String mavenEnv = mavenEnvService.getConfiguredMavenPath();
            LoggerUtil.info("Current JDK config: " + jdkEnv);
            LoggerUtil.info("Current Maven config: " + mavenEnv);
            LoggerUtil.info("Download source (JDK): " + DownloadSourceConfig.getJdkBaseUrl());
            LoggerUtil.info("Download source (Maven): " + DownloadSourceConfig.getMavenBaseUrl());
            LoggerUtil.info("Download source (Node): " + DownloadSourceConfig.getNodeBaseUrl());
        } catch (Exception e) {
            LoggerUtil.info("Read current config failed: " + safeError(e));
        }
    }

    public void onReloadDownloadSources() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        DownloadSourceConfig.reload();
        loadDownloadSourceFields();
        LoggerUtil.info("Download sources reloaded.");
    }

    public void onSaveDownloadSources() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        String jdkBase = safeTrim(jdkSourceField.getText());
        String mavenBase = safeTrim(mavenSourceField.getText());
        String nodeBase = safeTrim(nodeSourceField.getText());

        if (!isValidUrl(jdkBase) || !isValidUrl(mavenBase) || !isValidUrl(nodeBase)) {
            LoggerUtil.info("Save failed: source URL must start with http:// or https://");
            return;
        }

        try {
            DownloadSourceConfig.saveLocalOverrides(jdkBase, mavenBase, nodeBase);
            loadDownloadSourceFields();
            LoggerUtil.info("Download sources saved to: " + DownloadSourceConfig.getLocalOverridePath());
        } catch (Exception e) {
            LoggerUtil.info("Save download sources failed: " + safeError(e));
        }
    }

    public void onInstallJdk() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        String baseDir = requireInstallDirectory();
        if (baseDir == null) {
            return;
        }

        String version = jdkVersionCombo.getValue();
        if (version == null || version.isBlank()) {
            LoggerUtil.info("Please select a JDK version.");
            return;
        }

        setBusy(true);
        LoggerUtil.info("Start installing JDK " + version + " ...");

        service.downloadJdk(version, baseDir, this::handleJdkInstalled, success -> {
            if (success) {
                LoggerUtil.info("JDK files installed. Applying JDK environment variables...");
                applyJdkEnvironment();
            } else {
                LoggerUtil.info("JDK install did not complete.");
                setBusy(false);
            }
        });
    }

    public void onInstallMaven() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        String baseDir = requireInstallDirectory();
        if (baseDir == null) {
            return;
        }

        String version = mavenVersionCombo.getValue();
        setBusy(true);
        LoggerUtil.info("Start Maven install " + version + " ...");

        service.installMaven(version, baseDir, success -> {
            if (success) {
                LoggerUtil.info("Maven install completed.");
            } else {
                LoggerUtil.info("Maven install did not complete.");
            }
            setBusy(false);
        });
    }

    public void onInstallNode() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        String baseDir = requireInstallDirectory();
        if (baseDir == null) {
            return;
        }

        String version = nodeVersionCombo.getValue();
        if (version == null || version.isBlank()) {
            LoggerUtil.info("Please select a Node version.");
            return;
        }

        setBusy(true);
        LoggerUtil.info("Start Node install v" + version + " ...");

        service.installNode("v" + version, baseDir, success -> {
            if (success) {
                LoggerUtil.info("Node install completed.");
            } else {
                LoggerUtil.info("Node install did not complete.");
            }
            setBusy(false);
        });
    }

    @FXML
    private void initialize() {
        ObservableList<String> jdkVersions = FXCollections.observableArrayList("8", "11", "17", "21");
        jdkVersionCombo.setItems(jdkVersions);
        jdkVersionCombo.setValue("17");

        ObservableList<String> mavenVersions = FXCollections.observableArrayList("3.8.8", "3.9.8", "3.9.9", "3.9.10");
        mavenVersionCombo.setItems(mavenVersions);
        mavenVersionCombo.setValue("3.9.10");

        nodeVersionCombo.getItems().addAll("18.20.8", "20.19.2", "22.16.0");
        nodeVersionCombo.setValue("20.19.2");

        LoggerUtil.init(outputArea);
        LoggerUtil.info("UI initialized. Recommended versions selected.");

        loadDownloadSourceFields();
        refreshUiState();
    }

    private void handleJdkInstalled(String extractedJdkDir) {
        if (extractedJdkDir == null || extractedJdkDir.isBlank()) {
            LoggerUtil.info("Invalid JDK path.");
            return;
        }

        File extractedRoot = new File(extractedJdkDir);
        if (!extractedRoot.exists() || !extractedRoot.isDirectory()) {
            LoggerUtil.info("Extracted path is invalid: " + extractedJdkDir);
            return;
        }

        // Prefer the detected JDK home so later env setup targets the actual bin folder.
        File javaExeFile = PathUtils.findJavaExecutable(extractedRoot);
        if (javaExeFile != null) {
            File jdkHome = javaExeFile.getParentFile().getParentFile();
            LoggerUtil.info("JDK home auto-detected: " + jdkHome.getAbsolutePath());
        } else {
            LoggerUtil.info("java.exe not found. Use extracted path: " + extractedRoot.getAbsolutePath());
        }

        refreshUiState();
    }

    private void applyJdkEnvironment() {
        String javaHome = resolveInstalledJdkHome();
        if (javaHome == null || javaHome.isBlank()) {
            LoggerUtil.info("Please install JDK first.");
            setBusy(false);
            return;
        }

        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                jdkEnvService.configureJdkEnvironment(javaHome, "%JAVA_HOME%\\bin");
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            LoggerUtil.info("JDK install completed.");
            setBusy(false);
        });

        task.setOnFailed(event -> {
            LoggerUtil.info("Set JDK environment failed: " + safeError(task.getException()));
            setBusy(false);
        });

        new Thread(task, "set-jdk-env-thread").start();
    }

    private void setBusy(boolean busyState) {
        this.busy = busyState;
        refreshUiState();
    }

    private void refreshUiState() {
        boolean hasInstallDir = hasInstallDirectory();

        chooseInstallDirButton.setDisable(busy);
        installJdkButton.setDisable(busy || !hasInstallDir);
        installMavenButton.setDisable(busy || !hasInstallDir);
        installNodeButton.setDisable(busy || !hasInstallDir);
        showConfigButton.setDisable(busy);
        reloadSourcesButton.setDisable(busy);
        saveSourcesButton.setDisable(busy);

        jdkVersionCombo.setDisable(busy);
        mavenVersionCombo.setDisable(busy);
        nodeVersionCombo.setDisable(busy);
        jdkSourceField.setDisable(busy);
        mavenSourceField.setDisable(busy);
        nodeSourceField.setDisable(busy);
    }

    private void loadDownloadSourceFields() {
        jdkSourceField.setText(DownloadSourceConfig.getJdkBaseUrl());
        mavenSourceField.setText(DownloadSourceConfig.getMavenBaseUrl());
        nodeSourceField.setText(DownloadSourceConfig.getNodeBaseUrl());
    }

    private boolean hasInstallDirectory() {
        return installDirField.getText() != null && !installDirField.getText().isBlank();
    }

    private String requireInstallDirectory() {
        String baseDir = safeTrim(installDirField.getText());
        if (baseDir.isBlank()) {
            LoggerUtil.info("Please select install directory first.");
            return null;
        }
        return baseDir;
    }

    private String resolveInstalledJdkHome() {
        String baseDir = safeTrim(installDirField.getText());
        if (baseDir.isBlank()) {
            return null;
        }

        String version = safeTrim(jdkVersionCombo.getValue());
        if (version.isBlank()) {
            return null;
        }

        File extractedRoot = new File(PathUtils.getJdkExtractDir(baseDir, version));
        if (!extractedRoot.exists() || !extractedRoot.isDirectory()) {
            return null;
        }

        // Resolve the nested JDK home because different archives may unzip into an extra version folder.
        File javaExeFile = PathUtils.findJavaExecutable(extractedRoot);
        if (javaExeFile != null) {
            return javaExeFile.getParentFile().getParentFile().getAbsolutePath();
        }
        return extractedRoot.getAbsolutePath();
    }

    private boolean isValidUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeError(Throwable t) {
        return t == null ? "unknown" : String.valueOf(t.getMessage());
    }
}
