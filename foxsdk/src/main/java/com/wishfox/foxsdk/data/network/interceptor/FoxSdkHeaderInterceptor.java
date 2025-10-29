package com.wishfox.foxsdk.data.network.interceptor;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;

import java.util.Locale;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:31
 */
public class FoxSdkHeaderInterceptor implements okhttp3.Interceptor {

    private final FoxSdkConfig config;

    public FoxSdkHeaderInterceptor(FoxSdkConfig config) {
        this.config = config;
    }

    @Override
    public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
        okhttp3.Request original = chain.request();

        // 使用配置信息
        okhttp3.Request.Builder requestBuilder = original.newBuilder()
                .header("Lang", Locale.getDefault().getLanguage() == "zh" ? "zh_CN" : "en_US")
                .header("Content-Type", "multipart/form-data")
                .header("Platform", "android")
                .header("SysSource", "wishfoxSdk")
                .header("Platform-Type", "USER")
                .header("AppId", config.getAppId())
                .header("ChannelId", config.getChannelId())
                .header("Authorization", FSLoginResult.getTokenEd())
                .header("Version", "1.5.5");

        return chain.proceed(requestBuilder.method(original.method(), original.body()).build());
    }
}
