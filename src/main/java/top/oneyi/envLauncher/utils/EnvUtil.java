package top.oneyi.envLauncher.utils;

import java.io.File;
import java.io.IOException;

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
        return CmdUtil.getPathFromEnvironment("jdk", "java");
    }

    /**
     * 获取当前设置的 Maven 环境变量
     */
    public static String getMavenEnvironmentVariables() throws IOException {
        return CmdUtil.getPathFromEnvironment("maven");
    }

    /**
     * 设置 JDK 环境变量
     *
     * @param javaHome   javaHome
     * @param jdkBinPath jdkBinPath
     */
    public static void setJdkEnvironmentVariables(String javaHome, String jdkBinPath) throws Exception {
        setEnvironmentVariable("JAVA_HOME", javaHome, jdkBinPath, "jdk", "java");
    }

    /**
     * 设置 Maven 环境变量
     *
     * @param mavenHome    MavenHome
     * @param mavenBinPath mavenBinPath
     */
    public static void setMavenEnvironmentVariables(String mavenHome, String mavenBinPath) throws Exception {
        setEnvironmentVariable("MAVEN_HOME", mavenHome, mavenBinPath, "maven");
    }

    /**
     * 通用设置环境变量的方法
     *
     * @param homeVarName 环境变量名（如 JAVA_HOME）
     * @param homePath    环境变量值（如 JDK 安装路径）
     * @param pathEntry   要添加到 PATH 的路径
     * @param excludeKeywords 排除的关键词（用于清理旧路径）
     */
    public static void setEnvironmentVariable(String homeVarName, String homePath, String pathEntry, String... excludeKeywords) throws Exception {
        // 设置主环境变量（如 JAVA_HOME）
        String command = "cmd /c setx " + homeVarName + " \"" + homePath + "\" /M";
        String result = CmdUtil.executeCmdCommand(command);
        System.out.println("✅ 设置 " + homeVarName + " 成功: " + result);

        // 刷新注册表中的环境变量
        CmdUtil.setHomeInWindows(homePath, homeVarName);

        // 更新 PATH 环境变量
        setPah(pathEntry, excludeKeywords);

        LoggerUtil.info("✅ [" + homeVarName + "] 相关环境变量已设置，请重启终端或 IDE 生效。");
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
        setEnvironmentVariable("NODE_HOME", nodePath, nodeHome);

        // 分别设置 PATH 中的各个路径
        setPah(globalInstallPath);   // 全局模块路径
        setPah(cachePath);            // 缓存路径
        setNpmConfig(cachePath, globalInstallPath);

        LoggerUtil.info("✅ Node 环境变量已设置，请重启软件生效。");
    }


    /**
     * 设置 NPM 配置
     * @param cachePath 缓存路径
     * @param globalInstallPath 全局模块路径
     * @throws IOException
     */
    public static void setNpmConfig(String cachePath, String globalInstallPath) throws IOException {
        try {
            // 设置缓存路径
            String cacheCommand = "npm config set cache \"" + cachePath + "\" --location=global";
            String cacheResult = CmdUtil.executeCmdCommand(cacheCommand);
            LoggerUtil.info("✅ NPM 缓存路径已设置\n" + cacheResult);

            // 设置全局安装路径
            String prefixCommand = "npm config set prefix \"" + globalInstallPath + "\" --location=global";
            String prefixResult = CmdUtil.executeCmdCommand(prefixCommand);
            LoggerUtil.info("✅ NPM 全局安装路径已设置\n" + prefixResult);

            // 设置淘宝镜像源
            String registryCommand = "npm config set registry https://registry.npm.taobao.org/";
            String registryResult = CmdUtil.executeCmdCommand(registryCommand);
            LoggerUtil.info("✅ NPM 镜像源设置为淘宝镜像:\n" + registryResult);

            // 但是淘宝镜像好像过时了
            String strictCommand = "npm config set strict-ssl false";
            CmdUtil.executeCmdCommand(strictCommand);
            LoggerUtil.info("✅ NPM 严格SSL验证已关闭,正在安装cnpm....");
            String cnpmCommand = "npm install cnpm@7.1.1 -g";
            CmdUtil.executeCmdCommand(cnpmCommand);
            LoggerUtil.info("✅ NPM 安装 cnpm 成功");

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


        globalPath = PathUtils.filterAndInsertPath(pathHome, globalPath, excludeKeywords);

        CmdUtil.regAddPath(globalPath);
    }
}
