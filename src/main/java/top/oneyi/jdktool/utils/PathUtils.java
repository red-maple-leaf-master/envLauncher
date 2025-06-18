package top.oneyi.jdktool.utils;

import java.io.File;

public class PathUtils {

    public static String getCurrentDrive() {
        String userDir = System.getProperty("user.dir");
        File dir = new File(userDir);
        return dir.getAbsolutePath().split(":")[0] + ":\\";
    }

    public static String getDownloadPath(String version) {
        File downloadDir = new File(getCurrentDrive() + "environment");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        return new File(downloadDir, "jdk-" + version + ".zip").getAbsolutePath();
    }
}
