package com.wishfox.foxsdk.media;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.WebResourceResponse;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.utils.FoxSdkLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * H5 图片保存协调器。
 *
 * <p>所有下载、校验、hash、沙盒文件和 MediaStore 操作均在 IO 线程执行；
 * API 21～28 的系统文件选择器通过 FSFileSaveActivity 完成。</p>
 */
public final class FSMediaSaveCoordinator {
    public static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_DATA_URL_BYTES = 5L * 1024 * 1024;
    private static final int MAX_REDIRECTS = 5;
    private static final long DOWNLOAD_TIMEOUT_MS = 120000L;
    private static final String TAG = "FoxSdk[H5MediaSave]";

    public interface Callback {
        void onProgress(JSONObject data);
        void onResult(JSONObject data);
    }

    public interface RemoveCallback { void complete(boolean removed, String error); }

    private static final Map<String, PickerRequest> PICKERS = new ConcurrentHashMap<>();

    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final Set<String> knownRequests = Collections.newSetFromMap(
            new ConcurrentHashMap<String, Boolean>());
    private volatile boolean destroyed;

    public FSMediaSaveCoordinator(Activity activity) {
        this.activity = activity;
    }

    public boolean contains(String requestId) {
        return requestId != null && knownRequests.contains(requestId);
    }

    public String validate(JSONObject params) {
        try {
            SaveInput input = parseInput(params);
            if (!input.dataUrl && !allowedMediaUrl(input.sourceValue)) return "IMAGE_HOST_NOT_ALLOWED";
            return null;
        } catch (SaveException failure) {
            return failure.code;
        }
    }

    public boolean start(String requestId, JSONObject params, Callback callback) {
        Task task = new Task(requestId, params, callback);
        if (destroyed || !knownRequests.add(requestId) || tasks.putIfAbsent(requestId, task) != null) return false;
        task.future = executor.submit(() -> run(task));
        return true;
    }

    public boolean cancel(String requestId) {
        Task task = tasks.get(requestId);
        if (task == null) return false;
        task.cancelled.set(true);
        Future<?> future = task.future;
        if (future != null) future.cancel(true);
        if (task.connection != null) task.connection.disconnect();
        if (task.pickerId != null) PICKERS.remove(task.pickerId);
        deleteQuietly(task.temporary);
        finish(task, cancelledResult(task));
        if (tasks.get(requestId) == task) tasks.remove(requestId);
        return true;
    }

    public void removeSandboxImage(String assetId, RemoveCallback callback) {
        if (destroyed || callback == null) return;
        executor.execute(() -> {
            if (assetId == null || !assetId.matches("asset_[A-Za-z0-9]{16,64}")) {
                main.post(() -> callback.complete(false, "INVALID_ARGUMENT"));
                return;
            }
            boolean removed = false;
            File directory = new File(activity.getFilesDir(), "wishfox_sdk/images");
            for (String extension : new String[]{".png", ".jpg", ".webp"}) {
                File candidate = new File(directory, assetId + extension);
                if (candidate.exists()) removed |= candidate.delete();
            }
            boolean result = removed;
            main.post(() -> { if (!destroyed) callback.complete(result, null); });
        });
    }

    public WebResourceResponse openSandboxAsset(String value) {
        if (TextUtils.isEmpty(value)) return null;
        try {
            Uri uri = Uri.parse(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"appassets.androidplatform.net".equalsIgnoreCase(uri.getHost())
                    || !uri.getPath().startsWith("/assets/wishfox/")) return null;
            String assetId = uri.getLastPathSegment();
            if (assetId == null || !assetId.matches("asset_[A-Za-z0-9]{16,64}")) return null;
            File directory = new File(activity.getFilesDir(), "wishfox_sdk/images");
            for (String extension : new String[]{".png", ".jpg", ".webp"}) {
                File candidate = new File(directory, assetId + extension);
                if (!candidate.isFile()) continue;
                String mime = ".png".equals(extension) ? "image/png"
                        : (".webp".equals(extension) ? "image/webp" : "image/jpeg");
                return new WebResourceResponse(mime, null, new FileInputStream(candidate));
            }
        } catch (Exception ignored) { }
        return null;
    }

    public void destroy() {
        destroyed = true;
        for (String requestId : tasks.keySet()) cancel(requestId);
        tasks.clear();
        knownRequests.clear();
        executor.shutdownNow();
    }

    private void run(Task task) {
        File downloaded = null;
        try {
            SaveInput input = parseInput(task.params);
            emitProgress(task, "downloading", 0L, null);
            downloaded = input.dataUrl ? decodeDataUrl(input.sourceValue, task) : download(input.sourceValue, task);
            task.temporary = downloaded;
            emitProgress(task, "validating", downloaded.length(), downloaded.length());
            validateImage(downloaded, input.mimeType);
            String assetId = "asset_" + UUID.randomUUID().toString().replace("-", "");
            File sandboxFile = saveToSandbox(downloaded, assetId, input.mimeType, task);
            emitProgress(task, "saving_sandbox", sandboxFile.length(), sandboxFile.length());
            String sha256 = sha256(sandboxFile);

            GalleryResult gallery = saveGalleryIfRequested(
                    task, sandboxFile, input.fileName, input.mimeType, input.legacyGalleryMode);
            JSONObject result = result(task.requestId, true, assetId, sandboxFile, input,
                    sha256, gallery.status, gallery.uri, gallery.error);
            finish(task, result);
        } catch (SaveException failure) {
            String status = "CANCELLED".equals(failure.code) ? "cancelled" : "failed";
            JSONObject error = error(failure.code, failure.getMessage());
            JSONObject result;
            try {
                result = new JSONObject().put("requestId", task.requestId)
                        .put("success", false)
                        .put("sandboxStatus", status)
                        .put("galleryStatus", "not_requested")
                        .put("galleryUri", JSONObject.NULL)
                        .put("fileName", task.params.optString("fileName", ""))
                        .put("mimeType", task.params.optString("mimeType", ""))
                        .put("sizeBytes", 0)
                        .put("sha256", JSONObject.NULL)
                        .put("error", error);
            } catch (JSONException ignored) {
                result = null;
            }
            if (result != null) finish(task, result);
        } catch (Throwable failure) {
            FoxSdkLogger.e(TAG, "save image failed: requestId=" + task.requestId, failure);
            try {
                finish(task, new JSONObject().put("requestId", task.requestId)
                        .put("success", false)
                        .put("sandboxStatus", "failed")
                        .put("galleryStatus", "not_requested")
                        .put("galleryUri", JSONObject.NULL)
                        .put("error", error("INTERNAL_ERROR", "图片保存失败")));
            } catch (JSONException ignored) { }
        } finally {
            deleteQuietly(downloaded);
            if (task.pickerId != null) PICKERS.remove(task.pickerId);
            if (tasks.get(task.requestId) == task) tasks.remove(task.requestId);
        }
    }

    private JSONObject cancelledResult(Task task) {
        try {
            return new JSONObject().put("requestId", task.requestId)
                    .put("success", false)
                    .put("sandboxStatus", "cancelled")
                    .put("galleryStatus", "cancelled")
                    .put("galleryUri", JSONObject.NULL)
                    .put("error", error("USER_CANCELLED", "用户取消保存"));
        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }

    private SaveInput parseInput(JSONObject params) throws SaveException {
        if (params == null) throw new SaveException("INVALID_ARGUMENT", "参数不能为空");
        String requestId = params.optString("requestId", "").trim();
        JSONObject source = params.optJSONObject("source");
        String fileName = params.optString("fileName", "").trim();
        String mimeType = params.optString("mimeType", "").trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(mimeType)) mimeType = "image/jpeg";
        if (requestId.isEmpty() || requestId.length() > 64 || source == null
                || fileName.isEmpty() || fileName.length() > 128
                || mimeType.isEmpty()) {
            throw new SaveException("INVALID_ARGUMENT", "图片保存参数无效");
        }
        String type = source.optString("type", "");
        String value = source.optString("value", "");
        if (value.isEmpty() || (!"https_url".equals(type) && !"data_url".equals(type))) {
            throw new SaveException("INVALID_IMAGE_URL", "图片来源无效");
        }
        if (!isSupportedMime(mimeType)) {
            throw new SaveException("UNSUPPORTED_IMAGE_TYPE", "不支持的图片类型");
        }
        if (!params.has("saveToGallery") || !(params.opt("saveToGallery") instanceof Boolean)) {
            throw new SaveException("INVALID_ARGUMENT", "saveToGallery 参数无效");
        }
        String mode = params.optString("legacyGalleryMode", "none");
        if (!"none".equals(mode) && !"system_picker".equals(mode)) {
            throw new SaveException("INVALID_ARGUMENT", "legacyGalleryMode 参数无效");
        }
        return new SaveInput(type, value, safeFileName(fileName), mimeType,
                params.optBoolean("saveToGallery", false), mode, "data_url".equals(type));
    }

    private File decodeDataUrl(String value, Task task) throws SaveException {
        int comma = value.indexOf(',');
        if (!value.startsWith("data:") || comma < 0 || value.length() > MAX_DATA_URL_BYTES * 2) {
            throw new SaveException("INVALID_IMAGE_URL", "data URL 无效");
        }
        String metadata = value.substring(5, comma).toLowerCase(Locale.ROOT);
        if (!metadata.contains(";base64") || !metadata.startsWith("image/")) {
            throw new SaveException("INVALID_IMAGE_URL", "仅支持 base64 图片 data URL");
        }
        byte[] bytes;
        try {
            bytes = Base64.decode(value.substring(comma + 1), Base64.DEFAULT);
        } catch (IllegalArgumentException failure) {
            throw new SaveException("IMAGE_VALIDATION_FAILED", "data URL 解码失败");
        }
        if (bytes.length == 0 || bytes.length > MAX_DATA_URL_BYTES) {
            throw new SaveException("FILE_TOO_LARGE", "图片超过大小限制");
        }
        File file = createTemporary(task.requestId);
        task.temporary = file;
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            return file;
        } catch (Exception failure) {
            deleteQuietly(file);
            throw new SaveException("DOWNLOAD_FAILED", "data URL 写入失败");
        }
    }

    private File download(String source, Task task) throws SaveException {
        String next = source;
        File file = createTemporary(task.requestId);
        task.temporary = file;
        long deadline = android.os.SystemClock.elapsedRealtime() + DOWNLOAD_TIMEOUT_MS;
        try {
            for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
                checkCancelled(task);
                if (!allowedMediaUrl(next)) throw new SaveException("IMAGE_HOST_NOT_ALLOWED", "图片来源不在白名单");
                HttpURLConnection connection = (HttpURLConnection) new URL(next).openConnection();
                task.connection = connection;
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "image/png,image/jpeg,image/webp,image/*;q=0.8");
                try {
                    int status = connection.getResponseCode();
                    if (status >= 300 && status <= 399) {
                        String location = connection.getHeaderField("Location");
                        if (location == null || redirect == MAX_REDIRECTS) {
                            throw new SaveException("DOWNLOAD_FAILED", "图片重定向失败");
                        }
                        next = new URL(new URL(next), location).toString();
                        continue;
                    }
                    if (status != HttpURLConnection.HTTP_OK) {
                        throw new SaveException("DOWNLOAD_FAILED", "图片下载失败");
                    }
                    String responseMime = contentMime(connection.getContentType());
                    if (responseMime != null && !isSupportedMime(responseMime)) {
                        throw new SaveException("UNSUPPORTED_IMAGE_TYPE", "响应不是支持的图片类型");
                    }
                    long total = connection.getContentLength();
                    if (total > MAX_IMAGE_BYTES) throw new SaveException("FILE_TOO_LARGE", "图片超过大小限制");
                    try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(file)) {
                        byte[] buffer = new byte[32768];
                        long count = 0;
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            checkCancelled(task);
                            if (android.os.SystemClock.elapsedRealtime() > deadline) {
                                throw new SaveException("DOWNLOAD_TIMEOUT", "图片下载超时");
                            }
                            count += read;
                            if (count > MAX_IMAGE_BYTES) throw new SaveException("FILE_TOO_LARGE", "图片超过大小限制");
                            output.write(buffer, 0, read);
                            emitProgress(task, "downloading", count, total > 0 ? total : null);
                        }
                        if (count == 0) throw new SaveException("DOWNLOAD_FAILED", "图片内容为空");
                    }
                    return file;
                } finally {
                    connection.disconnect();
                    task.connection = null;
                }
            }
            throw new SaveException("DOWNLOAD_FAILED", "图片重定向次数过多");
        } catch (SaveException failure) {
            deleteQuietly(file);
            throw failure;
        } catch (Exception failure) {
            deleteQuietly(file);
            if (task.cancelled.get()) throw new SaveException("CANCELLED", "用户取消保存");
            throw new SaveException("DOWNLOAD_FAILED", "图片下载失败");
        }
    }

    private File saveToSandbox(File source, String assetId, String mimeType, Task task) throws SaveException {
        checkCancelled(task);
        File directory = new File(activity.getFilesDir(), "wishfox_sdk/images");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new SaveException("SANDBOX_WRITE_FAILED", "沙盒目录创建失败");
        }
        String extension = extensionForMime(mimeType);
        File temporary = new File(directory, assetId + ".tmp");
        File target = new File(directory, assetId + extension);
        try {
            copy(source, temporary, task);
            if (!temporary.renameTo(target)) throw new SaveException("SANDBOX_WRITE_FAILED", "沙盒原子保存失败");
            return target;
        } catch (SaveException failure) {
            deleteQuietly(temporary);
            throw failure;
        } catch (Exception failure) {
            deleteQuietly(temporary);
            throw new SaveException("SANDBOX_WRITE_FAILED", "沙盒保存失败");
        }
    }

    private GalleryResult saveGalleryIfRequested(
            Task task, File source, String fileName, String mimeType, String legacyMode
    ) throws SaveException {
        if (!task.params.optBoolean("saveToGallery", false)) {
            return new GalleryResult("not_requested", null, null);
        }
        emitProgress(task, "saving_gallery", source.length(), source.length());
        if (Build.VERSION.SDK_INT >= 29) return saveToMediaStore(source, fileName, mimeType, task);
        if (!"system_picker".equals(legacyMode)) {
            return new GalleryResult("unsupported_no_permission", null,
                    error("UNSUPPORTED_NO_PERMISSION", "当前系统版本无法在无存储权限下自动写入公共相册"));
        }
        return saveWithSystemPicker(source, fileName, mimeType, task);
    }

    private GalleryResult saveToMediaStore(File source, String fileName, String mimeType, Task task)
            throws SaveException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WishFox");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri uri;
        try {
            uri = activity.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable failure) {
            return new GalleryResult("failed", null,
                    error("GALLERY_WRITE_FAILED", "相册记录创建失败"));
        }
        if (uri == null) {
            return new GalleryResult("failed", null,
                    error("GALLERY_WRITE_FAILED", "相册记录创建失败"));
        }
        try (InputStream input = new FileInputStream(source);
             OutputStream output = activity.getContentResolver().openOutputStream(uri)) {
            if (output == null) throw new SaveException("GALLERY_WRITE_FAILED", "相册文件打开失败");
            copy(input, output, task);
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            activity.getContentResolver().update(uri, ready, null, null);
            return new GalleryResult("saved", uri.toString(), null);
        } catch (SaveException failure) {
            activity.getContentResolver().delete(uri, null, null);
            if ("CANCELLED".equals(failure.code)) throw failure;
            return new GalleryResult("failed", null, error(failure.code, failure.getMessage()));
        } catch (Exception failure) {
            activity.getContentResolver().delete(uri, null, null);
            return new GalleryResult("failed", null,
                    error("GALLERY_WRITE_FAILED", "相册保存失败"));
        }
    }

    private GalleryResult saveWithSystemPicker(
            File source, String fileName, String mimeType, Task task
    ) throws SaveException {
        CountDownLatch latch = new CountDownLatch(1);
        PickerResult[] result = new PickerResult[1];
        String pickerId = task.requestId + "_" + UUID.randomUUID();
        task.pickerId = pickerId;
        PICKERS.put(pickerId, new PickerRequest(source, fileName, mimeType,
                (success, uri, error) -> {
                    result[0] = new PickerResult(success, uri, error);
                    latch.countDown();
                }));
        main.post(() -> {
            try {
                Intent intent = new Intent(activity, FSFileSaveActivity.class)
                        .putExtra(FSFileSaveActivity.EXTRA_PICKER_ID, pickerId);
                activity.startActivity(intent);
            } catch (Throwable failure) {
                PickerRequest request = PICKERS.remove(pickerId);
                if (request != null) request.callback.complete(false, null, "GALLERY_WRITE_FAILED");
            }
        });
        try {
            while (!latch.await(500, TimeUnit.MILLISECONDS)) checkCancelled(task);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new SaveException("CANCELLED", "用户取消保存");
        }
        PickerResult pickerResult = result[0];
        if (pickerResult == null || !pickerResult.success) {
            if (pickerResult != null && "USER_CANCELLED".equals(pickerResult.error)) {
                return new GalleryResult("cancelled", null,
                        error("USER_CANCELLED", "用户取消相册保存"));
            }
            return new GalleryResult("failed", null,
                    error("GALLERY_WRITE_FAILED", "系统选择器保存失败"));
        }
        return new GalleryResult("user_selected", pickerResult.uri, null);
    }

    private void validateImage(File file, String mimeType) throws SaveException {
        if (file == null || !file.isFile() || file.length() <= 0 || file.length() > MAX_IMAGE_BYTES) {
            throw new SaveException("FILE_TOO_LARGE", "图片大小无效");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw new SaveException("IMAGE_VALIDATION_FAILED", "图片内容无效");
        }
        if (!isSupportedMime(mimeType)) throw new SaveException("UNSUPPORTED_IMAGE_TYPE", "图片类型不支持");
        String decodedMime = contentMime(options.outMimeType);
        if (decodedMime != null && !mimeType.equals(decodedMime)
                && !("image/jpeg".equals(mimeType) && "image/jpg".equals(decodedMime))) {
            throw new SaveException("IMAGE_VALIDATION_FAILED", "图片类型与声明不一致");
        }
    }

    private boolean allowedMediaUrl(String value) {
        if (TextUtils.isEmpty(value) || value.length() > 4096) return false;
        try {
            Uri uri = Uri.parse(value);
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                FoxSdkConfig config = WishFoxSdk.getConfig();
                List<String> origins = config.getH5MediaOrigins();
                if (origins == null || origins.isEmpty()) {
                    String trusted = config.getH5TrustedOrigin();
                    if (TextUtils.isEmpty(trusted)) return false;
                    origins = java.util.Collections.singletonList(FSMediaPolicy.origin(trusted));
                }
                return FSMediaPolicy.allowed(value, origins);
            }
            // 仅为本地/内网调试保留 HTTP，并且必须与可信 H5 Origin 精确一致。
            if (!WishFoxSdk.getConfig().isAllowInsecureH5()
                    || !"http".equalsIgnoreCase(uri.getScheme())) return false;
            String trusted = WishFoxSdk.getConfig().getH5TrustedOrigin();
            return trusted != null && sameHttpOrigin(value, trusted);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean sameHttpOrigin(String first, String second) {
        Uri a = Uri.parse(first);
        Uri b = Uri.parse(second);
        return "http".equalsIgnoreCase(a.getScheme()) && "http".equalsIgnoreCase(b.getScheme())
                && TextUtils.equals(a.getHost(), b.getHost()) && a.getPort() == b.getPort();
    }

    private void emitProgress(Task task, String stage, long received, Long total) {
        if (task.cancelled.get() || destroyed || task.callback == null) return;
        try {
            JSONObject data = new JSONObject().put("requestId", task.requestId)
                    .put("stage", stage)
                    .put("bytesReceived", received)
                    .put("totalBytes", total == null ? JSONObject.NULL : total)
                    .put("percent", total == null || total <= 0 ? JSONObject.NULL
                            : Math.min(100, Math.round(received * 100f / total)));
            main.post(() -> { if (!destroyed && !task.cancelled.get()) task.callback.onProgress(data); });
        } catch (JSONException ignored) { }
    }

    private void finish(Task task, JSONObject result) {
        if (destroyed || task.callback == null || !task.resultSent.compareAndSet(false, true)) return;
        main.post(() -> { if (!destroyed) task.callback.onResult(result); });
    }

    private JSONObject result(String requestId, boolean success, String assetId, File sandbox,
                              SaveInput input, String sha256, String galleryStatus,
                              String galleryUri, JSONObject error) throws JSONException {
        return new JSONObject().put("requestId", requestId).put("success", success)
                .put("assetId", assetId).put("sandboxStatus", "saved")
                .put("sandboxUri", "https://appassets.androidplatform.net/assets/wishfox/" + assetId)
                .put("galleryStatus", galleryStatus)
                .put("galleryUri", galleryUri == null ? JSONObject.NULL : galleryUri)
                .put("fileName", input.fileName).put("mimeType", input.mimeType)
                .put("sizeBytes", sandbox.length()).put("sha256", sha256)
                .put("error", error == null ? JSONObject.NULL : error);
    }

    private JSONObject error(String code, String message) {
        try { return new JSONObject().put("code", code).put("message", message); }
        catch (JSONException ignored) { return null; }
    }

    private void copy(File source, File target, Task task) throws Exception {
        try (InputStream input = new FileInputStream(source); OutputStream output = new FileOutputStream(target)) {
            copy(input, output, task);
        }
    }

    private void copy(InputStream input, OutputStream output, Task task) throws Exception {
        byte[] buffer = new byte[32768];
        int read;
        while ((read = input.read(buffer)) != -1) {
            checkCancelled(task);
            output.write(buffer, 0, read);
        }
        output.flush();
    }

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[32768];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format(Locale.US, "%02x", value));
        return result.toString();
    }

    private File createTemporary(String requestId) throws SaveException {
        File directory = new File(activity.getCacheDir(), "wishfox_sdk/images");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new SaveException("DOWNLOAD_FAILED", "临时目录创建失败");
        }
        return new File(directory, requestId.replaceAll("[^A-Za-z0-9._-]", "_") + ".tmp");
    }

    private void checkCancelled(Task task) throws SaveException {
        if (task.cancelled.get() || destroyed || Thread.currentThread().isInterrupted()) {
            throw new SaveException("CANCELLED", "用户取消保存");
        }
    }

    private static String contentMime(String value) {
        if (value == null) return null;
        int separator = value.indexOf(';');
        String mime = (separator < 0 ? value : value.substring(0, separator))
                .trim().toLowerCase(Locale.ROOT);
        return "image/jpg".equals(mime) ? "image/jpeg" : mime;
    }

    private static boolean isSupportedMime(String value) {
        return "image/png".equals(value) || "image/jpeg".equals(value) || "image/webp".equals(value);
    }

    private static String extensionForMime(String mimeType) {
        if ("image/png".equals(mimeType)) return ".png";
        if ("image/webp".equals(mimeType)) return ".webp";
        return ".jpg";
    }

    private static String safeFileName(String value) {
        String result = value.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
        return result.isEmpty() ? "wishfox-image" : result;
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) file.delete();
    }

    static PickerRequest picker(String id) { return PICKERS.get(id); }

    static void completePicker(String id, boolean success, String uri, String error) {
        PickerRequest request = PICKERS.remove(id);
        if (request != null) request.callback.complete(success, uri, error);
    }

    static final class PickerRequest {
        final File source;
        final String fileName;
        final String mimeType;
        final PickerCallback callback;
        PickerRequest(File source, String fileName, String mimeType, PickerCallback callback) {
            this.source = source; this.fileName = fileName; this.mimeType = mimeType; this.callback = callback;
        }
    }

    interface PickerCallback { void complete(boolean success, String uri, String error); }

    private static final class PickerResult {
        final boolean success; final String uri; final String error;
        PickerResult(boolean success, String uri, String error) {
            this.success = success; this.uri = uri; this.error = error;
        }
    }

    private static final class GalleryResult {
        final String status; final String uri; final JSONObject error;
        GalleryResult(String status, String uri, JSONObject error) {
            this.status = status; this.uri = uri; this.error = error;
        }
    }

    private static final class SaveInput {
        final String sourceType; final String sourceValue; final String fileName; final String mimeType;
        final boolean saveToGallery; final String legacyGalleryMode; final boolean dataUrl;
        SaveInput(String sourceType, String sourceValue, String fileName, String mimeType,
                  boolean saveToGallery, String legacyGalleryMode, boolean dataUrl) {
            this.sourceType = sourceType; this.sourceValue = sourceValue; this.fileName = fileName;
            this.mimeType = mimeType; this.saveToGallery = saveToGallery;
            this.legacyGalleryMode = legacyGalleryMode; this.dataUrl = dataUrl;
        }
    }

    private static final class Task {
        final String requestId; final JSONObject params; final Callback callback;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final AtomicBoolean resultSent = new AtomicBoolean(false);
        volatile Future<?> future; volatile HttpURLConnection connection; volatile File temporary;
        volatile String pickerId;
        Task(String requestId, JSONObject params, Callback callback) {
            this.requestId = requestId; this.params = params; this.callback = callback;
        }
    }

    public static final class SaveException extends Exception {
        final String code;
        SaveException(String code, String message) { super(message); this.code = code; }
    }
}
