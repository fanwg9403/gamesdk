package com.wishfox.demo;

import android.app.Application;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.core.WishFoxSdk;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月29日 9:50
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        WishFoxSdk.initialize(
                this,
                new FoxSdkConfig.Builder(
                        "1",
                        "1",
                        "billcomwishfoxdemoe"
                )
//                        .setBaseUrl("http://192.168.150.240:9113")
                        .setEnableLog(true)
                        .setWechatTest(true)
                        .setScreenOrientation(FoxSdkConfig.ORIENTATION_LANDSCAPE)
                        .setFloatXScale(0.6f)
                        .setFloatXxOffset(100)
                        .build()
        );
    }
}
