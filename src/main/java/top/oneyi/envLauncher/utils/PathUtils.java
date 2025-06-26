package top.oneyi.envLauncher.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 路径工具类
 */
public class PathUtils {

    /**
     * 获取当前驱动器的根目录
     * @return 当前驱动器的根目录
     */
    public static String getCurrentDrive() {
        String userDir = System.getProperty("user.dir");
        File dir = new File(userDir);
        return dir.getAbsolutePath().split(":")[0] + ":\\";
    }

    /**
     * 获取下载路径
     * @param version 版本号
     * @return 下载的路径
     */
    public static String getDownloadPath(String version) {
        File downloadDir = new File(getCurrentDrive() + "environment");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        return new File(downloadDir, "jdk-" + version + ".zip").getAbsolutePath();
    }

    /**
     * 查找指定目录下的 bin/java.exe（Windows）或 bin/java（Linux/macOS）
     */
    public static File findJavaExecutable(File rootDir) {
        File[] files = rootDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File javaExe = new File(file, "bin" + File.separator + "java.exe"); // Windows
                    if (!javaExe.exists()) {
                        javaExe = new File(file, "bin" + File.separator + "java"); // Linux/macOS
                    }
                    if (javaExe.exists() && javaExe.isFile()) {
                        return javaExe;
                    }
                }
            }
        }

        return null;
    }

    /**
     *  获取文件大小
     * @param url   文件URL
     * @return
     * @throws IOException
     */
    public static long getFileSize(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        long contentLength = connection.getContentLengthLong();
        connection.disconnect();
        return contentLength;
    }

    /**
     * 解压zip文件
     * @param zipFilePath zip文件路径
     * @param destDirectory 解压目标目录
     * @throws IOException
     */
    public static void unzipFile(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            // 缓存大小
            byte[] buffer = new byte[1024];
            int len;

            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                File newFile = new File(filePath);

                // 创建父目录
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // 确保父目录存在
                    new File(newFile.getParent()).mkdirs();

                    // 写入文件
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int read;
                    while ((read = zipIn.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                    }
                    fos.close();
                }
                entry = zipIn.getNextEntry();
            }
        }
    }

    /**
     * 获取Maven下载路径
     * @param version 版本号
     * @return Maven下载路径
     */
    public static String getMavenDownloadPath(String version) {
        File downloadDir = new File(getCurrentDrive() + "environment");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        return new File(downloadDir, "apache-maven-" + version + ".zip").getAbsolutePath();
    }

    /**
     * 获取Node下载路径
     * @param version 节点版本号
     * @return 节点下载路径
     */
    public static String getNodeDownloadPath(String version) {
        File downloadDir = new File(getCurrentDrive() + "environment");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        return new File(downloadDir, "node-" + version + ".zip").getAbsolutePath();
    }

    /**
     * 清理 PATH 中已有的指定类型相关路径，并插入新路径到最前面
     *
     * @param binPath     新路径（如 %JAVA_HOME%\bin）
     * @param currentPath 当前 PATH
     * @param keywords    要排除的关键词数组（如 ["java", "jdk"]）
     * @return 优化后的 PATH 字符串
     */
    public static String filterAndInsertPath(String binPath, String currentPath, String... keywords) {
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



}
