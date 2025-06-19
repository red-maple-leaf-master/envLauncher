package top.oneyi.envLauncher.utils;

import javafx.scene.control.TextArea;

/**
 * @author W
 * @date 2025/6/19
 * @description 日志输出类
 */
public class LoggerUtil {

    public static TextArea outputArea;

    public static void info(String log) {
        if(outputArea != null){
            outputArea.appendText(log + "\n");
        } else {
            throw new RuntimeException("未设置输出区域");
        }

    }
}
