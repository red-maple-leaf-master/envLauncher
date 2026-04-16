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
import top.oneyi.envLauncher.service.EnvironmentSetupResult;
import top.oneyi.envLauncher.service.EnvInstallerService;
import top.oneyi.envLauncher.service.JdkEnvService;
import top.oneyi.envLauncher.service.MavenEnvService;
import top.oneyi.envLauncher.service.NodeEnvService;
import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;

import java.io.File;

public class EnvInstallerController {

    @FXML
    TextField installDirField;
    @FXML
    private TextArea outputArea;

    @FXML
    ComboBox<String> jdkVersionCombo;
    @FXML
    ComboBox<String> mavenVersionCombo;
    @FXML
    ComboBox<String> nodeVersionCombo;

    @FXML
    TextField jdkSourceField;
    @FXML
    TextField mavenSourceField;
    @FXML
    TextField nodeSourceField;

    @FXML
    Button chooseInstallDirButton;
    @FXML
    Button installJdkButton;
    @FXML
    Button useLocalJdkButton;
    @FXML
    Button installMavenButton;
    @FXML
    Button useLocalMavenButton;
    @FXML
    Button installNodeButton;
    @FXML
    Button useLocalNodeButton;
    @FXML
    Button showConfigButton;
    @FXML
    Button reloadSourcesButton;
    @FXML
    Button saveSourcesButton;

    private final EnvInstallerService service = new EnvInstallerService();
    private final JdkEnvService jdkEnvService = new JdkEnvService();
    private final MavenEnvService mavenEnvService = new MavenEnvService();
    private final NodeEnvService nodeEnvService = new NodeEnvService();

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

    public void onUseLocalNode() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        File selectedDir = chooseLocalDirectory("Select local Node directory");
        if (selectedDir == null) {
            LoggerUtil.info("Local Node selection cancelled.");
            return;
        }

        String nodeHome = resolveLocalNodeHomeForTest(selectedDir);
        if (nodeHome == null) {
            LoggerUtil.info("Selected directory is not a valid Node root. Missing node.exe.");
            return;
        }

        LoggerUtil.info("Local Node root selected: " + nodeHome);
        applyNodeEnvironment(nodeHome);
    }

    public void onUseLocalMaven() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        File selectedDir = chooseLocalDirectory("Select local Maven directory");
        if (selectedDir == null) {
            LoggerUtil.info("Local Maven selection cancelled.");
            return;
        }

        String mavenHome = resolveLocalMavenHomeForTest(selectedDir);
        if (mavenHome == null) {
            LoggerUtil.info("Selected directory is not a valid Maven root. Missing bin\\mvn.cmd or conf directory.");
            return;
        }

        LoggerUtil.info("Local Maven root selected: " + mavenHome);
        applyMavenEnvironment(mavenHome);
    }

    public void onUseLocalJdk() {
        if (busy) {
            LoggerUtil.info("Task is running. Please wait.");
            return;
        }

        File selectedDir = chooseLocalDirectory("Select local JDK directory");
        if (selectedDir == null) {
            LoggerUtil.info("Local JDK selection cancelled.");
            return;
        }

        String javaHome = resolveLocalJdkHomeForTest(selectedDir);
        if (javaHome == null) {
            LoggerUtil.info("Selected directory is not a valid JDK root. Missing bin\\java.exe.");
            return;
        }

        LoggerUtil.info("Local JDK root selected: " + javaHome);
        applyJdkEnvironment(javaHome);
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

        applyJdkEnvironment(javaHome);
    }

    private void applyJdkEnvironment(String javaHome) {
        setBusy(true);

        Task<EnvironmentSetupResult> task = new Task<>() {
            @Override
            protected EnvironmentSetupResult call() throws Exception {
                return jdkEnvService.configureJdkEnvironment(javaHome, "%JAVA_HOME%\\bin");
            }
        };

        task.setOnSucceeded(event -> {
            EnvironmentSetupResult result = task.getValue();
            if (result != null && result.isCompleted()) {
                LoggerUtil.info("JDK install completed.");
            } else {
                LoggerUtil.info("JDK files are installed, but environment setup is incomplete.");
            }
            setBusy(false);
        });

        task.setOnFailed(event -> {
            LoggerUtil.info("Set JDK environment failed: " + safeError(task.getException()));
            setBusy(false);
        });

        new Thread(task, "set-jdk-env-thread").start();
    }

    String resolveLocalJdkHomeForTest(File selectedDir) {
        return isValidLocalJdkRoot(selectedDir) ? selectedDir.getAbsolutePath() : null;
    }

    String resolveLocalMavenHomeForTest(File selectedDir) {
        return isValidLocalMavenRoot(selectedDir) ? selectedDir.getAbsolutePath() : null;
    }

    String resolveLocalNodeHomeForTest(File selectedDir) {
        return isValidLocalNodeRoot(selectedDir) ? selectedDir.getAbsolutePath() : null;
    }

    private void setBusy(boolean busyState) {
        this.busy = busyState;
        refreshUiState();
    }

    void setBusyForTest(boolean busyState) {
        setBusy(busyState);
    }

    private void refreshUiState() {
        boolean hasInstallDir = hasInstallDirectory();

        chooseInstallDirButton.setDisable(busy);
        installJdkButton.setDisable(busy || !hasInstallDir);
        useLocalJdkButton.setDisable(busy);
        installMavenButton.setDisable(busy || !hasInstallDir);
        useLocalMavenButton.setDisable(busy);
        installNodeButton.setDisable(busy || !hasInstallDir);
        useLocalNodeButton.setDisable(busy);
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

    private File chooseLocalDirectory(String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        return chooser.showDialog(null);
    }

    private void applyMavenEnvironment(String mavenHome) {
        setBusy(true);

        Task<EnvironmentSetupResult> task = new Task<>() {
            @Override
            protected EnvironmentSetupResult call() throws Exception {
                return mavenEnvService.configureMavenEnvironment(mavenHome, mavenHome + "\\bin");
            }
        };

        task.setOnSucceeded(event -> {
            EnvironmentSetupResult result = task.getValue();
            if (result != null && result.isCompleted()) {
                LoggerUtil.info("Maven install completed.");
            } else {
                LoggerUtil.info("Maven files are installed, but environment setup is incomplete.");
            }
            setBusy(false);
        });

        task.setOnFailed(event -> {
            LoggerUtil.info("Set Maven environment failed: " + safeError(task.getException()));
            setBusy(false);
        });

        new Thread(task, "set-maven-env-thread").start();
    }

    private void applyNodeEnvironment(String nodeHome) {
        setBusy(true);

        Task<EnvironmentSetupResult> task = new Task<>() {
            @Override
            protected EnvironmentSetupResult call() throws Exception {
                return nodeEnvService.configureNodeEnvironment(nodeHome, "%NODE_HOME%");
            }
        };

        task.setOnSucceeded(event -> {
            EnvironmentSetupResult result = task.getValue();
            if (result != null && result.isCompleted()) {
                LoggerUtil.info("Node install completed.");
            } else {
                LoggerUtil.info("Node files are installed, but environment setup is incomplete.");
            }
            setBusy(false);
        });

        task.setOnFailed(event -> {
            LoggerUtil.info("Set Node environment failed: " + safeError(task.getException()));
            setBusy(false);
        });

        new Thread(task, "set-node-env-thread").start();
    }

    private boolean isValidUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    // Local import requires the user to point at the tool root so validation stays deterministic.
    static boolean isValidLocalJdkRoot(File root) {
        return root != null
                && root.isDirectory()
                && new File(root, "bin" + File.separator + "java.exe").isFile();
    }

    static boolean isValidLocalMavenRoot(File root) {
        return root != null
                && root.isDirectory()
                && new File(root, "bin" + File.separator + "mvn.cmd").isFile()
                && new File(root, "conf").isDirectory();
    }

    static boolean isValidLocalNodeRoot(File root) {
        return root != null
                && root.isDirectory()
                && new File(root, "node.exe").isFile();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeError(Throwable t) {
        return t == null ? "unknown" : String.valueOf(t.getMessage());
    }
}
