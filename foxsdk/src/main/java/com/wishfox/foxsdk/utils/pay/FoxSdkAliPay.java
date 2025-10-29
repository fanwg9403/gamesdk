package com.wishfox.foxsdk.utils.pay;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.R;

import io.reactivex.rxjava3.core.Observable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:05
 */
public class FoxSdkAliPay {

    public static Observable<Boolean> payAliYiMa(Context context, String codeUrl, String jumpUrl) {
        return Observable.fromCallable(() -> {
            boolean installed = isAlipayAvailable(context);
            if (!installed) {
                Toaster.show(context.getString(R.string.fs_str_ali_pay));
                return false;
            } else {
                String finalJumpUrl;
                if (TextUtils.isEmpty(jumpUrl)) {
                    String topic = "alipays://platformapi/startapp?saId=10000007&qrcode=";
                    finalJumpUrl = topic + codeUrl;
                } else {
                    finalJumpUrl = jumpUrl;
                }

                Intent intent = new Intent()
                        .setAction("android.intent.action.VIEW")
                        .setData(Uri.parse(finalJumpUrl));
                context.startActivity(intent);
                return true;
            }
        });
    }

    public static boolean isAlipayAvailable(Context context) {
        return checkAlipayInstallation(context);
    }

    private static boolean checkAlipayInstallation(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.eg.android.AlipayGphone", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
