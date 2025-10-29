package com.wishfox.foxsdk.utils;

import android.content.Context;

import com.wishfox.foxsdk.core.WishFoxSdk;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:08
 */
public class FoxSdkCommonExt {

    public static int dp2px(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int dp2px(int dp) {
        return (int) (dp * WishFoxSdk.getContext().getResources().getDisplayMetrics().density + 0.5f);
    }
}
