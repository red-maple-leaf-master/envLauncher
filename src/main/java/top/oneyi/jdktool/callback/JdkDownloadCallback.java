package top.oneyi.jdktool.callback;

/**
 * 回调接口
 */
@FunctionalInterface
public interface JdkDownloadCallback {
    void onDownloadComplete(String jdkPath);
}
