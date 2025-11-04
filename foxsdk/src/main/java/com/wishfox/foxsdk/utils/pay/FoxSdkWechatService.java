package com.wishfox.foxsdk.utils.pay;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.wishfox.foxsdk.core.WishFoxSdk;

import io.reactivex.rxjava3.core.Observable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:06
 */
public class FoxSdkWechatService {
    private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;

    private static IWXAPI iwxapi;
    private static BroadcastReceiver wechatRefreshReceiver;
    private static boolean isInitialized = false;

    public static void init(Context context) {
        if (isInitialized)
            return;

        iwxapi = WXAPIFactory.createWXAPI(context, TextUtils.isEmpty(WishFoxSdk.getConfig().getWechatAppId()) ? FoxSdkPayConfig.APP_ID : WishFoxSdk.getConfig().getWechatAppId(), true);
        iwxapi.registerApp(TextUtils.isEmpty(WishFoxSdk.getConfig().getWechatAppId()) ? FoxSdkPayConfig.APP_ID : WishFoxSdk.getConfig().getWechatAppId());

        IntentFilter filter = new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                iwxapi.registerApp(TextUtils.isEmpty(WishFoxSdk.getConfig().getWechatAppId())?FoxSdkPayConfig.APP_ID:WishFoxSdk.getConfig().getWechatAppId());
            }
        };

        context.registerReceiver(receiver, filter);

        isInitialized = true;
    }

    /**
     * 注册微信刷新广播接收器（兼容Android 14+）
     */
    @SuppressLint("WrongConstant")
    private static void registerWechatRefreshReceiver(Context context) {
        if (wechatRefreshReceiver != null) {
            return; // 避免重复注册
        }

        wechatRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String appId = TextUtils.isEmpty(WishFoxSdk.getConfig().getWechatAppId())
                        ? FoxSdkPayConfig.APP_ID
                        : WishFoxSdk.getConfig().getWechatAppId();
                if (iwxapi != null) {
                    iwxapi.registerApp(appId);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP);

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                // Android 14+ 必须指定导出行为
                // 微信广播是系统级广播，使用 RECEIVER_EXPORTED
                context.registerReceiver(wechatRefreshReceiver, filter, 0x2);
            } else {
                // Android 13 及以下
                context.registerReceiver(wechatRefreshReceiver, filter);
            }
        } catch (Exception e) {
            // 捕获可能的注册异常，避免崩溃
            e.printStackTrace();
        }
    }

    /**
     * 释放资源，注销广播接收器
     */
    public static void release(Context context) {
        if (wechatRefreshReceiver != null) {
            try {
                context.unregisterReceiver(wechatRefreshReceiver);
            } catch (IllegalArgumentException e) {
                // 接收器可能未注册，忽略异常
            }
            wechatRefreshReceiver = null;
        }

        if (iwxapi != null) {
            iwxapi.unregisterApp();
            iwxapi = null;
        }

        isInitialized = false;
    }

    /**
     * 重新初始化（用于配置变更等情况）
     */
    public static void reinit(Context context) {
        release(context);
        init(context);
    }

    public static Observable<Boolean> isWeChatInstalled() {
        return Observable.fromCallable(() -> {
            if (iwxapi != null) {
                return iwxapi.isWXAppInstalled();
            }
            return false;
        });
    }

    public static Observable<Boolean> isWeChatTimelineSupported() {
        return Observable.fromCallable(() -> {
            if (iwxapi != null) {
                return iwxapi.getWXAppSupportAPI() >= TIMELINE_SUPPORTED_VERSION;
            }
            return false;
        });
    }

    public static IWXAPI getIwxapi() {
        return iwxapi;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 安全的微信API调用封装
     */
    public static boolean sendReq(Context context, Object req) {
        if (!isInitialized) {
            init(context.getApplicationContext());
        }

        if (iwxapi != null) {
            try {
                // 使用反射调用sendReq方法，避免直接依赖
                return (boolean) iwxapi.getClass()
                        .getMethod("sendReq", req.getClass())
                        .invoke(iwxapi, req);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
