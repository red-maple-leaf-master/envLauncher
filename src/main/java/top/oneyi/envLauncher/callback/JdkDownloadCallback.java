package top.oneyi.envLauncher.callback;

/**
 * Notifies the caller when the JDK archive has been downloaded and extracted.
 */
@FunctionalInterface
public interface JdkDownloadCallback {
    void onDownloadComplete(String extractedJdkDir);
}
