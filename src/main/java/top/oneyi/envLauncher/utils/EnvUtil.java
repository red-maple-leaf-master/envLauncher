package top.oneyi.envLauncher.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author W
 * @date 2025/6/16
 * @description 环境变量配置工具类
 */
public class EnvUtil {

    private static String globalPath = null;


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
        setJavaHomeInWindows(javaHome, "JAVA_HOME");

        setPah(jdkBinPath, "jdk", "java");

        System.out.println("✅ JDK 环境变量已设置，请重启电脑生效。");


    }

    /**
     * 设置 Maven 环境变量
     *
     * @param mavenHome    MavenHome
     * @param mavenBinPath mavenBinPath
     */
    public static void setMavenEnvironmentVariables(String mavenHome, String mavenBinPath) throws Exception {

        // 设置 MAVEN_HOME
        Process javaHomeProcess = Runtime.getRuntime().exec(
                "cmd /c setx MAVEN_HOME \"" + mavenHome + "\" /M"
        );
        printProcessOutput(javaHomeProcess);
        // 刷新环境变量
        setJavaHomeInWindows(mavenHome, "MAVEN_HOME");

        setPah(mavenBinPath, "maven");

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

    public static void setJavaHomeInWindows(String newPath, String homeType) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("reg", "add",
                "HKCU\\Environment", "/v", homeType, "/d", newPath, "/f");
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
        boolean added = false;

        for (String path : currentPath.split(";")) {
            boolean matched = false;
            for (String keyword : keywords) {
                if (path.toLowerCase().contains(keyword)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (!added) {
                    newPath.append(binPath).append(";");
                    added = true;
                }
                newPath.append(path).append(";");
            }
        }

        if (!added) {
            newPath.append(binPath);
        }

        return newPath.toString();
    }

    /**
     * @param nodeHome
     */
    public static void setNodeEnvironmentVariables(String nodePath, String nodeHome) throws Exception {
        // 创建全局安装位置和缓存路径
        String globalInstallPath = nodePath + "\\node_global";
        String cachePath = nodePath + "\\node_cache";
        if (!new File(globalInstallPath).exists()) {
            new File(globalInstallPath).mkdirs();
        }
        if (!new File(cachePath).exists()) {
            new File(cachePath).mkdirs();
        }

        // 设置 NODE_HOME
        String command = "setx NODE_HOME \"" + nodePath + "\" /M";
        String result = CmdUtil.executeCmdCommand(command);

        System.out.println("CMD 输出结果：\n" + result);
        setJavaHomeInWindows(nodePath, "NODE_HOME");

        // 分别设置 PATH 中的各个路径
        setPah(nodeHome);                   // 主目录
        setPah(globalInstallPath);   // 全局模块路径
        setPah(cachePath);            // 缓存路径
        setNpmConfig(cachePath, globalInstallPath);


        System.out.println("✅ Node 环境变量已设置，请重启电脑生效。");
    }

    public static void setNpmConfig(String cachePath, String globalInstallPath) throws IOException {
        try {
            // 设置缓存路径
            String cacheCommand = "npm config set cache \"" + cachePath + "\" --location=global";
            String cacheResult = CmdUtil.executeCmdCommand(cacheCommand);
            System.out.println("✅ NPM 缓存路径设置完成:\n" + cacheResult);

            // 设置全局安装路径
            String prefixCommand = "npm config set prefix \"" + globalInstallPath + "\" --location=global";
            String prefixResult = CmdUtil.executeCmdCommand(prefixCommand);
            System.out.println("✅ NPM 全局安装路径设置完成:\n" + prefixResult);

            // 设置淘宝镜像源
            String registryCommand = "npm config set registry https://registry.npm.taobao.org/";
            String registryResult = CmdUtil.executeCmdCommand(registryCommand);
            System.out.println("✅ NPM 镜像源设置为淘宝镜像:\n" + registryResult);

            // 但是淘宝镜像好像过时了
            String strictCommand = "npm config set strict-ssl false";
            CmdUtil.executeCmdCommand(strictCommand);
            System.out.println("✅ NPM 严格SSL验证已关闭");
            String cnpmCommand = "npm install cnpm@7.1.1 -g";
            CmdUtil.executeCmdCommand(cnpmCommand);
            System.out.println("✅ NPM 安装 cnpm 成功");

        } catch (Exception e) {
            System.err.println("❌ 设置 NPM 配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置 PATH 环境变量
     *
     * @param pathHome        PATH 路径
     * @param excludeKeywords 要排除的关键词
     * @throws IOException
     */
    private static void setPah(String pathHome, String... excludeKeywords) throws IOException {
        // 获取当前系统的 PATH
        if (globalPath == null) {
            globalPath = System.getenv("PATH");
        }


        globalPath = filterAndInsertPath(pathHome, globalPath, excludeKeywords);

        // 使用 reg add 设置 PATH
        Process pathProcess = Runtime.getRuntime().exec(
                new String[]{
                        "cmd.exe", "/c", "reg", "add",
                        "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment",
                        "/v", "Path", "/t", "REG_EXPAND_SZ", "/d", "\"" + globalPath + "\"", "/f"
                }
        );
        printProcessOutput(pathProcess);
    }
}
