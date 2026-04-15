package top.oneyi.envLauncher.utils;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sends timestamped log messages to the shared UI console.
 */
public class LoggerUtil {

    private static TextArea logArea;

    public static void init(TextArea area) {
        logArea = area;
    }

    public static void info(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedMessage = "[" + LocalDateTime.now().format(formatter) + "] :  " + message + "\n";

        // Always switch back to the JavaFX thread because most logs originate from background tasks.
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(formattedMessage);
            } else {
                System.out.println("Logger not initialized, writing to console instead: " + formattedMessage);
            }
        });
    }
}
