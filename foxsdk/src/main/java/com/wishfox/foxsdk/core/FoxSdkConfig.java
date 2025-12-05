package com.wishfox.foxsdk.core;

import android.content.pm.ActivityInfo;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:14
 */
public class FoxSdkConfig {
    // 游戏id
    private String appId;
    // 游戏key
    private String channelId;
    // 接口域名
    private String baseUrl = "https://api-game.wishfoxs.com";
    // 是否开启log日志
    private boolean enableLog = false;
    // 网络请求超时时间
    private long timeout = 30000L;
    // 屏幕方向
    private int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    // 悬浮球收起隐藏的比例
    private float floatXScale = 0.5f;
    // 悬浮球y轴偏移量，单位px
    private int floatXxOffset = 100;
    private boolean wechatTest = false;

    //快钱支付宝支付配置
    private String kqFusedApplicationScheme;

    // 方向常量
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;

    // 请求码
    public static final int WISH_FOX_REQUEST_CODE = 0x0278887;

    // 包名和Activity类名
    public static final String WISH_FOX_PACKAGE_NAME = "com.wishfox.foxsdk";
    public static final String WISH_FOX_AUTH_LOGIN_ACTIVITY = "com.wishfox.foxsdk.auth.AuthLoginActivity";

    // 私有构造函数，使用Builder模式
    private FoxSdkConfig(Builder builder) {
        this.appId = builder.appId;
        this.channelId = builder.channelId;
        this.baseUrl = builder.baseUrl;
        this.enableLog = builder.enableLog;
        this.timeout = builder.timeout;
        this.screenOrientation = builder.screenOrientation;
        this.floatXScale = builder.floatXScale;
        this.floatXxOffset = builder.floatXxOffset;
        this.wechatTest = builder.wechatTest;
        this.kqFusedApplicationScheme = builder.kqFusedApplicationScheme;
    }

    // Getters
    public String getAppId() {
        return appId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isEnableLog() {
        return enableLog;
    }

    public long getTimeout() {
        return timeout;
    }

    public int getScreenOrientation() {
        return screenOrientation;
    }

    public float getFloatXScale() {
        return floatXScale;
    }

    public int getFloatXxOffset() {
        return floatXxOffset;
    }
    public String getKqFusedApplicationScheme() {
        return kqFusedApplicationScheme;
    }

    public boolean isWechatTest() {
        return wechatTest;
    }

    /**
     * Builder模式用于创建FoxSdkConfig实例
     */
    public static class Builder {
        // 必需参数
        private final String appId;
        private final String channelId;

        // 可选参数 - 使用默认值
        private String baseUrl = "https://api-game.wishfoxs.com";
        private boolean enableLog = false;
        private long timeout = 30000L;
        private int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        private float floatXScale = 0.5f;
        private int floatXxOffset = 100;
        private String kqFusedApplicationScheme;
        private boolean wechatTest = false;

        /**
         * 构造Builder，必需参数
         *
         * @param appId     游戏id
         * @param channelId 游戏key
         * @param kqFusedApplicationScheme  支付宝支付配置
         */
        public Builder(String appId, String channelId,String kqFusedApplicationScheme) {
            this.appId = appId;
            this.channelId = channelId;
            this.kqFusedApplicationScheme = kqFusedApplicationScheme;
        }

        /**
         * 设置接口域名
         *
         * @param baseUrl 接口域名
         * @return Builder实例
         */
        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * 设置是否开启log日志
         *
         * @param enableLog 是否开启log
         * @return Builder实例
         */
        public Builder setEnableLog(boolean enableLog) {
            this.enableLog = enableLog;
            return this;
        }

        /**
         * 设置网络请求超时时间
         *
         * @param timeout 超时时间（毫秒）
         * @return Builder实例
         */
        public Builder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * 设置屏幕方向
         *
         * @param screenOrientation 屏幕方向
         * @return Builder实例
         */
        public Builder setScreenOrientation(int screenOrientation) {
            this.screenOrientation = screenOrientation;
            return this;
        }

        public Builder setFloatXScale(float floatXScale) {
            this.floatXScale = floatXScale;
            return this;
        }

        public Builder setFloatXxOffset(int floatXxOffset) {
            this.floatXxOffset = floatXxOffset;
            return this;
        }

        public Builder setWechatTest(boolean wechatTest) {
            this.wechatTest = wechatTest;
            return this;
        }

        /**
         * 构建FoxSdkConfig实例
         *
         * @return FoxSdkConfig实例
         */
        public FoxSdkConfig build() {
            return new FoxSdkConfig(this);
        }
    }

    /**
     * 动作常量类
     */
    public static class WishFoxActions {
        private WishFoxActions() {
            // 防止实例化
        }

        public static final String WISH_FOX_AUTH_ACTION = "com.wishfox.foxsdk.AUTH_LOGIN";
        public static final String WISH_FOX_AUTH_RESULT_ACTION = "com.wishfox.foxsdk.AUTH_LOGIN_RESULT";
    }

    @Override
    public String toString() {
        return "FoxSdkConfig{" +
                "appId='" + appId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", enableLog=" + enableLog +
                ", timeout=" + timeout +
                ", screenOrientation=" + screenOrientation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FoxSdkConfig that = (FoxSdkConfig) o;

        if (enableLog != that.enableLog) return false;
        if (timeout != that.timeout) return false;
        if (screenOrientation != that.screenOrientation) return false;
        if (!appId.equals(that.appId)) return false;
        if (!channelId.equals(that.channelId)) return false;
        if (!kqFusedApplicationScheme.equals(that.kqFusedApplicationScheme)) return false;
        return baseUrl.equals(that.baseUrl);
    }

    @Override
    public int hashCode() {
        int result = appId.hashCode();
        result = 31 * result + channelId.hashCode();
        result = 31 * result + baseUrl.hashCode();
        result = 31 * result + (enableLog ? 1 : 0);
        result = 31 * result + (int) (timeout ^ (timeout >>> 32));
        result = 31 * result + screenOrientation;
        return result;
    }
}
