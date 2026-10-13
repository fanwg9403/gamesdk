package com.wishfox.foxsdk.data.network.interceptor;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.utils.FoxSdkLogger;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:31
 */
public class FoxSdkHeaderInterceptor implements okhttp3.Interceptor {

    private static final String TAG = "FoxSdk[DynamicParam]";
    private final FoxSdkConfig config;
    private final AtomicBoolean dynamicHeaderLogReported = new AtomicBoolean(false);

    public FoxSdkHeaderInterceptor(FoxSdkConfig config) {
        this.config = config;
    }

    @Override
    public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
        okhttp3.Request original = chain.request();

        // 使用配置信息
        String authorization = original.header("Authorization");
        if (authorization == null) authorization = FSLoginResult.getTokenEd();
        okhttp3.Request.Builder requestBuilder = original.newBuilder()
                .header("Lang", Locale.getDefault().getLanguage() == "zh" ? "zh_CN" : "en_US")
//                .header("Content-Type", "multipart/form-data")
                .header("Platform", "android")
                .header("SysSource", "wishfoxSdk")
                .header("Platform-Type", "USER")
                .header("AppId", config.getAppId())
                .header("ChannelId", config.getChannelId())
                .header("Authorization", authorization)
                .header("Version", "1.5.5");

        // 动态参数是服务端生成的 opaque JSON，SDK 不解析、不改写，只在有值时透传。
        // 清除原请求可能携带的旧值，确保母包无参数或新值非法时不会残留旧 Header。
        requestBuilder.removeHeader("DynamicParam");
        String dynamicParam = config.getDynamicParam();
        if (dynamicParam != null && !dynamicParam.isEmpty()) {
            try {
                requestBuilder.header("DynamicParam", dynamicParam);
                if (dynamicHeaderLogReported.compareAndSet(false, true)) {
                    FoxSdkLogger.d(TAG, "DynamicParam 已注入公共请求头：present=true, payloadBytes="
                            + dynamicParam.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                }
            } catch (IllegalArgumentException ignored) {
                // HTTP Header 不允许控制字符；服务端打包校验应阻止此类值，SDK 保持请求可用。
                if (dynamicHeaderLogReported.compareAndSet(false, true)) {
                    FoxSdkLogger.e(TAG, "DynamicParam 无法注入公共请求头：Header 包含非法字符", ignored);
                }
            }
        } else if (dynamicHeaderLogReported.compareAndSet(false, true)) {
            FoxSdkLogger.d(TAG, "DynamicParam 未注入公共请求头：present=false");
        }

        return chain.proceed(requestBuilder.method(original.method(), original.body()).build());
    }
}
