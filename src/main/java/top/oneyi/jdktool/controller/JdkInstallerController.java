package top.oneyi.jdktool.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import top.oneyi.jdktool.service.JdkInstallerService;
import top.oneyi.jdktool.utils.JDKUtil;

import java.io.File;

/**
 * java 相关环境设置
 */
public class JdkInstallerController {

    @FXML
    private TextField jdkPathField;

    @FXML
    private TextArea outputArea;


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

    public void onShowCurrentConfig() {
        try {
            String jdkEnv = JDKUtil.getJdkEnvironmentVariables(); // 可以重定向输出到 TextArea
            outputArea.appendText("🔍 当前 JDK 配置：" + jdkEnv + "\n");
        } catch (Exception e) {
            outputArea.appendText("⚠️ 获取 JDK 配置失败\n");
            e.printStackTrace();
        }

    }

    public void onDownloadJdk() {
        JdkInstallerService service = new JdkInstallerService();
        service.onDownloadJdk(outputArea);
    }

    public void onSetupMaven() {
        JdkInstallerService service = new JdkInstallerService();
        service.onSetupMaven(outputArea);
    }
}
