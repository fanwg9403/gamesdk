package com.wishfox.foxsdk.media;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.content.FileProvider;

import com.wishfox.foxsdk.utils.FoxSdkLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 主线程管理任务与唤端；文件复制/校验在 IO 线程执行。不会弹出系统分享选择器。 */
public final class FSMediaShareCoordinator {
    private static final String TAG = "FoxSdk[H5Share]";
    private static final long RETENTION_MS = 24L * 60 * 60 * 1000;
    private static final long MAX_CACHE_BYTES = 128L * 1024 * 1024;
    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final LinkedHashSet<String> requests = new LinkedHashSet<>();
    private Task active;
    private boolean destroyed;
    private long lastLaunch;

    public interface Callback {
        boolean canLaunch();
        void onProgress(JSONObject data);
        void onResult(JSONObject data);
    }

    public FSMediaShareCoordinator(Activity activity) { this.activity = activity; }

    /** 返回拒绝码；返回 null 时异步发送唯一终态。调用者先回复 accepted。 */
    public String start(String requestId, String targetId, String assetId, Callback callback) {
        if (requestId == null || requestId.trim().isEmpty() || requestId.length() > 64
                || assetId == null || !assetId.matches("asset_[A-Za-z0-9]{16,64}")) return "INVALID_ARGUMENT";
        FSShareTargetResolver.Target target = FSShareTargetResolver.find(targetId);
        if (target == null) return "INVALID_ARGUMENT";
        if (destroyed || !callback.canLaunch()) return "HOST_NOT_RESUMED";
        if (requests.contains(requestId)) return "DUPLICATE_REQUEST";
        if (active != null || (lastLaunch > 0 && SystemClock.elapsedRealtime() - lastLaunch < 1500)) return "BUSY";
        // 提前识别未安装，具体 MIME 在文件准备完成后再次检查。
        String error = FSShareTargetResolver.check(activity, target, "image/*");
        if (error != null) return error;
        requests.add(requestId);
        if (requests.size() > 256) requests.remove(requests.iterator().next());
        Task task = new Task(requestId, target, assetId, callback);
        active = task;
        task.timeout = () -> finish(task, "failed", "PREPARE_TIMEOUT");
        main.postDelayed(task.timeout, 30000);
        main.post(() -> {
            if (active != task) return;
            progress(task, "preparing");
            worker.execute(() -> prepare(task));
        });
        return null;
    }

    private void prepare(Task task) {
        File snapshot = null;
        try {
            File directory = new File(activity.getFilesDir(), "wishfox_sdk/images");
            File source = null;
            for (String extension : new String[]{".png", ".jpg", ".webp"}) {
                File candidate = new File(directory, task.assetId + extension);
                if (candidate.isFile()) { source = candidate; break; }
            }
            if (source == null) throw new ShareFailure("IMAGE_NOT_FOUND");
            long size = source.length();
            if (size <= 0 || size > FSMediaSaveCoordinator.MAX_IMAGE_BYTES) throw new ShareFailure("FILE_TOO_LARGE");
            File cache = new File(activity.getCacheDir(), "wishfox_share");
            if (!cache.isDirectory() && !cache.mkdirs()) throw new ShareFailure("CACHE_FAILED");
            long bytes = cleanExpired(cache);
            if (bytes + size > MAX_CACHE_BYTES) throw new ShareFailure("SHARE_CACHE_FULL");
            String extension = source.getName().substring(source.getName().lastIndexOf('.'));
            snapshot = new File(cache, UUID.randomUUID().toString() + extension);
            // 独立副本避免 H5 删除原 asset 或 Overlay 销毁后目标应用读取失败。
            try (FileInputStream input = new FileInputStream(source);
                 FileOutputStream output = new FileOutputStream(snapshot)) {
                byte[] buffer = new byte[32768];
                long count = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (task.finished || Thread.currentThread().isInterrupted()) throw new ShareFailure("CANCELLED");
                    count += read;
                    if (count > FSMediaSaveCoordinator.MAX_IMAGE_BYTES) throw new ShareFailure("FILE_TOO_LARGE");
                    output.write(buffer, 0, read);
                }
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(snapshot.getAbsolutePath(), options);
            if (options.outWidth <= 0 || options.outHeight <= 0
                    || (long) options.outWidth * options.outHeight > 64000000L) {
                throw new ShareFailure("IMAGE_VALIDATION_FAILED");
            }
            String mime = options.outMimeType;
            if (!"image/png".equals(mime) && !"image/jpeg".equals(mime) && !"image/webp".equals(mime)) {
                throw new ShareFailure("UNSUPPORTED_IMAGE_TYPE");
            }
            task.galleryStatus = "saving";
            main.post(() -> { if (!task.finished && active == task) progress(task, "saving_gallery"); });
            String galleryName = "wishfox-poster-" + System.currentTimeMillis() + extension;
            try {
                task.galleryUri = FSGalleryWriter.save(activity, snapshot, galleryName, mime, () -> task.finished).toString();
                task.galleryStatus = "saved";
            } catch (FSMediaSaveCoordinator.SaveException failure) {
                if (!"UNSUPPORTED_NO_PERMISSION".equals(failure.code)) throw failure;
                // 旧系统无权限写公共相册时，继续通过 FileProvider 分享沙盒副本。
                task.galleryStatus = "unsupported_no_permission";
            }
            FoxSdkLogger.d(TAG, "gallery prepared: target=" + task.target.id
                    + ", status=" + task.galleryStatus + ", bytes=" + size);
            File ready = snapshot;
            main.post(() -> launch(task, ready, mime));
        } catch (Exception failure) {
            if (snapshot != null) snapshot.delete();
            String code = failure instanceof ShareFailure ? ((ShareFailure) failure).code
                    : failure instanceof FSMediaSaveCoordinator.SaveException ? ((FSMediaSaveCoordinator.SaveException) failure).code
                    : failure instanceof IOException ? "CACHE_FAILED" : "IMAGE_VALIDATION_FAILED";
            if ("saving".equals(task.galleryStatus)) task.galleryStatus = "failed";
            main.post(() -> finish(task, "failed", code));
        }
    }

    private long cleanExpired(File directory) {
        File[] files = directory.listFiles();
        long retained = 0;
        if (files != null) for (File file : files) {
            if (!file.isFile()) continue;
            if (System.currentTimeMillis() - file.lastModified() > RETENTION_MS && file.delete()) continue;
            retained += file.length();
        }
        return retained;
    }

    private void launch(Task task, File image, String mime) {
        if (task.finished || destroyed || active != task) { image.delete(); return; }
        String error = null;
        try {
            if (!task.callback.canLaunch() || activity.isFinishing() || activity.isDestroyed()) {
                throw new ShareFailure("HOST_NOT_RESUMED");
            }
            error = FSShareTargetResolver.check(activity, task.target, mime);
            if (error != null) throw new ShareFailure(error);
            Uri uri;
            try {
                uri = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".wishfox.share.fileprovider", image);
            } catch (IllegalArgumentException invalidProvider) {
                throw new ShareFailure("IMAGE_URI_FAILED");
            }
            Intent intent = new Intent(Intent.ACTION_SEND).setComponent(task.target.component()).setType(mime)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newUri(activity.getContentResolver(), "wishfox_share_image", uri));
            progress(task, "launching");
            FoxSdkLogger.d(TAG, "launch: target=" + task.target.id
                    + ", version=" + FSShareTargetResolver.version(activity, task.target)
                    + ", component=" + task.target.component().flattenToShortString()
                    + ", mime=" + mime + ", bytes=" + image.length());
            activity.startActivity(intent);
            lastLaunch = SystemClock.elapsedRealtime();
            // 仅证明 startActivity 返回，不能证明目标已经导入图片或用户发布成功。
            finish(task, "launched", null);
        } catch (ShareFailure failure) {
            error = failure.code;
        } catch (ActivityNotFoundException missing) {
            error = "TARGET_UNSUPPORTED";
        } catch (SecurityException denied) {
            error = "TARGET_ACCESS_DENIED";
        } catch (RuntimeException failure) {
            error = "LAUNCH_FAILED";
        }
        if (error != null) {
            image.delete();
            finish(task, "failed", error);
        }
    }

    private void progress(Task task, String stage) {
        try { task.callback.onProgress(new JSONObject().put("requestId", task.id)
                .put("target", task.target.id).put("stage", stage)
                .put("galleryStatus", task.galleryStatus)); }
        catch (JSONException ignored) { }
    }

    private void finish(Task task, String status, String code) {
        if (task.finished) return;
        task.finished = true;
        main.removeCallbacks(task.timeout);
        if (active == task) active = null;
        FoxSdkLogger.d(TAG, "result: target=" + task.target.id + ", status=" + status + ", code=" + code);
        if (destroyed) return;
        try {
            JSONObject result = new JSONObject().put("requestId", task.id).put("target", task.target.id)
                    .put("status", status).put("mode", "explicit_intent")
                    .put("galleryStatus", task.galleryStatus)
                    .put("galleryUri", task.galleryUri == null ? JSONObject.NULL : task.galleryUri)
                    .put("publicationStatus", "unknown");
            result.put("error", code == null ? JSONObject.NULL : new JSONObject().put("code", code)
                    .put("message", FSMediaFeedback.message(code, task.target.appName)));
            task.callback.onResult(result);
        } catch (JSONException ignored) { }
    }

    public boolean cancel(String requestId) {
        if (active == null || !active.id.equals(requestId)) return false;
        finish(active, "cancelled", "CANCELLED");
        return true;
    }

    public void onHostPaused() {
        if (active != null) finish(active, "failed", "HOST_NOT_RESUMED");
    }

    public void destroy() {
        destroyed = true;
        if (active != null) finish(active, "cancelled", "CANCELLED");
        worker.shutdownNow();
        requests.clear();
    }

    private static final class Task {
        final String id;
        final FSShareTargetResolver.Target target;
        final String assetId;
        final Callback callback;
        volatile boolean finished;
        volatile String galleryStatus = "not_started";
        volatile String galleryUri;
        Runnable timeout;
        Task(String id, FSShareTargetResolver.Target target, String assetId, Callback callback) {
            this.id = id; this.target = target; this.assetId = assetId; this.callback = callback;
        }
    }

    private static final class ShareFailure extends Exception {
        final String code;
        ShareFailure(String code) { this.code = code; }
    }
}
