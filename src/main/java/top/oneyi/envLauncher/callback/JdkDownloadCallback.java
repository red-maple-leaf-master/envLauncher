package top.oneyi.envLauncher.callback;

/**
 * 下载回调接口
 */
@FunctionalInterface
public interface JdkDownloadCallback {
    void onDownloadComplete(String jdkPath);
}
