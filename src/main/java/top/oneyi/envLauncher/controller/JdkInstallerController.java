package top.oneyi.envLauncher.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import top.oneyi.envLauncher.service.JdkInstallerService;
import top.oneyi.envLauncher.utils.EnvUtil;
import top.oneyi.envLauncher.utils.LoggerUtil;
import top.oneyi.envLauncher.utils.PathUtils;



import java.io.File;

/**
 * java 相关环境设置
 */
public class JdkInstallerController {

    @FXML
    private TextField jdkPathField;

    @FXML
    private TextArea outputArea;

    @FXML
    private ComboBox<String> jdkVersionCombo;

    @FXML
    private ComboBox<String> mavenVersionCombo;


    /**
     * 选择 JDK 安装目录
     */
    public void onChooseJdkDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("请选择 JDK 安装目录");
        File selectedDir = chooser.showDialog(null);
        if (selectedDir != null) {
            jdkPathField.setText(selectedDir.getAbsolutePath());
        }
    }

public void onSetEnvironmentVariables() {
    String javaHome = jdkPathField.getText();
    if (javaHome.isEmpty()) {
        LoggerUtil.info("⚠️ 请先选择 JDK 目录");
        return;
    }

    LoggerUtil.info("⚙ 正在设置环境变量...");

    // 使用 Task 执行异步操作
    Task<Void> task = new Task<>() {
        @Override
        protected Void call() throws Exception {
            try {
                EnvUtil.setJdkEnvironmentVariables(javaHome, "%JAVA_HOME%\\bin");
                Platform.runLater(() ->
                    LoggerUtil.info("✅ 环境变量设置完成，请重启终端或 IDE 生效.")
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    LoggerUtil.info("⚠️ 设置环境变量失败");
                });
                throw new RuntimeException(e);
            }
            return null;
        }
    };

    new Thread(task).start();
}


    /**
     * 显示当前 JDK 配置
     */
    public void onShowCurrentConfig() {
        try {
            String jdkEnv = EnvUtil.getJdkEnvironmentVariables(); // 可以重定向输出到 TextArea
            LoggerUtil.info("🔍 当前 JDK 配置：" + jdkEnv);
            String mavenEnvironmentVariables = EnvUtil.getMavenEnvironmentVariables();
            LoggerUtil.info("🔍 当前 Maven 配置：" + mavenEnvironmentVariables);
        } catch (Exception e) {
            LoggerUtil.info("⚠️ 获取配置失败");
            e.printStackTrace();
        }

    }

    /**
     * 初始化 基本数据
     */
    @FXML
    private void initialize() {
        // 初始化 ComboBox 数据
        ObservableList<String> versions = FXCollections.observableArrayList("8", "11", "17", "21");
        jdkVersionCombo.setItems(versions);
        jdkVersionCombo.getSelectionModel().selectFirst(); // 默认选择第一个项

        // Maven 版本列表（根据图片内容补充）
        ObservableList<String> mavenVersions = FXCollections.observableArrayList(
                "3.0.4", "3.0.5", "3.1.0-alpha-1", "3.1.0", "3.1.1", "3.2.1", "3.2.2", "3.2.3", "3.2.5",
                "3.3.1", "3.3.3", "3.3.9", "3.5.0-alpha-1", "3.5.0-beta-1", "3.5.0", "3.5.2", "3.5.3",
                "3.5.4", "3.6.0", "3.6.1", "3.6.2", "3.6.3", "3.8.1", "3.8.2", "3.8.3", "3.8.4", "3.8.5",
                "3.8.6", "3.8.7", "3.8.8", "3.9.0", "3.9.1", "3.9.10", "3.9.2", "3.9.3", "3.9.4", "3.9.5",
                "3.9.6", "3.9.7", "3.9.8", "3.9.9"
        );
        mavenVersionCombo.setItems(mavenVersions);
        mavenVersionCombo.getSelectionModel().selectFirst(); // 默认选择第一个项

        // 设置日志输出类
        LoggerUtil.init(outputArea);

    }

    /**
     * 一键设置 Maven
     */
    public void onSetupMaven() {
        JdkInstallerService service = new JdkInstallerService();
        String version = mavenVersionCombo.getValue();
        service.onSetupMaven(version, this::updateJdkPathInput);
    }


    /**
     * 下载 JDK
     */
    public void onDownloadJdk() {
        JdkInstallerService service = new JdkInstallerService();
        String selectedVersion = jdkVersionCombo.getValue();
        if (selectedVersion != null) {
            // 根据 selectedVersion 执行下载逻辑
            service.onDownloadJdk(selectedVersion, this::updateJdkPathInput);
        } else {
            LoggerUtil.info("❌ 请选择 JDK 版本");
        }
    }


    /**
     * 回调方法：更新 JDK 输入框路径（自动识别解压后的子目录）
     *
     * @param jdkExtractedPath 解压后的根路径（如 D:\environment\jdk-17）
     */
    private void updateJdkPathInput(String jdkExtractedPath) {
        if (jdkExtractedPath == null || jdkExtractedPath.isEmpty() || !jdkExtractedPath.toLowerCase().contains("jdk")) {
            LoggerUtil.info("⚠ 无效的 JDK 路径");
            return;
        }

        File extractedRoot = new File(jdkExtractedPath);

        // ✅ 检查是否为有效目录
        if (!extractedRoot.exists() || !extractedRoot.isDirectory()) {
            LoggerUtil.info("⚠ 解压路径不存在或不是一个有效目录: " + jdkExtractedPath);
            return;
        }

        // ✅ 自动查找包含 bin/java.exe 的子目录（兼容解压后多一层目录的情况）
        File javaExeFile = PathUtils.findJavaExecutable(extractedRoot);

        if (javaExeFile != null) {
            File jdkHome = javaExeFile.getParentFile().getParentFile(); // 定位到 JDK 根目录
            jdkPathField.setText(jdkHome.getAbsolutePath());
            LoggerUtil.info("✅ 已自动定位到 JDK 根目录: " + jdkHome.getAbsolutePath());
        } else {
            jdkPathField.setText(extractedRoot.getAbsolutePath());
            LoggerUtil.info("⚠️ 未找到 java.exe，已使用默认路径: " + extractedRoot.getAbsolutePath());
        }
    }
}
