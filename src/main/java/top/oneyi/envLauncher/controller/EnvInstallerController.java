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
import top.oneyi.envLauncher.utils.EnvUtil;
import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.File;

public class EnvInstallerController {

    @FXML
    private TextField jdkPathField;
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
    private Button chooseJdkButton;
    @FXML
    private Button downloadJdkButton;
    @FXML
    private Button setupMavenButton;
    @FXML
    private Button setupNodeButton;
    @FXML
    private Button setEnvButton;
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
        chooser.setTitle("Select JDK directory");
        File selectedDir = chooser.showDialog(null);
        if (selectedDir != null) {
            jdkPathField.setText(selectedDir.getAbsolutePath());
            jdkReady = true;
            LoggerUtil.info("JDK dir selected: " + selectedDir.getAbsolutePath());
            refreshUiState();
        }
    }

    public void onSetEnvironmentVariables() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        String javaHome = jdkPathField.getText();
        if (javaHome == null || javaHome.isBlank()) {
            LoggerUtil.info("Please select JDK directory first.");
            return;
        }

        setBusy(true, "Step 4/4: Set JDK environment variables");
        LoggerUtil.info("Setting JDK environment variables...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                EnvUtil.setJdkEnvironmentVariables(javaHome, "%JAVA_HOME%\\bin");
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            jdkEnvReady = true;
            LoggerUtil.info("JDK environment variables set. Restart terminal or IDE.");
            setBusy(false, null);
        });

        task.setOnFailed(event -> {
            LoggerUtil.info("Set JDK environment failed: " + safeError(task.getException()));
            setBusy(false, null);
        });

        new Thread(task, "set-jdk-env-thread").start();
    }

    public void onShowCurrentConfig() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        try {
            String jdkEnv = EnvUtil.getJdkEnvironmentVariables();
            String mavenEnv = EnvUtil.getMavenEnvironmentVariables();
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

        String version = jdkVersionCombo.getValue();
        if (version == null || version.isBlank()) {
            LoggerUtil.info("Please select a JDK version.");
            return;
        }

        setBusy(true, "Step 2/4: Download JDK");
        LoggerUtil.info("Start downloading JDK " + version + " ...");

        service.onDownloadJdk(version, this::updateJdkPathInput, success -> {
            if (success) {
                jdkReady = true;
                LoggerUtil.info("JDK download and unzip completed.");
            } else {
                LoggerUtil.info("JDK download flow did not complete.");
            }
            setBusy(false, null);
        });
    }

    public void onSetupMaven() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        String version = mavenVersionCombo.getValue();
        setBusy(true, "Step 3/4: Setup Maven");
        LoggerUtil.info("Start Maven setup " + version + " ...");

        service.onSetupMaven(version, success -> {
            if (success) {
                mavenReady = true;
                LoggerUtil.info("Maven setup completed.");
            } else {
                LoggerUtil.info("Maven setup did not complete.");
            }
            setBusy(false, null);
        });
    }

    public void onSetupNode() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        String version = nodeVersionCombo.getValue();
        if (version == null || version.isBlank()) {
            LoggerUtil.info("Please select a Node version.");
            return;
        }

        setBusy(true, "Step 3/4: Setup Node");
        LoggerUtil.info("Start Node setup v" + version + " ...");

        service.onSetupNode("v" + version, success -> {
            if (success) {
                nodeReady = true;
                LoggerUtil.info("Node setup completed.");
            } else {
                LoggerUtil.info("Node setup did not complete.");
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

        if (!hasJdkPath()) {
            LoggerUtil.info("No JDK path detected. Download JDK first.");
            startOneClickJdk();
            return;
        }

        jdkReady = true;
        startOneClickMaven();
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
            jdkPathField.setText(jdkHome.getAbsolutePath());
            LoggerUtil.info("JDK home auto-detected: " + jdkHome.getAbsolutePath());
        } else {
            jdkPathField.setText(extractedRoot.getAbsolutePath());
            LoggerUtil.info("java.exe not found. Use extracted path: " + extractedRoot.getAbsolutePath());
        }

        jdkReady = true;
        refreshUiState();
    }

    private void startOneClickJdk() {
        String version = jdkVersionCombo.getValue();
        if (version == null || version.isBlank()) {
            LoggerUtil.info("One-click failed: no JDK version selected.");
            refreshUiState();
            return;
        }

        setBusy(true, "Step 2/4: Download JDK");
        service.onDownloadJdk(version, this::updateJdkPathInput, success -> {
            if (!success) {
                LoggerUtil.info("One-click interrupted: JDK download failed.");
                setBusy(false, null);
                return;
            }
            jdkReady = true;
            LoggerUtil.info("One-click: JDK done, continue Maven.");
            startOneClickMaven();
        });
    }

    private void startOneClickMaven() {
        setBusy(true, "Step 3/4: Setup Maven");
        String mavenVersion = mavenVersionCombo.getValue();
        service.onSetupMaven(mavenVersion, success -> {
            if (!success) {
                LoggerUtil.info("One-click interrupted: Maven setup failed.");
                setBusy(false, null);
                return;
            }
            mavenReady = true;
            LoggerUtil.info("One-click: Maven done, continue Node.");
            startOneClickNode();
        });
    }

    private void startOneClickNode() {
        setBusy(true, "Step 3/4: Setup Node");
        String nodeVersion = nodeVersionCombo.getValue();
        if (nodeVersion == null || nodeVersion.isBlank()) {
            LoggerUtil.info("One-click interrupted: no Node version selected.");
            setBusy(false, null);
            return;
        }

        service.onSetupNode("v" + nodeVersion, success -> {
            if (!success) {
                LoggerUtil.info("One-click interrupted: Node setup failed.");
                setBusy(false, null);
                return;
            }
            nodeReady = true;
            LoggerUtil.info("One-click: Node done, continue JDK env.");
            startOneClickSetJdkEnv();
        });
    }

    private void startOneClickSetJdkEnv() {
        String javaHome = jdkPathField.getText();
        if (javaHome == null || javaHome.isBlank()) {
            LoggerUtil.info("One-click interrupted: JDK path is empty.");
            setBusy(false, null);
            return;
        }

        setBusy(true, "Step 4/4: Set JDK environment variables");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                EnvUtil.setJdkEnvironmentVariables(javaHome, "%JAVA_HOME%\\bin");
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            jdkEnvReady = true;
            LoggerUtil.info("One-click completed.");
            setBusy(false, "Done: click Show Config to verify");
        });

        task.setOnFailed(event -> {
            LoggerUtil.info("One-click interrupted at JDK env step: " + safeError(task.getException()));
            setBusy(false, null);
        });

        new Thread(task, "one-click-set-env-thread").start();
    }

    private void setBusy(boolean busyState, String customStepText) {
        this.busy = busyState;
        if (customStepText != null && !customStepText.isBlank()) {
            flowStepLabel.setText(customStepText);
        }
        refreshUiState();
    }

    private void refreshUiState() {
        boolean hasJdk = hasJdkPath();

        chooseJdkButton.setDisable(busy);
        downloadJdkButton.setDisable(busy);
        setupMavenButton.setDisable(busy);
        setupNodeButton.setDisable(busy);
        showConfigButton.setDisable(busy);
        oneClickInstallButton.setDisable(busy);
        reloadSourcesButton.setDisable(busy);
        saveSourcesButton.setDisable(busy);

        jdkVersionCombo.setDisable(busy);
        mavenVersionCombo.setDisable(busy);
        nodeVersionCombo.setDisable(busy);
        jdkSourceField.setDisable(busy);
        mavenSourceField.setDisable(busy);
        nodeSourceField.setDisable(busy);

        setEnvButton.setDisable(busy || !hasJdk);

        if (busy) {
            return;
        }
        if (!hasJdk) {
            flowStepLabel.setText("Step 1/4: Select JDK directory");
            return;
        }
        if (!jdkReady) {
            flowStepLabel.setText("Step 2/4: Download JDK");
            return;
        }
        if (!mavenReady || !nodeReady) {
            flowStepLabel.setText("Step 3/4: Setup Maven/Node");
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

    private boolean hasJdkPath() {
        return jdkPathField.getText() != null && !jdkPathField.getText().isBlank();
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
