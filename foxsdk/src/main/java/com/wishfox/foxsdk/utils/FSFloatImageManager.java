package com.wishfox.foxsdk.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.wishfox.foxsdk.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 下载并持久化保存接口返回的悬浮球图片。
 *
 * <p>图片地址与下载后的文件分别保存，地址未变化时直接加载本地文件，
 * 无需再次通过网络下载图片。</p>
 */
public final class FSFloatImageManager {

    private static final String PREFERENCES_NAME = "WishFoxSdkFloatImage";
    private static final String URL_KEY = "float_image_url";
    private static final String VERSION_KEY = "float_image_version";
    private static final String IMAGE_FILE_NAME = "float_image.cache";
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final int DOWNLOAD_TIMEOUT_MS = 15_000;

    /**
     * 工具类不允许实例化。
     */
    private FSFloatImageManager() {
    }

    /**
     * 更新应用内部沙盒中的图片缓存。
     * 返回新下载的图片文件、之前的可用缓存文件，或 {@code null}。
     */
    @Nullable
    public static synchronized File refreshCache(@NonNull Context context, @Nullable String remoteUrl) {
        return refreshCache(context, remoteUrl, null);
    }

    /** 服务端版本变化时，即使图片 URL 不变也会重新下载。 */
    @Nullable
    public static synchronized File refreshCache(@NonNull Context context,
                                                  @Nullable String remoteUrl,
                                                  @Nullable Long remoteVersion) {
        Context appContext = context.getApplicationContext();
        String url = remoteUrl == null ? "" : remoteUrl.trim();
        File cacheFile = getCacheFile(appContext);
        android.content.SharedPreferences preferences = appContext.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        String cachedUrl = preferences.getString(URL_KEY, "");
        String cachedVersion = preferences.getString(VERSION_KEY, "");
        boolean versionMatches = remoteVersion == null
                || String.valueOf(remoteVersion).equals(cachedVersion);

        // 图片地址未变化且缓存文件可用时，直接复用本地文件。
        // 缓存文件不存在或为空时，继续尝试重新下载。
        if (!TextUtils.isEmpty(url) && url.equals(cachedUrl) && versionMatches
                && isUsableFile(cacheFile)) {
            return cacheFile;
        }

        if (!TextUtils.isEmpty(url)) {
            try {
                downloadAtomically(url, cacheFile);
                if (isUsableFile(cacheFile)) {
                    android.content.SharedPreferences.Editor editor = preferences.edit()
                            .putString(URL_KEY, url);
                    if (remoteVersion == null) {
                        editor.remove(VERSION_KEY);
                    } else {
                        editor.putString(VERSION_KEY, String.valueOf(remoteVersion));
                    }
                    editor.apply();
                    return cacheFile;
                }
            } catch (Throwable failure) {
                // 网络异常或图片下载失败时，保留之前的缓存；不记录 URL，避免日志泄露资源地址。
                FoxSdkLogger.w("FoxSdk[FloatImage]",
                        "悬浮球图片下载失败: " + failure.getClass().getSimpleName()
                                + safeFailureMessage(failure));
            }
        }

        return isUsableFile(cacheFile) ? cacheFile : null;
    }

    /** 加载本地缓存，加载失败时回退到预置图片。 */
    public static void loadInto(@NonNull ImageView imageView) {
        Context context = imageView.getContext().getApplicationContext();
        File cacheFile = getCacheFile(context);
        if (!isUsableFile(cacheFile)) {
            imageView.setImageResource(R.mipmap.fs_icon_float_round_fill);
            return;
        }

        Glide.with(imageView)
                .load(cacheFile)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @Nullable com.bumptech.glide.load.engine.GlideException e,
                            Object model,
                            Target<Drawable> target,
                            boolean isFirstResource) {
                        imageView.setImageResource(R.mipmap.fs_icon_float_round_fill);
                        invalidateCache(context);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            Drawable resource,
                            Object model,
                            Target<Drawable> target,
                            com.bumptech.glide.load.DataSource dataSource,
                            boolean isFirstResource) {
                        return false;
                    }
                })
                .into(imageView);
    }

    /**
     * 获取悬浮球图片缓存文件路径，并确保父目录存在。
     */
    private static File getCacheFile(Context context) {
        File directory = new File(context.getFilesDir(), "wishfox_sdk");
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
        return new File(directory, IMAGE_FILE_NAME);
    }

    /**
     * 清理本地缓存文件和已保存的远程图片地址。
     */
    private static void invalidateCache(Context context) {
        File cacheFile = getCacheFile(context);
        if (cacheFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheFile.delete();
        }
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(URL_KEY)
                .remove(VERSION_KEY)
                .apply();
    }

    /**
     * 判断缓存文件是否存在且内容有效。
     */
    private static boolean isUsableFile(@Nullable File file) {
        return file != null && file.isFile() && file.length() > 0;
    }

    /** 独立下载远程图片；Glide 仅负责显示本地文件，避免网络 Future 异常被包装。 */
    private static void downloadAtomically(String remoteUrl, File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IOException("Unable to create float image cache directory");
        }
        URL next = new URL(remoteUrl);
        for (int redirect = 0; redirect <= 3; redirect++) {
            if (!"https".equalsIgnoreCase(next.getProtocol())) {
                throw new IOException("HTTPS_REQUIRED");
            }
            HttpURLConnection connection = (HttpURLConnection) next.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT_MS);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "image/*");
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("Cache-Control", "no-cache");
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status <= 399) {
                    String location = connection.getHeaderField("Location");
                    if (redirect == 3 || TextUtils.isEmpty(location)) {
                        throw new IOException("REDIRECT_FAILED");
                    }
                    next = new URL(next, location);
                    continue;
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP_" + status);
                }
                long declaredLength = connection.getContentLength();
                if (declaredLength > MAX_IMAGE_BYTES) throw new IOException("IMAGE_TOO_LARGE");
                File temporary = new File(destination.getPath() + ".tmp");
                boolean replaced = false;
                try {
                    try (InputStream input = connection.getInputStream();
                         FileOutputStream output = new FileOutputStream(temporary)) {
                        byte[] buffer = new byte[16 * 1024];
                        long total = 0;
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            total += count;
                            if (total > MAX_IMAGE_BYTES) throw new IOException("IMAGE_TOO_LARGE");
                            output.write(buffer, 0, count);
                        }
                        if (total == 0) throw new IOException("EMPTY_IMAGE");
                        output.getFD().sync();
                    }
                    replaceTemporary(temporary, destination);
                    replaced = true;
                } finally {
                    if (!replaced && temporary.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        temporary.delete();
                    }
                }
                return;
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("REDIRECT_FAILED");
    }

    private static void replaceTemporary(File temporary, File destination) throws IOException {
        if (!temporary.renameTo(destination)) {
            if (destination.exists() && !destination.delete()) {
                throw new IOException("Unable to replace float image cache");
            }
            if (!temporary.renameTo(destination)) {
                throw new IOException("Unable to move float image cache into place");
            }
        }
    }

    private static String safeFailureMessage(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        if (TextUtils.isEmpty(message)) return "";
        String safe = message.replaceAll("https?://\\S+", "<url>");
        return ": " + safe.substring(0, Math.min(160, safe.length()));
    }
}
