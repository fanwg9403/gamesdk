package com.wishfox.foxsdk.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.wishfox.foxsdk.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 下载并持久化保存接口返回的悬浮球图片。
 *
 * <p>图片地址与下载后的文件分别保存，地址未变化时直接加载本地文件，
 * 无需再次通过网络下载图片。</p>
 */
public final class FSFloatImageManager {

    private static final String PREFERENCES_NAME = "WishFoxSdkFloatImage";
    private static final String URL_KEY = "float_image_url";
    private static final String IMAGE_FILE_NAME = "float_image.cache";

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
        Context appContext = context.getApplicationContext();
        String url = remoteUrl == null ? "" : remoteUrl.trim();
        File cacheFile = getCacheFile(appContext);
        String cachedUrl = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(URL_KEY, "");

        // 图片地址未变化且缓存文件可用时，直接复用本地文件。
        // 缓存文件不存在或为空时，继续尝试重新下载。
        if (!TextUtils.isEmpty(url) && url.equals(cachedUrl) && isUsableFile(cacheFile)) {
            return cacheFile;
        }

        if (!TextUtils.isEmpty(url)) {
            try {
                File downloaded = Glide.with(appContext)
                        .asFile()
                        .load(url)
                        .submit()
                        .get();
                if (isUsableFile(downloaded)) {
                    copyAtomically(downloaded, cacheFile);
                    appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(URL_KEY, url)
                            .apply();
                    return cacheFile;
                }
            } catch (Throwable ignored) {
                // 网络异常或图片下载失败时，保留之前的缓存。
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
                .apply();
    }

    /**
     * 判断缓存文件是否存在且内容有效。
     */
    private static boolean isUsableFile(@Nullable File file) {
        return file != null && file.isFile() && file.length() > 0;
    }

    /**
     * 使用临时文件完成图片缓存替换，避免写入中断留下半文件。
     */
    private static void copyAtomically(File source, File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IOException("Unable to create float image cache directory");
        }
        File temporary = new File(destination.getPath() + ".tmp");
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.getFD().sync();
        }
        if (!temporary.renameTo(destination)) {
            if (destination.exists() && !destination.delete()) {
                throw new IOException("Unable to replace float image cache");
            }
            if (!temporary.renameTo(destination)) {
                throw new IOException("Unable to move float image cache into place");
            }
        }
    }
}
