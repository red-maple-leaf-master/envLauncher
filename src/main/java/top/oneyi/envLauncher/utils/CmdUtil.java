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

    /**
     * 根据关键词从 PATH 中查找匹配的路径
     *
     * @param keywords 要匹配的关键词数组（如 ["jdk", "java"]）
     * @return 匹配的第一个路径，未找到返回 null
     */
    public static String getPathFromEnvironment(String... keywords) throws IOException {
        Process process = Runtime.getRuntime().exec("cmd /c echo %PATH%");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] paths = line.split(";");
            for (String path : paths) {
                for (String keyword : keywords) {
                    if (path.toLowerCase().contains(keyword)) {
                        System.out.println("✅ 匹配到路径: " + path);
                        return path;
                    }
                }
            }
        }

        reader.close();
        return null;
    }

    /**
     * 设置环境变量
     *
     * @param newPath  新环境变量值
     * @param homeType 环境变量类型
     * @throws Exception
     */
    public static void setHomeInWindows(String newPath, String homeType) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("reg", "add",
                "HKCU\\Environment", "/v", homeType, "/d", newPath, "/f");
        Process process = pb.start();
        process.waitFor();

    }

    /**
     * 设置 path 环境变量 (修改注册表,坏处是不会立即生效)
     * @param path
     * @throws IOException
     */
    public static void regAddPath(String path) throws IOException {
        // 使用 reg add 设置 PATH
        Process pathProcess = Runtime.getRuntime().exec(
                new String[]{
                        "cmd.exe", "/c", "reg", "add",
                        "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment",
                        "/v", "Path", "/t", "REG_EXPAND_SZ", "/d", "\"" + path + "\"", "/f"
                }
        );
        printProcessOutput(pathProcess);
    }

    /**
     * 辅助方法：读取进程输出
     *
     * @param process 进程
     * @throws IOException
     */
    private static void printProcessOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "GBK"));
        String line;

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        while ((line = errorReader.readLine()) != null) {
            System.err.println(line);
        }

        reader.close();
        errorReader.close();
    }

}
