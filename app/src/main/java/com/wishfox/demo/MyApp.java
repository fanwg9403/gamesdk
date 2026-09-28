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

        initializeWishFoxSdk();
    }

    /**
     * H5 首页不在宿主初始化阶段配置，由登录接口返回并随登录态保存。
     */
    public void initializeWishFoxSdk() {

        WishFoxSdk.initialize(
                this,
                new FoxSdkConfig.Builder(
                        "1",
                        "1",
                        "billcomwishfoxdemoe"
                )
//                        .setBaseUrl("https://test-api-game.wishfoxs.com")
                        // 仅 Demo 调试地址允许 HTTP；正式第三方接入保持默认关闭。
                        .setAllowInsecureH5(true)
                        .setBaseUrl("https://api-game-pre.wishfoxs.com")
                        .setEnableLog(true)
                        .setWechatTest(true)
                        .setScreenOrientation(FoxSdkConfig.ORIENTATION_LANDSCAPE)
                        .setFloatXScale(0.5f)
                        .setFloatXxOffset(100)
                        .setOnUserStateListener(new WishFoxSdk.OnUserStateListener() {
                            @Override
                            public void onLogin(String userId, String token) {
                                // SDK 内部登录成功后回调宿主。
                            }

                            @Override
                            public void onLogout() {
                                // SDK 内部退出登录接口成功后回调宿主。
                            }
                        })
                        .build()
        );
    }
}
