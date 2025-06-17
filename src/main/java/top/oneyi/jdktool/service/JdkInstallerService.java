package top.oneyi.jdktool.service;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

/**
 * @author W
 * @date 2025/6/17
 * @description JDK安装服务业务层
 */
public class JdkInstallerService {


    public void onDownloadJdk(TextArea outputArea) {
        outputArea.appendText("⚠ 暂不支持 JDK 下载功能\n");
    }

    public void onSetupMaven(TextArea outputArea) {
        outputArea.appendText("⚠ 暂不支持 Maven 设置功能\n");
    }
}
