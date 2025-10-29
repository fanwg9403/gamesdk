package com.wishfox.foxsdk.data.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wishfox.foxsdk.data.network.interceptor.FoxSdkHeaderInterceptor;
import com.wishfox.foxsdk.data.network.interceptor.FoxSdkLoggingInterceptor;
import com.wishfox.foxsdk.core.FoxSdkConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:23
 */
public class FoxSdkRetrofitManager {

    private static final String TAG = "FoxSdk[RetrofitManager]";
    private static Retrofit retrofit;
    private static FoxSdkApiService apiService;

    /**
     * 初始化Retrofit
     */
    public static void initialize(FoxSdkConfig config) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.MILLISECONDS)
                .addInterceptor(new FoxSdkHeaderInterceptor(config))
                .addInterceptor(new FoxSdkLoggingInterceptor(config))
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(config.getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(createGson()))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        apiService = retrofit.create(FoxSdkApiService.class);

        if (config.isEnableLog()) {
            Log.d(TAG, "RetrofitManager 初始化完成");
        }
    }

    /**
     * 获取API服务
     */
    public static FoxSdkApiService getApiService() {
        if (apiService == null) {
            throw new IllegalStateException("RetrofitManager 没有初始化");
        }
        return apiService;
    }

    /**
     * 创建Gson实例
     */
    private static Gson createGson() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .setPrettyPrinting()
                .serializeNulls()
                .create();
    }
}
