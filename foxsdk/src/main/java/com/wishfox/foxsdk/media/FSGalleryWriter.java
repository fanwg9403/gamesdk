package com.wishfox.foxsdk.media;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.LinkedHashMap;

/** 保存/分享共用的相册写入器，只在 IO 线程调用。 */
final class FSGalleryWriter {
    interface Cancellation { boolean isCancelled(); }
    private static final LinkedHashMap<String, SavedImage> RECENT = new LinkedHashMap<>();
    private static final long REUSE_MS = 10 * 60 * 1000L;

    private static final class SavedImage {
        final Uri uri;
        final long savedAt = SystemClock.elapsedRealtime();
        SavedImage(Uri uri) { this.uri = uri; }
    }

    private FSGalleryWriter() { }

    static synchronized Uri save(Context context, File source, String name, String mime,
                                 Cancellation cancellation) throws FSMediaSaveCoordinator.SaveException {
        check(cancellation);
        if (Build.VERSION.SDK_INT < 29) {
            throw new FSMediaSaveCoordinator.SaveException("UNSUPPORTED_NO_PERMISSION", "当前系统无法自动保存到相册");
        }
        ContentResolver resolver = context.getContentResolver();
        Uri inserted = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[32768];
            try (FileInputStream input = new FileInputStream(source)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    check(cancellation);
                    digest.update(buffer, 0, count);
                }
            }
            StringBuilder hash = new StringBuilder();
            for (byte value : digest.digest()) hash.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            String key = context.getPackageName() + ":" + hash;
            SavedImage previous = RECENT.get(key);
            if (previous != null && SystemClock.elapsedRealtime() - previous.savedAt < REUSE_MS
                    && readable(resolver, previous.uri, source.length())) {
                check(cancellation);
                return previous.uri;
            }
            RECENT.remove(key);
            check(cancellation);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, mime);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WishFox");
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            inserted = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (inserted == null) throw new IllegalStateException("MediaStore insert failed");
            // 关闭输出流后再解除 pending，避免其他 App 读到尚未写完的图片。
            try (FileInputStream input = new FileInputStream(source);
                 OutputStream output = resolver.openOutputStream(inserted)) {
                if (output == null) throw new IllegalStateException("MediaStore stream unavailable");
                int count;
                while ((count = input.read(buffer)) != -1) {
                    check(cancellation);
                    output.write(buffer, 0, count);
                }
                output.flush();
            }
            check(cancellation);
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (resolver.update(inserted, ready, null, null) != 1
                    || !readable(resolver, inserted, source.length())) {
                throw new IllegalStateException("MediaStore publish failed");
            }
            RECENT.put(key, new SavedImage(inserted));
            if (RECENT.size() > 128) RECENT.remove(RECENT.keySet().iterator().next());
            return inserted;
        } catch (Exception failure) {
            if (inserted != null) {
                try { resolver.delete(inserted, null, null); } catch (RuntimeException ignored) { }
            }
            if (failure instanceof FSMediaSaveCoordinator.SaveException) {
                throw (FSMediaSaveCoordinator.SaveException) failure;
            }
            throw new FSMediaSaveCoordinator.SaveException("GALLERY_WRITE_FAILED", "图片保存到相册失败");
        }
    }

    private static boolean readable(ContentResolver resolver, Uri uri, long size) {
        try (Cursor cursor = resolver.query(uri, new String[]{MediaStore.Images.Media.IS_PENDING}, null, null, null);
             ParcelFileDescriptor descriptor = resolver.openFileDescriptor(uri, "r")) {
            return cursor != null && cursor.moveToFirst() && cursor.getInt(0) == 0
                    && descriptor != null && descriptor.getStatSize() == size;
        } catch (Exception ignored) { return false; }
    }

    private static void check(Cancellation cancellation) throws FSMediaSaveCoordinator.SaveException {
        if (cancellation.isCancelled() || Thread.currentThread().isInterrupted()) {
            throw new FSMediaSaveCoordinator.SaveException("CANCELLED", "操作已取消");
        }
    }
}
