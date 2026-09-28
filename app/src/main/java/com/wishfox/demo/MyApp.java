package com.wishfox.demo;

import android.app.Application;
import android.util.Log;

import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.data.model.entity.FSUserProfile;
import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.utils.FoxSdkConstant;
import com.wishfox.foxsdk.utils.FoxSdkSPUtils;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月29日 9:50
 */
public class MyApp extends Application {

    public enum Environment {
        TEST("测试", "https://test-api-game.wishfoxs.com", true),
        PRE("预发", "https://api-game-pre.wishfoxs.com", true),
        PRODUCTION("正式", "https://api-game.wishfoxs.com", false);

        final String label;
        final String baseUrl;
        final boolean testMode;

        Environment(String label, String baseUrl, boolean testMode) {
            this.label = label;
            this.baseUrl = baseUrl;
            this.testMode = testMode;
        }
    }

    private static final String ENV_PREFERENCES = "wishfox_demo_environment";
    private static final String KEY_ENVIRONMENT = "environment";

    @Override
    public void onCreate() {
        super.onCreate();

        initializeWishFoxSdk();
    }

    /**
     * H5 首页不在宿主初始化阶段配置，由登录接口返回并随登录态保存。
     */
    public void initializeWishFoxSdk() {
        Environment environment = getEnvironment();

        WishFoxSdk.initialize(
                this,
                new FoxSdkConfig.Builder(
                        "1",
                        "1",
                        "billcomwishfoxdemoe"
                )
                        .setBaseUrl(environment.baseUrl)
                        // 测试/预发允许调试 H5，并使用微信体验版；正式环境关闭这两项。
                        .setAllowInsecureH5(environment.testMode)
                        .setEnableLog(true)
                        .setWechatTest(environment.testMode)
                        .setScreenOrientation(FoxSdkConfig.ORIENTATION_LANDSCAPE)
                        .setFloatXScale(0.5f)
                        .setFloatXxOffset(100)
                        .setOnUserStateListener(new WishFoxSdk.OnUserStateListener() {
                            @Override
                            public void onLogin(String userId, String token) {
                                // SDK 内部登录成功后回调宿主。
                                Log.i("FoxSdk[status]", "onLogin: " + userId + ", token: " + token);
                            }

                            @Override
                            public void onLogout() {
                                // SDK 内部退出登录接口成功后回调宿主。
                                Log.i("FoxSdk[status]", "onLogout");
                            }
                        })
                        .build()
        );
    }

    public Environment getEnvironment() {
        String name = getSharedPreferences(ENV_PREFERENCES, MODE_PRIVATE)
                .getString(KEY_ENVIRONMENT, Environment.PRODUCTION.name());
        try {
            return Environment.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return Environment.PRODUCTION;
        }
    }

    /** Demo 运行时切换 API 环境，并丢弃其他环境保存的登录凭证。 */
    public void switchEnvironment(Environment environment) {
        if (environment == null || environment == getEnvironment()) return;
        getSharedPreferences(ENV_PREFERENCES, MODE_PRIVATE).edit()
                .putString(KEY_ENVIRONMENT, environment.name())
                .apply();

        // Token 与账号数据按环境隔离，切换后要求在目标环境重新登录。
        FSLoginResult.clear();
        FSUserProfile.clear();
        FSCoinInfo.clear();
        FoxSdkSPUtils.getInstance().remove(FoxSdkConstant.AUTHORIZATION);

        initializeWishFoxSdk();
    }
}
