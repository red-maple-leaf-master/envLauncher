package top.oneyi.envLauncher.utils;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author W
 * @date 2025/6/19
 * @description 日志输出类
 */
public class LoggerUtil {

    private static TextArea logArea;

    public static void init(TextArea area) {
        logArea = area;
    }

    public static void info(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String finalMessage = "[" + LocalDateTime.now().format(formatter) + "] :  " + message + "\n";

        // 确保在 JavaFX 线程中更新 UI
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(finalMessage);
            } else {
                System.out.println("Logger未初始化，日志输出到控制台：" + finalMessage);
            }
        });
    }
}
