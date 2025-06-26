package top.oneyi.envLauncher.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author W
 * @date 2025/6/26
 * @description CMD命令工具类
 */
public class CmdUtil {

    /**
     * 执行 CMD 命令并返回输出结果
     *
     * @param command 要执行的命令（如 "setx NODE_HOME ... /M"）
     * @return 命令执行后的输出内容
     */
    public static String executeCmdCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec("cmd /c " + command);

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "GBK"));
        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line).append(System.lineSeparator());
        }

        while ((line = errorReader.readLine()) != null) {
            output.append("ERROR: ").append(line).append(System.lineSeparator());
        }

        reader.close();
        errorReader.close();

        return output.toString();
    }

}
