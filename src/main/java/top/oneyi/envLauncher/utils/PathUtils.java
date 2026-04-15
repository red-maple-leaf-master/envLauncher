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
 * Path helpers for download locations, extraction folders, and PATH entry updates.
 */
public class PathUtils {

    /**
     * Resolve the current workspace drive so default installs stay on the same disk.
     */
    public static String getCurrentDrive() {
        String userDir = System.getProperty("user.dir");
        File dir = new File(userDir);
        return dir.getAbsolutePath().split(":")[0] + ":\\";
    }

    /**
     * Build the default JDK archive path under the workspace drive.
     */
    public static String getJdkDownloadPath(String version) {
        File downloadDir = new File(getCurrentDrive() + "environment");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        return new File(downloadDir, "jdk-" + version + ".zip").getAbsolutePath();
    }

    public static String getJdkDownloadPath(String baseDir, String version) {
        File downloadDir = getDownloadsDir(baseDir);
        return new File(downloadDir, "jdk-" + version + ".zip").getAbsolutePath();
    }

    public static String getJdkExtractDir(String baseDir, String version) {
        return new File(baseDir, "jdk-" + version).getAbsolutePath();
    }

    /**
     * Some JDK archives add an extra version folder, so scan one level down for the runtime executable.
     */
    public static File findJavaExecutable(File rootDir) {
        File[] files = rootDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File javaExe = new File(file, "bin" + File.separator + "java.exe");
                    if (!javaExe.exists()) {
                        javaExe = new File(file, "bin" + File.separator + "java");
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
     * Read the remote file size before download so the UI can render progress.
     */
    public static long getFileSize(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        long contentLength = connection.getContentLengthLong();
        connection.disconnect();
        return contentLength;
    }

    /**
     * Unzip the downloaded archive into the target directory.
     */
    public static void unzipFile(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            byte[] buffer = new byte[1024];

            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                File newFile = new File(filePath);

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();

                    try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
                entry = zipIn.getNextEntry();
            }
        }
    }

    /**
     * Build the default Maven archive path under the workspace drive.
     */
    public static String getMavenDownloadPath(String version) {
        File downloadDir = new File(getCurrentDrive() + "environment");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        return new File(downloadDir, "apache-maven-" + version + ".zip").getAbsolutePath();
    }

    public static String getMavenDownloadPath(String baseDir, String version) {
        File downloadDir = getDownloadsDir(baseDir);
        return new File(downloadDir, "apache-maven-" + version + ".zip").getAbsolutePath();
    }

    public static String getMavenExtractDir(String baseDir, String version) {
        return new File(baseDir, "apache-maven-" + version).getAbsolutePath();
    }

    /**
     * Build the default Node archive path under the workspace drive.
     */
    public static String getNodeDownloadPath(String version) {
        File downloadDir = new File(getCurrentDrive() + "environment");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        return new File(downloadDir, "node-" + version + ".zip").getAbsolutePath();
    }

    public static String getNodeDownloadPath(String baseDir, String version) {
        File downloadDir = getDownloadsDir(baseDir);
        return new File(downloadDir, "node-" + version + ".zip").getAbsolutePath();
    }

    public static String getNodeExtractDir(String baseDir, String version) {
        return new File(baseDir, "node-" + version).getAbsolutePath();
    }

    /**
     * Remove stale tool-specific entries before inserting the new path at the front of PATH.
     */
    public static String filterAndInsertPath(String pathEntry, String currentPath, String... keywords) {
        if (currentPath == null || currentPath.isEmpty()) {
            return pathEntry;
        }

        StringBuilder mergedPath = new StringBuilder();
        boolean pathInserted = false;

        for (String currentEntry : currentPath.split(";")) {
            boolean shouldExclude = false;
            for (String keyword : keywords) {
                if (currentEntry.toLowerCase().contains(keyword.toLowerCase())) {
                    shouldExclude = true;
                    break;
                }
            }
            if (!shouldExclude) {
                if (!pathInserted) {
                    mergedPath.append(pathEntry).append(";");
                    pathInserted = true;
                }
                mergedPath.append(currentEntry).append(";");
            }
        }

        if (!pathInserted) {
            mergedPath.append(pathEntry);
        }

        return mergedPath.toString();
    }

    private static File getDownloadsDir(String baseDir) {
        File downloadDir = new File(baseDir, "downloads");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        return downloadDir;
    }
}
