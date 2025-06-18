package top.oneyi.jdktool.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import top.oneyi.jdktool.service.JdkInstallerService;
import top.oneyi.jdktool.utils.JDKUtil;
import top.oneyi.jdktool.utils.PathUtils;

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

    /**
     * 设置全局的 Java 环境变量
     */
    public void onSetEnvironmentVariables() {

        String javaHome = jdkPathField.getText();
        if (javaHome.isEmpty()) {
            outputArea.appendText("⚠️ 请先选择 JDK 目录\n");
            return;
        }

        outputArea.appendText("⚙️ 正在设置环境变量...\n");
        Platform.runLater(() -> {
            try {
                JDKUtil.setJdkEnvironmentVariables(javaHome, "%JAVA_HOME%\\bin");
            } catch (Exception e) {
                outputArea.appendText("⚠️ 设置环境变量失败\n");
                throw new RuntimeException(e);
            }
            outputArea.appendText("✅ 环境变量设置完成，请重启终端或 IDE 生效。\n");
        });
    }

    /**
     * 显示当前 JDK 配置
     */
    public void onShowCurrentConfig() {
        try {
            String jdkEnv = JDKUtil.getJdkEnvironmentVariables(); // 可以重定向输出到 TextArea
            outputArea.appendText("🔍 当前 JDK 配置：" + jdkEnv + "\n");
        } catch (Exception e) {
            outputArea.appendText("⚠️ 获取 JDK 配置失败\n");
            e.printStackTrace();
        }

    }

    /**
     * 初始化 ComboBox 数据
     */
    @FXML
    private void initialize() {
        // 初始化 ComboBox 数据
        ObservableList<String> versions = FXCollections.observableArrayList("8", "11", "17", "21");
        jdkVersionCombo.setItems(versions);
        jdkVersionCombo.getSelectionModel().selectFirst(); // 默认选择第一个项
    }

    /**
     * 一键设置 Maven
     */
    public void onSetupMaven() {
        JdkInstallerService service = new JdkInstallerService();
        service.onSetupMaven(outputArea);
    }


    /**
     * 下载 JDK
     */
    public void onDownloadJdk() {
        JdkInstallerService service = new JdkInstallerService();
        String selectedVersion = jdkVersionCombo.getValue();
        if (selectedVersion != null) {
            // 根据 selectedVersion 执行下载逻辑
            service.onDownloadJdk(outputArea, selectedVersion, this::updateJdkPathInput);
        } else {
            outputArea.appendText("❌ 请选择 JDK 版本\n");
        }
    }


    /**
     * 回调方法：更新 JDK 输入框路径（自动识别解压后的子目录）
     * @param jdkExtractedPath 解压后的根路径（如 D:\environment\jdk-17）
     */
    private void updateJdkPathInput(String jdkExtractedPath) {
        if (jdkExtractedPath == null || jdkExtractedPath.isEmpty()) {
            outputArea.appendText("⚠️ 无效的 JDK 路径\n");
            return;
        }

        File extractedRoot = new File(jdkExtractedPath);

        // ✅ 检查是否为有效目录
        if (!extractedRoot.exists() || !extractedRoot.isDirectory()) {
            outputArea.appendText("⚠️ 解压路径不存在或不是一个有效目录: " + jdkExtractedPath + "\n");
            return;
        }

        // ✅ 自动查找包含 bin/java.exe 的子目录（兼容解压后多一层目录的情况）
        File javaExeFile = PathUtils.findJavaExecutable(extractedRoot);

        if (javaExeFile != null) {
            File jdkHome = javaExeFile.getParentFile().getParentFile(); // 定位到 JDK 根目录
            jdkPathField.setText(jdkHome.getAbsolutePath());
            outputArea.appendText("✅ 已自动定位到 JDK 根目录: " + jdkHome.getAbsolutePath() + "\n");
        } else {
            jdkPathField.setText(extractedRoot.getAbsolutePath());
            outputArea.appendText("⚠️ 未找到 java.exe，已使用默认路径: " + extractedRoot.getAbsolutePath() + "\n");
        }
    }
}
