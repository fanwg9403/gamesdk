package com.wishfox.foxsdk.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.wishfox.foxsdk.utils.FoxSdkDynamicParamReader;
import com.wishfox.foxsdk.utils.FoxSdkLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 管理当前安装包的动态分包参数。
 *
 * <p>每次初始化只读取当前 APK Signing Block，结果仅保存在当前配置内存中。
 * 母包没有动态参数时将配置置空，不使用历史分包参数；旧版磁盘缓存会被清理。</p>
 */
final class FoxSdkDynamicParamManager {

    private static final String TAG = "FoxSdk[DynamicParam]";
    // 仅用于清理旧版 SDK 的专用缓存，新版不再持久化动态参数。
    private static final String PREFS_NAME = "WishFoxSdkDynamicParam";

    private FoxSdkDynamicParamManager() {
    }

    static synchronized void initialize(Context context, FoxSdkConfig config) {
        if (config == null) {
            return;
        }
        // 即使复用同一个配置对象，读取失败或母包无参数也不能留下上次的值。
        config.setDynamicParam(null);
        if (context == null) {
            FoxSdkLogger.w(TAG, "动态参数初始化失败：Context 为空，不使用历史参数");
            return;
        }

        try {
            Context applicationContext = context.getApplicationContext();
            if (applicationContext == null) {
                applicationContext = context;
            }
            clearLegacyCache(applicationContext);
            String appId = config.getAppId();
            String channelId = config.getChannelId();

            FoxSdkLogger.d(TAG, "开始初始化动态参数：blockId=0x"
                    + Integer.toHexString(FoxSdkDynamicParamReader.WISHFOX_DYNAMIC_JSON_BLOCK_ID)
                    + ", appId=" + appId + ", channelId=" + channelId);
            String currentDynamicParam = normalize(FoxSdkDynamicParamReader.read(applicationContext));

            config.setDynamicParam(currentDynamicParam);
            logResult(currentDynamicParam == null ? "APK_EMPTY" : "APK_SIGNING_BLOCK", currentDynamicParam);
        } catch (RuntimeException ignored) {
            // 动态参数读取失败不能阻断 SDK 的基础初始化和原有网络能力。
            config.setDynamicParam(null);
            FoxSdkLogger.e(TAG, "动态参数初始化失败，后续请求将不携带 DynamicParam", ignored);
        }
    }

    private static void clearLegacyCache(Context context) {
        try {
            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (!preferences.getAll().isEmpty()) {
                preferences.edit().clear().apply();
                FoxSdkLogger.d(TAG, "已提交旧版动态参数缓存清理，后续仅使用当前 APK 参数");
            }
        } catch (RuntimeException e) {
            // 磁盘清理异常不影响读取当前 APK；旧缓存无论是否清理成功都不再使用。
            FoxSdkLogger.w(TAG, "旧版动态参数缓存清理失败，仍仅使用当前 APK 参数："
                    + e.getClass().getSimpleName());
        }
    }

    private static void logResult(String source, String dynamicParam) {
        if (dynamicParam == null) {
            FoxSdkLogger.d(TAG, "动态参数读取结果：source=" + source + ", present=false");
            return;
        }

        byte[] bytes = dynamicParam.getBytes(StandardCharsets.UTF_8);
        FoxSdkLogger.d(TAG, "动态参数读取成功：source=" + source
                + ", present=true, payloadBytes=" + bytes.length
                + ", sha256=" + sha256(bytes)
                + ", raw=" + dynamicParam);
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format(java.util.Locale.US, "%02x", value & 0xFF));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unavailable";
        }
    }

    private static String normalize(String value) {
        return value == null || value.length() == 0 ? null : value;
    }

}
