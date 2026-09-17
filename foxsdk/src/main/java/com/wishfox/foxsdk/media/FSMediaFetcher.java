package com.wishfox.foxsdk.media;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 仅用于图片的有界临时下载。视频在线播放，不得接入此下载器。 */
public final class FSMediaFetcher {
    public interface Callback { void complete(File file, String error); }
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean cancelled;
    private volatile HttpURLConnection connection;
    private volatile File temporary;

    public void fetchImage(File cacheDir, String url, List<String> origins, Callback callback) {
        if (cancelled) return;
        worker.execute(() -> {
            File file = null;
            String error = null;
            try {
                File directory = new File(cacheDir, "wishfox-preview");
                if (!directory.isDirectory() && !directory.mkdirs()) throw new Exception("CACHE_FAILED");
                file = File.createTempFile("preview-", ".media", directory);
                temporary = file;
                long limit = FSMediaPolicy.IMAGE_BYTES;
                String next = url;
                long deadline = android.os.SystemClock.elapsedRealtime() + 120000;
                for (int redirect = 0; ; redirect++) {
                    if (cancelled) throw new Exception("CANCELLED");
                    if (!FSMediaPolicy.allowed(next, origins)) throw new Exception("MEDIA_URL_NOT_ALLOWED");
                    HttpURLConnection current = (HttpURLConnection) new URL(next).openConnection();
                    connection = current;
                    current.setInstanceFollowRedirects(false);
                    current.setConnectTimeout(15000);
                    current.setReadTimeout(15000);
                    current.setRequestProperty("Accept-Encoding", "identity");
                    try {
                        if (cancelled) throw new Exception("CANCELLED");
                        int status = current.getResponseCode();
                        if (status >= 300 && status <= 399) {
                            if (redirect >= 3 || current.getHeaderField("Location") == null)
                                throw new Exception("MEDIA_REDIRECT_FAILED");
                            next = new URL(new URL(next), current.getHeaderField("Location")).toString();
                            continue;
                        }
                        if (status != 200) throw new Exception("MEDIA_DOWNLOAD_FAILED");
                        String length = current.getHeaderField("Content-Length");
                        if (length != null && Long.parseLong(length) > limit) throw new Exception("MEDIA_TOO_LARGE");
                        try (InputStream input = current.getInputStream(); FileOutputStream output = new FileOutputStream(file)) {
                            byte[] buffer = new byte[32768];
                            long count = 0;
                            int size;
                            while ((size = input.read(buffer)) != -1) {
                                if (cancelled) throw new Exception("CANCELLED");
                                if (android.os.SystemClock.elapsedRealtime() > deadline) throw new Exception("MEDIA_TIMEOUT");
                                count += size;
                                if (count > limit) throw new Exception("MEDIA_TOO_LARGE");
                                output.write(buffer, 0, size);
                            }
                            if (count == 0) throw new Exception("MEDIA_DOWNLOAD_FAILED");
                        }
                        break;
                    } finally { current.disconnect(); connection = null; }
                }
            } catch (Exception failure) {
                error = failure.getMessage();
                if (error == null || !error.startsWith("MEDIA_")) error = "MEDIA_DOWNLOAD_FAILED";
            }
            if (cancelled || error != null) { if (file != null) file.delete(); }
            final File result = file;
            final String code = error;
            main.post(() -> { if (!cancelled) callback.complete(result, code); });
            worker.shutdown();
        });
    }

    public void cancel() {
        cancelled = true;
        HttpURLConnection active = connection;
        if (active != null) active.disconnect();
        worker.shutdownNow();
        File file = temporary;
        if (file != null) file.delete();
    }
}
