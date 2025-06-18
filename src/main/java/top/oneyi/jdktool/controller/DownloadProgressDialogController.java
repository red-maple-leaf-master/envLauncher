package top.oneyi.jdktool.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 下载弹窗控制器
 */
public class DownloadProgressDialogController {
    @FXML
    public ProgressBar progressBar;
    @FXML
    public Label statusLabel;
    @FXML
    public Label sizeLabel;

    private volatile boolean cancelRequested = false;

    public void onCancelDownload() {
        cancelRequested = true;
        statusLabel.setText("❌ 用户取消下载");
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public void closeStream(InputStream inputStream,
                            FileOutputStream fileOutputStream,
                            String destinationPath) throws IOException {
        // 如果有 InputStream 可以在这里关闭
        // ✅ 检查是否用户点击了取消按钮
        if (this.isCancelRequested()) {
            inputStream.close();      // 关闭输入流
            fileOutputStream.close();
            new File(destinationPath).delete(); // 删除未完成的文件
        }
    }
}
