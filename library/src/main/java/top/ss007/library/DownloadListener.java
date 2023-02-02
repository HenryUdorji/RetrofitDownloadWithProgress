package top.ss007.library;

import java.io.File;

public interface DownloadListener {
    void onFinish(File file);

    void onProgress(int progress,long downloadedLength,long totalLength);

    void onFailed(String errorMessage);
}