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
    // 旧版 FloatingX y 轴偏移量（旧调用链按 dp 转 px；当前悬浮球未使用）
    private int floatXxOffset = 100;
    private boolean wechatTest = false;
    /** H5 首页入口；为空时继续使用现有原生首页。 */
    private String h5HomeUrl;
    /** H5 可信 Origin，例如 https://sdk.example.com。 */
    private String h5TrustedOrigin;
    private java.util.List<String> h5MediaOrigins;
    private H5SessionTokenProvider h5SessionTokenProvider;

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
        this.h5HomeUrl = builder.h5HomeUrl;
        this.h5TrustedOrigin = builder.h5TrustedOrigin;
        this.h5MediaOrigins = java.util.Collections.unmodifiableList(
                new java.util.ArrayList<>(builder.h5MediaOrigins));
        this.h5SessionTokenProvider = builder.h5SessionTokenProvider;
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

    public String getH5HomeUrl() {
        return h5HomeUrl;
    }

    public String getH5TrustedOrigin() {
        return h5TrustedOrigin;
    }

    /** 原生媒体下载白名单，与 H5 Bridge Origin 分开配置。 */
    public java.util.List<String> getH5MediaOrigins() { return h5MediaOrigins; }

    /**
     * 获取可选的 H5 短时会话 Token 交换器覆盖实现。
     *
     * <p>交换器只在原生进程内接收长期登录 Token，不能把该 Token 回传给 H5、写入日志
     * 或持久化；成功后只应通过回调返回短时 Token。</p>
     */
    public H5SessionTokenProvider getH5SessionTokenProvider() {
        return h5SessionTokenProvider;
    }

    public boolean isH5Enabled() {
        return h5HomeUrl != null && !h5HomeUrl.trim().isEmpty();
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
        private String h5HomeUrl;
        private String h5TrustedOrigin;
        private java.util.List<String> h5MediaOrigins = new java.util.ArrayList<>();
        private H5SessionTokenProvider h5SessionTokenProvider;

        /**
         * 构造Builder，必需参数
         *
         * @param appId 游戏 ID，由 WishFox 平台分配，不能使用示例值上线
         * @param channelId 渠道 ID，由 WishFox 平台分配（不是客户端生成的密钥）
         * @param kqFusedApplicationScheme 快钱支付宝支付回跳 Scheme，应与宿主清单配置一致
         */
        public Builder(String appId, String channelId,String kqFusedApplicationScheme) {
            this.appId = appId;
            this.channelId = channelId;
            this.kqFusedApplicationScheme = kqFusedApplicationScheme;
        }

        /**
         * 设置接口域名
         *
         * @param baseUrl 原生接口根地址，默认 https://api-game.wishfoxs.com；生产环境使用 HTTPS，
         *                此配置不会自动修改 H5 首页和媒体白名单
         * @return Builder实例
         */
        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * 设置是否开启log日志
         *
         * @param enableLog 是否开启调试日志，默认 false；正式接入应关闭，禁止输出 Token、密码和支付密文
         * @return Builder实例
         */
        public Builder setEnableLog(boolean enableLog) {
            this.enableLog = enableLog;
            return this;
        }

        /**
         * 设置网络请求超时时间
         *
         * @param timeout 超时时间（毫秒），默认 30000；应传正数，不控制 H5 会话交换的 15 秒截止时间
         * @return Builder实例
         */
        public Builder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * 设置屏幕方向
         *
         * @param screenOrientation Android ActivityInfo.SCREEN_ORIENTATION_* 常量，默认
         *                          SCREEN_ORIENTATION_UNSPECIFIED（-1）；此项为既有原生界面配置。
         *                          H5/媒体预览跟随宿主实际方向，不因该值主动旋转宿主。
         *                          横屏传 ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE（0），
         *                          不要使用本类历史 ORIENTATION_LANDSCAPE（2）替代
         * @return Builder实例
         */
        public Builder setScreenOrientation(int screenOrientation) {
            this.screenOrientation = screenOrientation;
            return this;
        }

        /**
         * 设置旧版 FloatingX 半隐藏比例，默认 0.5。
         * <p>当前原生悬浮球流程未读取此字段；保留方法用于二进制兼容，不承诺即时改变新悬浮球。</p>
         * @param floatXScale 旧版半隐藏比例，建议 0～1
         * @return 当前 Builder，支持链式调用
         */
        public Builder setFloatXScale(float floatXScale) {
            this.floatXScale = floatXScale;
            return this;
        }

        /**
         * 设置旧版 FloatingX 的纵向初始偏移，默认 100，旧调用链按 dp 转为 px。
         * <p>历史方法名中的 Xx 不代表横向偏移；当前原生悬浮球流程未读取此字段。</p>
         * @param floatXxOffset 旧版纵向偏移（dp）
         * @return 当前 Builder，支持链式调用
         */
        public Builder setFloatXxOffset(int floatXxOffset) {
            this.floatXxOffset = floatXxOffset;
            return this;
        }

        /**
         * 设置既有支付流程选择的微信小程序版本，不切换所有接口的服务器环境。
         * @param wechatTest true 使用 trial（体验版），false 使用 release（正式版，默认）
         * @return 当前 Builder，支持链式调用
         */
        public Builder setWechatTest(boolean wechatTest) {
            this.wechatTest = wechatTest;
            return this;
        }

        /**
         * 设置 H5 首页入口；不修改现有原生弹窗和支付模块。
         * @param h5HomeUrl 可信 HTTPS 绝对 URL；null/空白关闭 H5 首页，回退原生首页；
         *                  非空时必须与 H5 可信 Origin 一致，不允许附带登录 Token
         * @return 当前 Builder，支持链式调用
         */
        public Builder setH5HomeUrl(String h5HomeUrl) {
            this.h5HomeUrl = h5HomeUrl;
            return this;
        }

        /**
         * 设置拥有 JS Bridge 权限、允许内部路由的 H5 来源。
         * @param h5TrustedOrigin HTTPS Origin（协议+域名+可选端口），例如 https://h5.example.com；
         *                        不支持通配符，不配置时取首页 Origin，不能将 CDN 作为可信业务来源
         * @return 当前 Builder，支持链式调用
         */
        public Builder setH5TrustedOrigin(String h5TrustedOrigin) {
            this.h5TrustedOrigin = h5TrustedOrigin;
            return this;
        }

        /**
         * 设置图片下载及视频在线缓冲的 HTTPS 来源白名单，不授予媒体服务器 JS Bridge 权限。
         * @param origins 允许的 HTTPS Origin，精确匹配域名和端口；传 null/空数组时回退 H5 可信 Origin；
         *                每次调用替换旧列表，不是追加。示例：https://cdn.example.com
         * @return 当前 Builder，支持链式调用
         * @throws IllegalArgumentException 任一来源不是合法 HTTPS 地址时抛出
         */
        public Builder setH5MediaOrigins(String... origins) {
            java.util.ArrayList<String> checked = new java.util.ArrayList<>();
            if (origins != null) for (String origin : origins) {
                checked.add(com.wishfox.foxsdk.media.FSMediaPolicy.origin(origin));
            }
            this.h5MediaOrigins = checked;
            return this;
        }

        /**
         * 设置 H5 短时会话 Token 交换器覆盖实现。
         *
         * <p>H5 调用 auth.login 或 auth.refreshSession 时，SDK 会在原生侧使用已登录的长期
         * Token 调用该交换器。回调中的 shortToken 必须是绑定 appId、channelId、用户和
         * H5 sessionId 的短时凭证，SDK 不会将长期 Token 暴露给 H5。</p>
         *
         * @param provider 自定义会话交换实现；传 null 使用 SDK 内置 getShortLogin 接口
         * @return Builder 实例
         */
        public Builder setH5SessionTokenProvider(H5SessionTokenProvider provider) {
            this.h5SessionTokenProvider = provider;
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
     * H5 短时会话 Token 交换接口。实现方应在自己的安全网络层完成长期 Token 到短时
     * Token 的交换。SDK 在 IO 线程调用 exchange，回调可在任意线程触发。
     * SDK 15 秒超时后丢弃迟到结果；交换器仍应设置自身网络超时并释放网络资源，
     * 不要持有 Activity，不得记录请求/响应凭证；SDK 不会自动中断交换器内部的异步网络请求。
     */
    public interface H5SessionTokenProvider {
        /**
         * @param nativeToken SDK 本地登录长期 Token，仅允许在原生进程内使用
         * @param sessionId 当前 H5 Bridge sessionId
         * @param callback 交换结果回调
         */
        void exchange(String nativeToken, String sessionId, Callback callback);

        interface Callback {
            /**
             * 交换成功；同一次请求只能回调一次，重复结果会被 SDK 丢弃。
             * @param shortToken 返回给 H5 的非空短时 Token，最长 8192 字符，不能等于原生长期 Token
             * @param expiresInSeconds 从本次响应开始计算的剩余有效期（秒），可选；原生不依赖该值判断短 Token 是否有效
             */
            void onSuccess(String shortToken, long expiresInSeconds);

            /**
             * @param code NETWORK_ERROR、RATE_LIMITED、SESSION_EXCHANGE_FAILED，或 AUTH_REQUIRED
             *             （仅后端明确确认长期凭证已失效时使用，会清除原生登录态）；
             *             未知值统一转换为 SESSION_EXCHANGE_FAILED，不透传服务器异常文本
             */
            void onFailure(String code);
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
