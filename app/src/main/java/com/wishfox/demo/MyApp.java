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

        initializeWishFoxSdk(
                "https://sdk.example.com/home",
                "https://sdk.example.com"
        );
    }

    /**
     * Demo 调试入口：允许 MainActivity 在运行时替换 H5 首页及其精确 Origin。
     * 正式接入仍应在 Application 启动时使用固定 HTTPS 配置。
     */
    public void initializeWishFoxSdk(String h5HomeUrl, String h5TrustedOrigin) {

        WishFoxSdk.initialize(
                this,
                new FoxSdkConfig.Builder(
                        "1",
                        "1",
                        "billcomwishfoxdemoe"
                )
                        .setBaseUrl("https://test-api-game.wishfoxs.com")
                        .setH5HomeUrl(h5HomeUrl)
                        .setH5TrustedOrigin(h5TrustedOrigin)
//                        .setBaseUrl("https://api-game-pre.wishfoxs.com")
                        .setEnableLog(true)
                        .setWechatTest(false)
                        .setScreenOrientation(FoxSdkConfig.ORIENTATION_LANDSCAPE)
                        .setFloatXScale(0.5f)
                        .setFloatXxOffset(100)
                        .build()
        );
    }
}
