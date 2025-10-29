package com.wishfox.foxsdk.utils.pay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

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
        iwxapi = WXAPIFactory.createWXAPI(context, FoxSdkPayConfig.APP_ID, true);
        iwxapi.registerApp(FoxSdkPayConfig.APP_ID);

        IntentFilter filter = new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                iwxapi.registerApp(FoxSdkPayConfig.APP_ID);
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
