package top.oneyi.envLauncher.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author W
 * @date 2025/6/16
 * @description 环境变量配置工具类
 */
public class EnvUtil {


    /**
     * 获取当前设置的 jdk 环境变量
     */
    public static String getJdkEnvironmentVariables() throws IOException {

        // 获取 PATH 中的 Java 相关路径
        Process process2 = Runtime.getRuntime().exec("cmd /c echo %PATH%");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process2.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] split = line.split(";");
            for (String path : split) {
                // 英文字母小写
                if (path.toLowerCase().contains("jdk") || path.toLowerCase().contains("java")) {
                    System.out.println("✅  JDK 路径: " + path);
                    return path;
                }
            }

        }
        reader.close();


        return null;
    }

    /**
     * 获取当前设置的 jdk 环境变量
     */
    public static String getMavenEnvironmentVariables() throws IOException {

        // 获取 PATH 中的 Java 相关路径
        Process process2 = Runtime.getRuntime().exec("cmd /c echo %PATH%");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process2.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] split = line.split(";");
            for (String path : split) {
                // 英文字母小写
                if (path.toLowerCase().contains("maven")) {
                    System.out.println("✅  maven 路径: " + path);
                    return path;
                }
            }

        }
        reader.close();


        return null;
    }

    /**
     * 设置 JDK 环境变量
     *
     * @param javaHome   javaHome
     * @param jdkBinPath jdkBinPath
     */
    public static void setJdkEnvironmentVariables(String javaHome, String jdkBinPath) throws Exception {

        // 设置 JAVA_HOME
        Process javaHomeProcess = Runtime.getRuntime().exec(
                "cmd /c setx JAVA_HOME \"" + javaHome + "\" /M"
        );
        printProcessOutput(javaHomeProcess);
        // 刷新环境变量
        setJavaHomeInWindows(javaHome);

        // 获取当前系统的 PATH
        String currentPath = System.getenv("PATH");

        currentPath = filterAndInsertPath(jdkBinPath, currentPath, "java", "jdk");

        // 使用 reg add 设置 PATH
        Process pathProcess = Runtime.getRuntime().exec(
                new String[]{
                        "cmd.exe", "/c", "reg", "add",
                        "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment",
                        "/v", "Path", "/t", "REG_EXPAND_SZ", "/d", "\"" + currentPath + "\"", "/f"
                }
        );
        printProcessOutput(pathProcess);

        System.out.println("✅ JDK 环境变量已设置，请重启电脑生效。");


    }

    /**
     * 设置 Maven 环境变量
     *
     * @param mavenHome    MavenHome
     * @param mavenBinPath mavenBinPath
     */
    public static void setMavenEnvironmentVariables(String mavenHome, String mavenBinPath) throws Exception {

        // 设置 JAVA_HOME
        Process javaHomeProcess = Runtime.getRuntime().exec(
                "cmd /c setx MAVEN_HOME \"" + mavenHome + "\" /M"
        );
        printProcessOutput(javaHomeProcess);
        // 刷新环境变量
        setJavaHomeInWindows(mavenHome);

        // 获取当前系统的 PATH
        String currentPath = System.getenv("PATH");

        currentPath = filterAndInsertPath(mavenBinPath, currentPath, "maven");

        // 使用 reg add 设置 PATH
        Process pathProcess = Runtime.getRuntime().exec(
                new String[]{
                        "cmd.exe", "/c", "reg", "add",
                        "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment",
                        "/v", "Path", "/t", "REG_EXPAND_SZ", "/d", "\"" + currentPath + "\"", "/f"
                }
        );
        printProcessOutput(pathProcess);

        System.out.println("✅ Maven 环境变量已设置，请重启电脑生效。");


    }

    /**
     * 清理 PATH 中已有的 JDK 相关路径，并插入新路径到最前面
     *
     * @param jdkBinPath  新 JDK 的 bin 路径（如 %JAVA_HOME%\bin）
     * @param currentPath 当前的 PATH 环境变量值
     * @return 优化后的 PATH 字符串
     */
    private static String whetherPathExist(String jdkBinPath, String currentPath) {
        if (currentPath == null || currentPath.isEmpty()) {
            return jdkBinPath;
        }

        // 拆分为数组处理
        StringBuilder newPath = new StringBuilder();
        for (String path : currentPath.split(";")) {
            String lowerPath = path.toLowerCase(); // 只转换一次
            if (lowerPath.contains("jdk") || lowerPath.contains("java")) {
                continue; // 忽略已有 JDK/Java 路径
            }
            newPath.append(path).append(";");
        }

        // 添加新 JDK 路径到最前面
        return jdkBinPath + ";" + newPath;
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

    public static void setJavaHomeInWindows(String newJdkPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("reg", "add",
                "HKCU\\Environment", "/v", "JAVA_HOME", "/d", newJdkPath, "/f");
        Process process = pb.start();
        process.waitFor();

    }


    /**
     * 清理 PATH 中已有的指定类型相关路径，并插入新路径到最前面
     *
     * @param binPath     新路径（如 %JAVA_HOME%\bin）
     * @param currentPath 当前 PATH
     * @param keywords    要排除的关键词数组（如 ["java", "jdk"]）
     * @return 优化后的 PATH 字符串
     */
    private static String filterAndInsertPath(String binPath, String currentPath, String... keywords) {
        if (currentPath == null || currentPath.isEmpty()) {
            return binPath;
        }

        StringBuilder newPath = new StringBuilder();
        for (String path : currentPath.split(";")) {
            boolean matched = false;
            for (String keyword : keywords) {
                if (path.toLowerCase().contains(keyword)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                newPath.append(path).append(";");
            }
        }

        return binPath + ";" + newPath.toString();
    }

}
