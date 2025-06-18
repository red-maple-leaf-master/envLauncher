package top.oneyi.jdktool.utils;

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

    public static String getMavenDownloadPath(String version) {
        File downloadDir = new File(getCurrentDrive() + "environment");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        return new File(downloadDir, "apache-maven-" + version + ".zip").getAbsolutePath();
    }



}
