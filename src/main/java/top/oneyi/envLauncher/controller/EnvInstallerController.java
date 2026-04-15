package top.oneyi.envLauncher.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
    private Button oneClickInstallButton;
    @FXML
    private Button reloadSourcesButton;
    @FXML
    private Button saveSourcesButton;

    @FXML
    private Label flowStepLabel;

    private final EnvInstallerService service = new EnvInstallerService();
    private final JdkEnvService jdkEnvService = new JdkEnvService();
    private final MavenEnvService mavenEnvService = new MavenEnvService();

    private boolean jdkReady;
    private boolean mavenReady;
    private boolean nodeReady;
    private boolean jdkEnvReady;
    private boolean busy;

    public void onChooseJdkDir() {
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
        applyJdkEnvironment(false);
    }

    public void onShowCurrentConfig() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        try {
            String jdkEnv = jdkEnvService.getJdkEnvironmentVariables();
            String mavenEnv = mavenEnvService.getMavenEnvironmentVariables();
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

    public void onDownloadJdk() {
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

        setBusy(true, "Step 2/4: Install JDK");
        LoggerUtil.info("Start installing JDK " + version + " ...");

        service.onDownloadJdk(version, baseDir, this::updateJdkPathInput, success -> {
            if (success) {
                LoggerUtil.info("JDK files installed. Applying JDK environment variables...");
                applyJdkEnvironment(false);
            } else {
                LoggerUtil.info("JDK install did not complete.");
                setBusy(false, null);
            }
        });
    }

    public void onSetupMaven() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        String baseDir = requireInstallDirectory();
        if (baseDir == null) {
            return;
        }

        String version = mavenVersionCombo.getValue();
        setBusy(true, "Step 3/4: Install Maven");
        LoggerUtil.info("Start Maven install " + version + " ...");

        service.onSetupMaven(version, baseDir, success -> {
            if (success) {
                mavenReady = true;
                LoggerUtil.info("Maven install completed.");
            } else {
                LoggerUtil.info("Maven install did not complete.");
            }
            setBusy(false, null);
        });
    }

    public void onSetupNode() {
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

        setBusy(true, "Step 3/4: Install Node");
        LoggerUtil.info("Start Node install v" + version + " ...");

        service.onSetupNode("v" + version, baseDir, success -> {
            if (success) {
                nodeReady = true;
                LoggerUtil.info("Node install completed.");
            } else {
                LoggerUtil.info("Node install did not complete.");
            }
            setBusy(false, null);
        });
    }

    public void onOneClickInstall() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        LoggerUtil.info("Start one-click install flow.");

        String baseDir = requireInstallDirectory();
        if (baseDir == null) {
            return;
        }

        if (!jdkReady) {
            LoggerUtil.info("No JDK detected. Start JDK install first.");
            startOneClickJdk(baseDir);
            return;
        }

        startOneClickMaven(baseDir);
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

    private void updateJdkPathInput(String jdkExtractedPath) {
        if (jdkExtractedPath == null || jdkExtractedPath.isBlank()) {
            LoggerUtil.info("Invalid JDK path.");
            return;
        }

        File extractedRoot = new File(jdkExtractedPath);
        if (!extractedRoot.exists() || !extractedRoot.isDirectory()) {
            LoggerUtil.info("Extracted path is invalid: " + jdkExtractedPath);
            return;
        }

        File javaExeFile = PathUtils.findJavaExecutable(extractedRoot);
        if (javaExeFile != null) {
            File jdkHome = javaExeFile.getParentFile().getParentFile();
            LoggerUtil.info("JDK home auto-detected: " + jdkHome.getAbsolutePath());
        } else {
            LoggerUtil.info("java.exe not found. Use extracted path: " + extractedRoot.getAbsolutePath());
        }

        jdkReady = true;
        refreshUiState();
    }

    private void startOneClickJdk(String baseDir) {
        String version = jdkVersionCombo.getValue();
        if (version == null || version.isBlank()) {
            LoggerUtil.info("One-click failed: no JDK version selected.");
            refreshUiState();
            return;
        }

        setBusy(true, "Step 2/4: Install JDK");
        service.onDownloadJdk(version, baseDir, this::updateJdkPathInput, success -> {
            if (!success) {
                LoggerUtil.info("One-click interrupted: JDK install failed.");
                setBusy(false, null);
                return;
            }
            LoggerUtil.info("One-click: JDK files installed. Applying JDK environment variables...");
            applyJdkEnvironment(true, () -> startOneClickMaven(baseDir));
        });
    }

    private void startOneClickMaven(String baseDir) {
        setBusy(true, "Step 3/4: Install Maven");
        String mavenVersion = mavenVersionCombo.getValue();
        service.onSetupMaven(mavenVersion, baseDir, success -> {
            if (!success) {
                LoggerUtil.info("One-click interrupted: Maven install failed.");
                setBusy(false, null);
                return;
            }
            mavenReady = true;
            LoggerUtil.info("One-click: Maven done, continue Node.");
            startOneClickNode(baseDir);
        });
    }

    private void startOneClickNode(String baseDir) {
        setBusy(true, "Step 3/4: Install Node");
        String nodeVersion = nodeVersionCombo.getValue();
        if (nodeVersion == null || nodeVersion.isBlank()) {
            LoggerUtil.info("One-click interrupted: no Node version selected.");
            setBusy(false, null);
            return;
        }

        service.onSetupNode("v" + nodeVersion, baseDir, success -> {
            if (!success) {
                LoggerUtil.info("One-click interrupted: Node install failed.");
                setBusy(false, null);
                return;
            }
            nodeReady = true;
            jdkEnvReady = true;
            LoggerUtil.info("One-click completed.");
            setBusy(false, "Done: click Show Config to verify");
        });
    }

    private void applyJdkEnvironment(boolean continueFlow) {
        applyJdkEnvironment(continueFlow, null);
    }

    private void applyJdkEnvironment(boolean continueFlow, Runnable onSuccess) {
        String javaHome = resolveInstalledJdkHome();
        if (javaHome == null || javaHome.isBlank()) {
            LoggerUtil.info(continueFlow ? "One-click interrupted: JDK home is empty." : "Please install JDK first.");
            setBusy(false, null);
            return;
        }

        setBusy(true, "Step 4/4: Set JDK environment variables");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                jdkEnvService.setJdkEnvironmentVariables(javaHome, "%JAVA_HOME%\\bin");
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            jdkEnvReady = true;
            if (continueFlow) {
                LoggerUtil.info("One-click: JDK environment variables applied. Continue Maven.");
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                LoggerUtil.info("JDK install completed.");
                setBusy(false, null);
            }
        });

        task.setOnFailed(event -> {
            LoggerUtil.info((continueFlow ? "One-click interrupted at JDK env step: " : "Set JDK environment failed: ") + safeError(task.getException()));
            setBusy(false, null);
        });

        new Thread(task, continueFlow ? "one-click-set-env-thread" : "set-jdk-env-thread").start();
    }

    private void setBusy(boolean busyState, String customStepText) {
        this.busy = busyState;
        if (customStepText != null && !customStepText.isBlank()) {
            flowStepLabel.setText(customStepText);
        }
        refreshUiState();
    }

    private void refreshUiState() {
        boolean hasInstallDir = hasInstallDirectory();

        chooseInstallDirButton.setDisable(busy);
        installJdkButton.setDisable(busy || !hasInstallDir);
        installMavenButton.setDisable(busy || !hasInstallDir);
        installNodeButton.setDisable(busy || !hasInstallDir);
        showConfigButton.setDisable(busy);
        oneClickInstallButton.setDisable(busy || !hasInstallDir);
        reloadSourcesButton.setDisable(busy);
        saveSourcesButton.setDisable(busy);

        jdkVersionCombo.setDisable(busy);
        mavenVersionCombo.setDisable(busy);
        nodeVersionCombo.setDisable(busy);
        jdkSourceField.setDisable(busy);
        mavenSourceField.setDisable(busy);
        nodeSourceField.setDisable(busy);

        if (busy) {
            return;
        }
        if (!hasInstallDir) {
            flowStepLabel.setText("Step 1/4: Select install directory");
            return;
        }
        if (!jdkReady) {
            flowStepLabel.setText("Step 2/4: Install JDK");
            return;
        }
        if (!mavenReady || !nodeReady) {
            flowStepLabel.setText("Step 3/4: Install Maven/Node");
            return;
        }
        if (!jdkEnvReady) {
            flowStepLabel.setText("Step 4/4: Set JDK environment variables");
            return;
        }
        flowStepLabel.setText("Done: click Show Config to verify");
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
