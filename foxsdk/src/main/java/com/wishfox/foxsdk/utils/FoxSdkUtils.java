package com.wishfox.foxsdk.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Pair;

import com.hjq.toast.Toaster;
//import com.petterp.floatingx.FloatingX;
//import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:18
 */
public class FoxSdkUtils {

    public static final String BUGLY_APPID = "0ddd1ab1b4";
    public static final String WISH_FOX_PACKAGE_NAME = "com.sohuglobal.world";
    public static final String FloatXTag = "WishFoxSdk";

    private static long _lastTime = 0L;

    public static String getTestMsg() {
        return "欢迎使用许愿狐SDK！";
    }

    public static void hideFloatX() {
//        FloatingX.configControl(FoxSdkUtils.FloatXTag).setEnableHalfHide(true, WishFoxSdk.getConfig().getFloatXScale());
//        if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null)
//            FloatingX.control(FoxSdkUtils.FloatXTag).getView().setAlpha(0.5f);
//        WishFoxSdk.setFloatActive(false);
    }

    public static Pair<String, String> getFSUInfo() {
        if (FSUserInfo.getInstance() == null) {
            Toaster.show("请先登录");
            return null;
        } else {
            return Pair.create(FSUserInfo.getInstance().getUserId(), FSLoginResult.getTokenEd());
        }
    }

    // RxJava 风格的节流
    public static <T> Observable<T> throttle(Observable<T> source, long delay, TimeUnit unit) {
        return source.throttleFirst(delay, unit);
    }

    // 原有的节流方法
    public static void throttle(long delay, Runnable func) {
        long nowTime = System.currentTimeMillis();
        if (nowTime - _lastTime >= delay) {
            func.run();
            _lastTime = nowTime;
        }
    }

    // 新增：RxJava 风格的节流方法，接受 Runnable
    public static Completable throttle(long delay, TimeUnit unit, Runnable func) {
        return Completable.fromRunnable(() -> {
            long nowTime = System.currentTimeMillis();
            if (nowTime - _lastTime >= unit.toMillis(delay)) {
                func.run();
                _lastTime = nowTime;
            }
        });
    }

    public static Observable<Boolean> copyText(Context context, String text) {
        return Observable.fromCallable(() -> {
            ClipboardManager clipboardManager =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("text", text);
            clipboardManager.setPrimaryClip(clipData);
            return true;
        });
    }

    public static Observable<Boolean> isWishFoxInstalled(Context ctx) {
        return Observable.fromCallable(() -> {
            try {
                PackageManager pm = ctx.getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(WISH_FOX_PACKAGE_NAME);
                return launchIntent != null;
            } catch (Exception e) {
                return false;
            }
        });
    }
}
