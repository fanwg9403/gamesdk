package com.wishfox.foxsdk.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.hjq.toast.Toaster;

import io.reactivex.rxjava3.core.Observable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:08
 */
public class FoxSdkAppJumpUtils {

    public static void launchByDeepLink(Context context, String deepLink, String h5Url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            byWebOpenH5(context, h5Url);
            Log.e("AppJump", "Error launching by deep link: " + e.getMessage());
        }
    }

    public static void byWebOpenH5(Context context, String url) {
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } else {
            Toaster.show("网址无法跳转");
        }
    }
}
