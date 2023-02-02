package top.ss007.library;


import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;


public class DownloadUtil {
    private static final String TAG = "DownloadUtil";
    private static final int DEFAULT_TIMEOUT = 120;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MainThreadExecutor uiExecutor = new MainThreadExecutor();
    private OkHttpClient.Builder builder;
    private DownloadListener listener;
    private InputParameter inputParameter;
    private static DownloadUtil INSTANCE;

    private DownloadUtil() {
    }

    public static DownloadUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DownloadUtil();
        }
        return INSTANCE;
    }

    public void initConfig(OkHttpClient.Builder builder) {
        this.builder = builder;
    }

    /**
     * download file and show the progress
     *
     * @param listener
     */
    public void downloadFile(InputParameter inputParam, final DownloadListener listener) {
        this.listener = listener;
        this.inputParameter = inputParam;


        DownloadInterceptor interceptor = new DownloadInterceptor(listener);
        if (builder != null) {
            builder.addInterceptor(interceptor);
        } else {
            builder = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .retryOnConnectionFailure(true)
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        }
        final DownloadService api = new Retrofit.Builder()
                .baseUrl(inputParam.getBaseUrl())
                .client(builder.build())
                .build()
                .create(DownloadService.class);
        executorService.execute(() -> {
            try {
                Response<ResponseBody> result = api.downloadWithDynamicUrl(inputParam.getRelativeUrl()).execute();
                File file = FileUtil.writeFile(inputParam.getLoadedFilePath(), result.body().byteStream());
                if (listener != null) {
                    if (inputParam.isCallbackOnUiThread()) {
                        uiExecutor.execute(() -> listener.onFinish(file));
                    } else {
                        listener.onFinish(file);
                    }
                }
            } catch (Exception e) {
                if (listener != null) {
                    if (inputParam.isCallbackOnUiThread()) {
                        uiExecutor.execute(() -> listener.onFailed(e.getMessage()));
                    } else {
                        listener.onFailed(e.getMessage());
                    }
                }
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    public void shutdownExecutor() {
        executorService.shutdownNow();
        INSTANCE = null;
        FileUtil.deleteFile(inputParameter.getLoadedFilePath());

        if (listener != null) {
            uiExecutor.execute(() -> listener.onFailed("File download stopped"));
            Log.d(TAG, "shutdownExecutor: FILE DOWNLOAD STOPPED");
        }
    }
}
