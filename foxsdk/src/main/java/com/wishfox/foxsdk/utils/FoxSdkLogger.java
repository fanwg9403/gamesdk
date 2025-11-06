package com.wishfox.foxsdk.utils;

import android.util.Log;

import com.wishfox.foxsdk.BuildConfig;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年11月06日 11:59
 */
public class FoxSdkLogger {
    private static boolean isDebug = BuildConfig.DEBUG;

    public static void setDebug(boolean debug) {
        isDebug = debug;
    }

    public static void d(String tag, String message) {
        if (isDebug) {
            Log.d(tag, message);
        }
    }

    public static void i(String tag, String message) {
        if (isDebug) {
            Log.i(tag, message);
        }
    }

    public static void w(String tag, String message) {
        if (isDebug) {
            Log.w(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (isDebug) {
            Log.e(tag, message);
        }
    }

    public static void e(String tag, String message, Throwable e) {
        if (isDebug) {
            Log.e(tag, message, e);
        }
    }
}
