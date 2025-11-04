package com.wishfox.foxsdk.utils.pay;

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

    public static void init(Context context) {
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
}
