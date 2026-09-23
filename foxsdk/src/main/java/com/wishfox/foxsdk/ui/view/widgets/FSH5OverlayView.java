package com.wishfox.foxsdk.ui.view.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.Toast;
import android.util.DisplayMetrics;
import com.wishfox.foxsdk.media.FSMediaPolicy;
import com.wishfox.foxsdk.media.FSMediaSaveCoordinator;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.utils.FoxSdkLogger;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.Nullable;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.auth.FSH5AuthSession;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;

/**
 * H5 业务页面的 Overlay 容器。
 *
 * <p>H5 按普通页面/SPA 路由开发。内部链接不使用 target=_blank，
 * 由本容器根据真实屏幕方向创建或复用 Secondary WebView。</p>
 */
public final class FSH5OverlayView extends FrameLayout {
    private static final String BRIDGE_LOG_TAG = "FoxSdk[H5]";
    private static final int PORTRAIT_TOP_SAFE_AREA_EXTRA_CSS_PX = 4;

    public interface Callback {
        void onClose();

        /**
         * H5 请求退出登录。原生负责二次确认、清理登录态并在成功后关闭整个 H5 Overlay。
         * Completion 必须只回调一次；取消确认时回传 USER_CANCELLED。
         */
        void onLogoutRequested(LogoutCompletion completion);

        /**
         * H5 请求打开微信小程序。请求参数交给 OverlayManager，由原生负责耗时的 Scheme
         * 获取、外部 App 拉起以及最终 completion 回传。
         */
        void onMiniProgramRequested(
                String requestId,
                String appName,
                JSONObject params,
                MiniProgramCompletion completion
        );
    }

    public interface LogoutCompletion {
        void complete(String code, JSONObject data);
    }

    public interface MiniProgramCompletion {
        void complete(String code, JSONObject data);
    }

    private final Activity activity;
    private final Callback callback;
    private final String trustedOrigin;
    private final boolean allowInsecureH5;
    private WebView primary;
    private WebView secondary;
    private String secondaryUrl;
    private View initialLoadingView;
    private boolean initialPageFinished;
    private boolean initialBridgeReady;
    private boolean initialVisualStateReady = android.os.Build.VERSION.SDK_INT < 23;
    private boolean initialVisualStateRequested;
    private boolean initialRevealPending = true;
    private boolean destroyed;
    private final Map<WebView, NavigationBridge> bridges = new HashMap<>();
    private final String sessionId = UUID.randomUUID().toString();
    private FSMediaPreviewView preview;
    private String previewId;
    private NavigationBridge previewOwner;
    private boolean hostResumed = true;
    private final FSMediaSaveCoordinator mediaSaveCoordinator;

    public FSH5OverlayView(Activity activity, Callback callback, String homeUrl) {
        super(activity);
        this.activity = activity;
        this.callback = callback;
        this.mediaSaveCoordinator = new FSMediaSaveCoordinator(activity);
        FoxSdkConfig config = com.wishfox.foxsdk.core.WishFoxSdk.getConfig();
        this.allowInsecureH5 = config.isAllowInsecureH5();
        this.trustedOrigin = h5Origin(TextUtils.isEmpty(config.getH5TrustedOrigin())
                ? homeUrl : config.getH5TrustedOrigin());
        FoxSdkLogger.d(BRIDGE_LOG_TAG, "overlay create: homeOrigin=" + h5Origin(homeUrl)
                + ", trustedOrigin=" + trustedOrigin
                + ", allowInsecureH5=" + allowInsecureH5);
        if (trustedOrigin == null) throw new IllegalArgumentException("Invalid H5 trusted origin");
        if (!isTrusted(homeUrl)) throw new IllegalArgumentException("Untrusted H5 home URL");
        setClickable(true);
        setFocusable(true);
        // 外层覆盖宿主窗口但保持透明；loading 和 WebView 都只占实际 H5 面板区域。
        setBackgroundColor(Color.TRANSPARENT);
        // 最外层仍覆盖宿主窗口，但 WebView 只占首页面板区域；面板外的透明区域点击关闭 H5。
        setOnClickListener(view -> {
            if (!destroyed && callback != null) callback.onClose();
        });
        // H5 容器铺满宿主窗口，安全区通过 bridge.ready/environment.changed 交给 H5 应用。
        // 不能再在原生容器上叠加 padding，否则会与 H5 CSS 产生双重留白。
        setPadding(0, 0, 0, 0);
        primary = createWebView();
        // 保持 WebView 可见性状态，确保 JS/WebView 正常初始化；通过 alpha 隐藏首帧内容。
        primary.setAlpha(0f);
        addView(primary, primaryParams());
        initialLoadingView = createInitialLoadingView();
        addView(initialLoadingView, primaryParams());
        primary.loadUrl(homeUrl);
    }

    private View createInitialLoadingView() {
        FrameLayout overlay = new FrameLayout(activity);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setBackgroundColor(initialLoadingBackground());

        // 复用项目已有的 loading 卡片、圆角和白色进度条，避免 H5 首页使用另一套样式。
        View loadingCard = LayoutInflater.from(activity)
                .inflate(R.layout.fs_dialog_loading, overlay, false);
        TextView message = loadingCard.findViewById(R.id.fs_tv_message);
        if (message != null) {
            message.setVisibility(VISIBLE);
            message.setText("加载中...");
        }
        LayoutParams cardParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        overlay.addView(loadingCard, cardParams);
        return overlay;
    }

    private int initialLoadingBackground() {
        return getResources().getColor(R.color.fs_activity_bg);
    }

    private void maybeRevealInitialContent() {
        if (!initialRevealPending || !initialPageFinished || !initialBridgeReady || destroyed) return;
        if (!initialVisualStateRequested) {
            initialVisualStateRequested = true;
            if (android.os.Build.VERSION.SDK_INT >= 23 && primary != null) {
                primary.postVisualStateCallback(SystemClock.uptimeMillis(),
                        new WebView.VisualStateCallback() {
                            @Override
                            public void onComplete(long requestId) {
                                initialVisualStateReady = true;
                                maybeRevealInitialContent();
                            }
                        });
            } else {
                initialVisualStateReady = true;
            }
        }
        if (!initialVisualStateReady) return;
        // 等待一帧，让 H5 在收到 ready 响应后完成首轮布局，再一次性显示 WebView。
        postDelayed(() -> {
            if (!initialRevealPending || destroyed || primary == null) return;
            initialRevealPending = false;
            setBackgroundColor(Color.TRANSPARENT);
            primary.animate().alpha(1f).setDuration(180L).start();
            if (initialLoadingView != null) {
                initialLoadingView.animate().alpha(0f).setDuration(180L).withEndAction(() -> {
                    if (initialLoadingView == null) return;
                    initialLoadingView.setVisibility(GONE);
                    initialLoadingView.setAlpha(1f);
                }).start();
            }
            FoxSdkLogger.d(BRIDGE_LOG_TAG, "initial content revealed after page and bridge ready");
        }, 32L);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        androidx.core.view.ViewCompat.requestApplyInsets(this);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private WebView createWebView() {
        WebView view = new WebView(activity);
        view.setBackgroundColor(initialLoadingBackground());
        view.setOverScrollMode(OVER_SCROLL_NEVER);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setDomStorageEnabled(true);
        view.getSettings().setSupportZoom(false);
        view.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        view.getSettings().setSupportMultipleWindows(false);
        view.getSettings().setAllowFileAccess(false);
        view.getSettings().setAllowContentAccess(false);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            view.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        NavigationBridge bridge = new NavigationBridge(view);
        bridges.put(view, bridge);
        view.addJavascriptInterface(bridge, "WishFoxNative");
        view.setWebViewClient(new RoutingClient(view));
        return view;
    }

    private LayoutParams primaryParams() {
        return primaryParams(getWidth());
    }

    private LayoutParams primaryParams(int width) {
        if (isLandscape()) {
            LayoutParams params = new LayoutParams(landscapePrimaryWidth(width), LayoutParams.MATCH_PARENT, Gravity.START);
            return params;
        }
        return new LayoutParams(portraitPanelWidth(width), LayoutParams.MATCH_PARENT, Gravity.START);
    }

    private LayoutParams secondaryParams() {
        return secondaryParams(getWidth());
    }

    private LayoutParams secondaryParams(int width) {
        if (isLandscape()) {
            LayoutParams params = new LayoutParams(landscapeSecondaryWidth(width), LayoutParams.MATCH_PARENT, Gravity.END);
            return params;
        }
        return new LayoutParams(portraitPanelWidth(width), LayoutParams.MATCH_PARENT, Gravity.START);
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }

    private int landscapePrimaryWidth() {
        return landscapePrimaryWidth(getWidth());
    }

    private int landscapePrimaryWidth(int width) {
        return width > 0 ? Math.round(width * (73f / 163f)) : LayoutParams.MATCH_PARENT;
    }

    private int landscapeSecondaryWidth() {
        return landscapeSecondaryWidth(getWidth());
    }

    private int landscapeSecondaryWidth(int width) {
        return width > 0 ? Math.round(width * (90f / 163f)) : LayoutParams.MATCH_PARENT;
    }

    private int portraitPanelWidth() {
        return portraitPanelWidth(getWidth());
    }

    private int portraitPanelWidth(int width) {
        return width > 0 ? Math.round(width * (4f / 5f)) : LayoutParams.MATCH_PARENT;
    }

    private void updateWebViewPanelParams(int width) {
        if (width <= 0) return;
        if (primary != null) applyPanelParams(primary, primaryParams(width));
        if (secondary != null) applyPanelParams(secondary, secondaryParams(width));
        if (initialLoadingView != null && initialRevealPending) {
            applyPanelParams(initialLoadingView, primaryParams(width));
        }
    }

    private static void applyPanelParams(View child, LayoutParams expected) {
        ViewGroup.LayoutParams current = child.getLayoutParams();
        if (current instanceof LayoutParams
                && current.width == expected.width
                && current.height == expected.height
                && ((LayoutParams) current).gravity == expected.gravity) {
            return;
        }
        child.setLayoutParams(expected);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        updateWebViewPanelParams(width);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private boolean isTrusted(String url) {
        if (TextUtils.isEmpty(url)) return false;
        return trustedOrigin.equals(h5Origin(url));
    }

    /** 允许 HTTP/HTTPS，但仍要求与配置的可信 Origin 精确匹配。 */
    private String h5Origin(String value) {
        if (TextUtils.isEmpty(value) || value.length() > 4096) return null;
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || uri.getUserInfo() != null) return null;
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !(allowInsecureH5 && "http".equals(scheme))) return null;
        int port = uri.getPort();
        if (port == 0 || port > 65535) return null;
        boolean defaultPort = port == -1
                || ("https".equals(scheme) && port == 443)
                || ("http".equals(scheme) && port == 80);
        return scheme + "://" + host.toLowerCase(Locale.ROOT)
                + (defaultPort ? "" : ":" + port);
    }

    private String resolveUrl(String value) {
        if (TextUtils.isEmpty(value)) return null;
        if (value.startsWith("/")) {
            return trustedOrigin == null ? null : trustedOrigin + value;
        }
        return value;
    }

    /** 路由内部 H5 链接到方向对应的 Secondary WebView。 */
    public void openInternal(String value) {
        openInternalResult(value);
    }

    /**
     * 打开内部 H5 链接，可按 H5 请求把当前原生用户 ID 追加为查询参数。
     */
    public void openInternal(String value, boolean needAppendUserId, String userIdKey) {
        openInternalResult(value, needAppendUserId, userIdKey, true);
    }

    private JSONObject openInternalResult(String value) {
        return openInternalResult(value, false, null, false);
    }

    private JSONObject openInternalResult(
            String value,
            boolean needAppendUserId,
            String userIdKey,
            boolean appendGameParams
    ) {
        if (destroyed) return null;
        String previousMode = layoutMode();
        final String resolvedUrl = resolveUrl(value);
        final String url = appendNativeInternalParams(
                resolvedUrl, needAppendUserId, userIdKey, appendGameParams);
        if (!isTrusted(url)) return null;
        boolean created = false;
        if (secondary == null) {
            secondary = createWebView();
            secondaryUrl = url;
            addView(secondary, secondaryParams());
            created = true;
        } else {
            secondaryUrl = url;
        }
        secondary.loadUrl(url);
        secondary.bringToFront();
        if (!isLandscape()) secondary.bringToFront();
        if (initialRevealPending && initialLoadingView != null) initialLoadingView.bringToFront();
        if (!previousMode.equals(layoutMode())) {
            sendEnvironmentChanged();
            sendLayoutChanged(previousMode, "navigation");
        }
        try {
            return new JSONObject().put("requestedUrl", value)
                    .put("actualUrl", url)
                    .put("needAppendUserId", needAppendUserId)
                    .put("userIdAppended", hasNativeUserId(url, needAppendUserId, userIdKey))
                    .put("gameParamsAppended", appendGameParams && hasNativeGameParams(url))
                    .put("webViewId", "secondary")
                    .put("layoutMode", isLandscape() ? "split" : "stacked")
                    .put("created", created);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private String appendNativeInternalParams(
            String url,
            boolean needAppendUserId,
            String userIdKey,
            boolean appendGameParams
    ) {
        if (TextUtils.isEmpty(url)) return url;
        if (!appendGameParams && !needAppendUserId) return url;
        try {
            Uri source = Uri.parse(url);
            Uri.Builder builder = source.buildUpon().clearQuery();

            // 保留 H5 原有查询参数，但移除由原生负责的固定参数，防止 H5 伪造或产生重复值。
            for (String key : source.getQueryParameterNames()) {
                if ((appendGameParams && ("gameId".equals(key) || "gameChannel".equals(key)))
                        || (needAppendUserId && TextUtils.equals(key, userIdKey))) {
                    continue;
                }
                for (String value : source.getQueryParameters(key)) {
                    builder.appendQueryParameter(key, value);
                }
            }

            if (appendGameParams) {
                FoxSdkConfig config = com.wishfox.foxsdk.core.WishFoxSdk.getConfig();
                String appId = config.getAppId();
                String channelId = config.getChannelId();
                builder.appendQueryParameter("gameId", appId == null ? "" : appId);
                builder.appendQueryParameter("gameChannel", channelId == null ? "" : channelId);
            }

            if (needAppendUserId && !TextUtils.isEmpty(userIdKey)
                    && !isReservedInternalParam(userIdKey)) {
                FSUserInfo userInfo = FSUserInfo.getInstance();
                String userId = userInfo == null ? null : userInfo.getUserId();
                if (!TextUtils.isEmpty(userId)) builder.appendQueryParameter(userIdKey, userId);
            }
            return builder.build().toString();
        } catch (RuntimeException failure) {
            FoxSdkLogger.w(BRIDGE_LOG_TAG, "openInternal native params append failed: invalid url/key");
            return url;
        }
    }

    private boolean hasNativeUserId(String url, boolean needAppendUserId, String userIdKey) {
        if (!needAppendUserId || TextUtils.isEmpty(url) || TextUtils.isEmpty(userIdKey)
                || isReservedInternalParam(userIdKey)) return false;
        FSUserInfo userInfo = FSUserInfo.getInstance();
        String userId = userInfo == null ? null : userInfo.getUserId();
        if (TextUtils.isEmpty(userId)) return false;
        try {
            return TextUtils.equals(userId, Uri.parse(url).getQueryParameter(userIdKey));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean hasNativeGameParams(String url) {
        if (TextUtils.isEmpty(url)) return false;
        try {
            FoxSdkConfig config = com.wishfox.foxsdk.core.WishFoxSdk.getConfig();
            Uri uri = Uri.parse(url);
            return TextUtils.equals(config.getAppId(), uri.getQueryParameter("gameId"))
                    && TextUtils.equals(config.getChannelId(), uri.getQueryParameter("gameChannel"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isReservedInternalParam(String key) {
        return "gameId".equals(key) || "gameChannel".equals(key);
    }

    public void closeSecondary() {
        if (secondary == null) return;
        removeView(secondary);
        destroyWebView(secondary);
        secondary = null;
        secondaryUrl = null;
        if (primary != null) primary.bringToFront();
        if (!destroyed) sendEnvironmentChanged();
    }

    private String layoutMode() {
        return secondary == null ? "single" : (isLandscape() ? "split" : "stacked");
    }

    private JSONObject environment(String webViewId) throws JSONException {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int widthPx = getWidth() > 0 ? getWidth() : metrics.widthPixels;
        int heightPx = getHeight() > 0 ? getHeight() : metrics.heightPixels;
        float density = metrics.density <= 0 ? 1f : metrics.density;
        float fontScale = getResources().getConfiguration().fontScale;
        FSOverlayInsets.Snapshot snapshot = FSOverlayInsets.snapshot(activity, this);
        // WindowInsets 使用 Android 物理像素；H5 CSS 使用 CSS px。按 WebView
        // devicePixelRatio 转换，避免高密度设备把物理 107px 当成 CSS 107px。
        // 横屏时系统导航栏可能位于左/右侧，但 H5 面板本身并不应因导航栏宽度
        // 被额外压缩；只保留挖孔屏 cutout 的横向安全区。
        int leftPhysical = isLandscape() ? snapshot.cutoutLeft : snapshot.left;
        int rightPhysical = isLandscape() ? snapshot.cutoutRight : snapshot.right;
        int safeLeft = toCssPx(leftPhysical, density);
        int safeTop = toCssPx(snapshot.top, density);
        int safeRight = toCssPx(rightPhysical, density);
        int safeBottom = toCssPx(snapshot.bottom, density);
        if (!isLandscape() && safeTop > 0 && !"fullscreen".equals(snapshot.navigationMode)) {
            // WindowInsets 与 WebView 首帧布局存在少量取整误差，给状态栏顶部留出保守余量。
            safeTop += PORTRAIT_TOP_SAFE_AREA_EXTRA_CSS_PX;
        }
        // 横屏 split 时只给两个 WebView 各自靠物理屏幕外侧的横向安全区，避免中缝双重留白。
        if (isLandscape() && secondary != null) {
            if ("primary".equals(webViewId)) safeRight = 0;
            else if ("secondary".equals(webViewId)) safeLeft = 0;
        }
        JSONObject safeArea = new JSONObject().put("top", safeTop).put("right", safeRight)
                .put("bottom", safeBottom).put("left", safeLeft);
        String orientation = isLandscape() ? "landscape" : "portrait";
        return new JSONObject()
                .put("orientation", orientation)
                .put("navigationMode", snapshot.navigationMode)
                .put("safeInsetTop", safeTop)
                .put("safeInsetRight", safeRight)
                .put("safeInsetBottom", safeBottom)
                .put("safeInsetLeft", safeLeft)
                .put("safeInsetUnit", "css_px")
                .put("widthPx", widthPx).put("heightPx", heightPx)
                .put("widthDp", Math.round(widthPx / density))
                .put("heightDp", Math.round(heightPx / density))
                .put("density", density).put("fontScale", fontScale)
                .put("apiLevel", android.os.Build.VERSION.SDK_INT)
                .put("safeArea", safeArea).put("layoutMode", layoutMode())
                .put("webViewId", webViewId)
                .put("locale", java.util.Locale.getDefault().toLanguageTag())
                .put("sdkVersion", com.wishfox.foxsdk.BuildConfig.XYH_GAME_SDK_VERSION_NAME);
    }

    private static int toCssPx(int physicalPx, float density) {
        return Math.max(0, Math.round(physicalPx / Math.max(1f, density)));
    }

    public void openExternal(String value) {
        if (destroyed || TextUtils.isEmpty(value)) return;
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return;
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Throwable ignored) { }
    }

    public void onConfigurationChanged() {
        if (destroyed) return;
        String previousMode = secondary == null ? "single" : (isLandscape() ? "stacked" : "split");
        updateWebViewPanelParams(getWidth());
        requestLayout();
        invalidate();
        sendEnvironmentChanged();
        sendLayoutChanged(previousMode, "orientation");
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateWebViewPanelParams(width);
    }

    @Override
    public boolean dispatchKeyEventPreIme(android.view.KeyEvent event) {
        if (event.getAction() == android.view.KeyEvent.ACTION_UP &&
                event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
            if (preview != null) { preview.close("system_back"); return true; }
            if (secondary != null) {
                if (secondary.canGoBack()) secondary.goBack(); else closeSecondary();
                return true;
            }
            if (primary != null && primary.canGoBack()) {
                primary.goBack();
                return true;
            }
            if (callback != null) callback.onClose();
            return true;
        }
        return super.dispatchKeyEventPreIme(event);
    }

    public void destroy() {
        closeMediaPreview("host_destroyed");
        mediaSaveCoordinator.destroy();
        destroyed = true;
        WebView oldPrimary = primary;
        WebView oldSecondary = secondary;
        primary = null;
        secondary = null;
        if (initialLoadingView != null) {
            initialLoadingView.animate().cancel();
            initialLoadingView = null;
        }
        removeAllViews();
        destroyWebView(oldSecondary);
        destroyWebView(oldPrimary);
    }

    private void destroyWebView(@Nullable WebView view) {
        if (view == null) return;
        NavigationBridge bridge = bridges.remove(view);
        if (bridge != null) bridge.auth.reset();
        if (previewOwner == bridge) closeMediaPreview("source_destroyed");
        try {
            view.stopLoading();
            view.removeJavascriptInterface("WishFoxNative");
            view.loadUrl("about:blank");
            view.removeAllViews();
            view.destroy();
        } catch (Throwable ignored) { }
    }

    private final class RoutingClient extends WebViewClient {
        private final WebView owner;
        RoutingClient(WebView owner) { this.owner = owner; }

        @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
            NavigationBridge bridge = bridges.get(owner);
            FoxSdkLogger.d(BRIDGE_LOG_TAG, "page started: origin=" + h5Origin(url)
                    + ", trusted=" + isTrusted(url));
            if (bridge != null) {
                if (owner == primary && initialRevealPending) initialPageFinished = false;
                if (previewOwner == bridge) closeMediaPreview("source_navigation");
                bridge.generation++;
                bridge.auth.reset();
                bridge.authPending.clear();
                bridge.authCompleted.clear();
                bridge.responses.clear();
            }
        }

        @Override public void onPageFinished(WebView view, String url) {
            FoxSdkLogger.d(BRIDGE_LOG_TAG, "page finished: origin=" + h5Origin(url)
                    + ", trusted=" + isTrusted(url));
            if (owner == primary && initialRevealPending) {
                initialPageFinished = true;
                maybeRevealInitialContent();
            }
        }

        @Override public void onReceivedError(WebView view, android.webkit.WebResourceRequest request,
                                               android.webkit.WebResourceError error) {
            if (request == null || request.isForMainFrame()) {
                FoxSdkLogger.e(BRIDGE_LOG_TAG, "page error: origin="
                        + (request == null || request.getUrl() == null ? "null" : h5Origin(request.getUrl().toString()))
                        + ", code=" + (error == null ? "null" : error.getErrorCode())
                        + ", description=" + (error == null ? "null" : error.getDescription()));
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request != null && request.getUrl() != null) {
                WebResourceResponse asset = mediaSaveCoordinator.openSandboxAsset(
                        request.getUrl().toString());
                if (asset != null) return asset;
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            WebResourceResponse asset = mediaSaveCoordinator.openSandboxAsset(url);
            return asset != null ? asset : super.shouldInterceptRequest(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (request == null || !request.isForMainFrame()) return true;
            return route(view, request == null ? null : request.getUrl().toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return route(view, url);
        }

        private boolean route(WebView view, String url) {
            if (isTrusted(resolveUrl(url))) {
                if (view == primary) openInternal(url);
                return view == primary;
            }
            openExternal(url);
            return true;
        }
    }

    private final class NavigationBridge {
        private final WebView owner;
        private volatile long generation;
        private final LinkedHashMap<String, JSONObject> responses = new LinkedHashMap<>();
        private final FSH5AuthSession auth = new FSH5AuthSession(activity,
                com.wishfox.foxsdk.core.WishFoxSdk.getConfig(), sessionId);
        private final java.util.Set<String> authPending = new java.util.HashSet<>();
        private final java.util.LinkedHashSet<String> authCompleted = new java.util.LinkedHashSet<>();
        private String route = "/";
        private String title = "";
        private boolean canGoBack;
        private boolean hasUnsavedChanges;
        NavigationBridge(WebView owner) { this.owner = owner; }

        boolean valid(long epoch) {
            return !destroyed && generation == epoch && bridges.get(owner) == this && isTrusted(owner.getUrl());
        }

        void send(JSONObject message, long epoch) {
            if (!valid(epoch)) return;
            try {
                owner.evaluateJavascript("window.WishFoxSDK && window.WishFoxSDK.__dispatch && window.WishFoxSDK.__dispatch(JSON.parse("
                        + JSONObject.quote(message.toString()) + "));", null);
            } catch (RuntimeException ignored) { /* 已失效的 WebView 不影响宿主，也不记录含 Token 的载荷。 */ }
        }

        @JavascriptInterface
        public void postMessage(String message) {
            FoxSdkLogger.d(BRIDGE_LOG_TAG, "postMessage received: length="
                    + (message == null ? 0 : message.length()));
            if (message == null) {
                com.wishfox.foxsdk.core.FoxSdkDiagnostics.record("h5_bridge_drop", activity, "null_message");
                return;
            }
            if (message.length() > 32768) {
                com.wishfox.foxsdk.core.FoxSdkDiagnostics.record("h5_bridge_drop", activity, "payload_too_large");
                return;
            }
            final long epoch = generation;
            post(() -> {
                if (!valid(epoch)) {
                    com.wishfox.foxsdk.core.FoxSdkDiagnostics.record("h5_bridge_drop", activity,
                            "invalid_webview_or_origin");
                    return;
                }
                try {
                    handleMediaRequest(this, new JSONObject(message), epoch);
                } catch (JSONException ignored) {
                    com.wishfox.foxsdk.core.FoxSdkDiagnostics.record("h5_bridge_drop", activity,
                            "invalid_json");
                }
            });
        }

        @JavascriptInterface
        public void openInternal(final String url) {
            long epoch = generation;
            post(() -> { if (valid(epoch)) FSH5OverlayView.this.openInternal(url); });
        }

        @JavascriptInterface
        public void openInternal(final String url, final boolean needAppendUserId,
                                  final String userIdKey) {
            long epoch = generation;
            post(() -> {
                if (valid(epoch)) {
                    FSH5OverlayView.this.openInternal(url, needAppendUserId, userIdKey);
                }
            });
        }

        @JavascriptInterface
        public void closeSecondary() {
            long epoch = generation;
            post(() -> { if (valid(epoch)) FSH5OverlayView.this.closeSecondary(); });
        }

        @JavascriptInterface
        public void openExternal(final String url) {
            long epoch = generation;
            post(() -> { if (valid(epoch)) FSH5OverlayView.this.openExternal(url); });
        }

        @JavascriptInterface
        public void closeOverlay() {
            long epoch = generation;
            post(() -> { if (valid(epoch) && callback != null) callback.onClose(); });
        }
    }

    public void onHostPaused() {
        hostResumed = false;
        if (preview != null) preview.onHostPaused();
    }

    public void onHostResumed() {
        hostResumed = true;
        if (preview != null) preview.onHostResumed();
    }

    public void closeMediaPreview(String reason) { if (preview != null) preview.close(reason); }

    public boolean handleMediaBack() {
        if (preview == null) return false;
        preview.close("system_back");
        return true;
    }

    private JSONObject capabilities() throws JSONException {
        return new JSONObject().put("mediaPreviewImage", true).put("mediaPreviewVideo", isHardwareAccelerated())
                .put("mediaPreviewClose", true).put("mediaPreviewMode", "streaming_native")
                .put("mediaSaveImage", true).put("mediaCancel", true)
                .put("mediaRemoveSandboxImage", true).put("mediaSandbox", true)
                .put("mediaGalleryDirect", android.os.Build.VERSION.SDK_INT >= 29
                        ? "api29_no_permission" : "unsupported_no_permission")
                .put("mediaGalleryPicker", android.os.Build.VERSION.SDK_INT < 29
                        ? "api21_plus_optional" : false)
                .put("supportedImageMimeTypes", new org.json.JSONArray()
                        .put("image/png").put("image/jpeg").put("image/webp"))
                .put("maxImageBytes", FSMediaSaveCoordinator.MAX_IMAGE_BYTES)
                .put("maxDataUrlBytes", FSMediaSaveCoordinator.MAX_DATA_URL_BYTES)
                .put("environmentGet", true)
                .put("clipboardCopyText", true)
                .put("authGetState", true).put("authLogin", true).put("authLogout", true)
                .put("authRefreshSession", true)
                .put("uiToast", true).put("uiClose", true)
                .put("navigationOpenInternal", true).put("navigationOpenExternal", true)
                .put("navigationUpdateState", true).put("navigationResolveBack", true)
                .put("layoutSetMode", true).put("layoutCloseSecondary", true)
                .put("h5SessionExchange", true)
                .put("videoDiskCache", false)
                .put("maxPreviewImageBytes", FSMediaPolicy.IMAGE_BYTES);
    }

    private void reply(NavigationBridge bridge, JSONObject request, long epoch, String code, JSONObject data) throws JSONException {
        JSONObject result = new JSONObject().put("version", "1.0").put("type", "response")
                .put("id", request.getString("id")).put("method", request.optString("method"))
                .put("success", "OK".equals(code)).put("code", code).put("message", code)
                .put("timestamp", System.currentTimeMillis()).put("data", data == null ? JSONObject.NULL : data);
        if (request.optString("method").startsWith("auth.")) {
            // 认证响应可能包含凭证：只记录已完成 ID，不进入普通响应重放缓存。
            bridge.authCompleted.add(request.getString("id"));
            if (bridge.authCompleted.size() > 128) bridge.authCompleted.remove(bridge.authCompleted.iterator().next());
        } else {
            bridge.responses.put(request.getString("id"), result);
            if (bridge.responses.size() > 128) bridge.responses.remove(bridge.responses.keySet().iterator().next());
        }
        bridge.send(result, epoch);
    }

    private void mediaEvent(NavigationBridge bridge, long epoch, String id, String state, String reason, int position) {
        if ("error".equals(state)) {
            FoxSdkLogger.e(BRIDGE_LOG_TAG, "media preview failed: previewId=" + id
                    + ", reason=" + reason
                    + ", webViewId=" + (bridge.owner == primary ? "primary" : "secondary"));
        }
        try {
            JSONObject data = new JSONObject().put("previewId", id).put("state", state)
                    .put("reason", reason).put("positionMs", position);
            bridge.send(new JSONObject().put("version", "1.0").put("type", "event")
                    .put("event", "media.previewChanged").put("timestamp", System.currentTimeMillis())
                    .put("webViewId", bridge.owner == primary ? "primary" : "secondary").put("data", data), epoch);
        } catch (JSONException ignored) { }
    }

    private void mediaSaveEvent(NavigationBridge bridge, long epoch, String event, JSONObject data) {
        if (bridge == null || !bridge.valid(epoch) || data == null) return;
        try {
            bridge.send(new JSONObject().put("version", "1.0").put("type", "event")
                    .put("event", event).put("timestamp", System.currentTimeMillis())
                    .put("webViewId", bridge.owner == primary ? "primary" : "secondary")
                    .put("data", data), epoch);
        } catch (JSONException ignored) { }
    }

    private void authChanged() {
        // 双窗口各自持有会话元信息，广播中永不包含任何 Token。
        for (NavigationBridge target : new ArrayList<>(bridges.values())) {
            try {
                target.send(new JSONObject().put("version", "1.0").put("type", "event")
                        .put("event", "auth.changed").put("timestamp", System.currentTimeMillis())
                        .put("webViewId", target.owner == primary ? "primary" : "secondary")
                        .put("data", target.auth.state().put("reason", "auth_operation_completed")), target.generation);
            } catch (JSONException ignored) { }
        }
    }

    private void sendEnvironmentChanged() {
        for (NavigationBridge target : new ArrayList<>(bridges.values())) {
            try {
                target.send(new JSONObject().put("version", "1.0").put("type", "event")
                        .put("event", "environment.changed").put("timestamp", System.currentTimeMillis())
                        .put("webViewId", target.owner == primary ? "primary" : "secondary")
                        .put("data", environment(target.owner == primary ? "primary" : "secondary")),
                        target.generation);
            } catch (JSONException ignored) { }
        }
    }

    private void sendLayoutChanged(String previousMode, String reason) {
        for (NavigationBridge target : new ArrayList<>(bridges.values())) {
            try {
                target.send(new JSONObject().put("version", "1.0").put("type", "event")
                        .put("event", "layout.changed").put("timestamp", System.currentTimeMillis())
                        .put("webViewId", target.owner == primary ? "primary" : "secondary")
                        .put("data", new JSONObject().put("previousMode", previousMode)
                                .put("actualMode", layoutMode()).put("reason", reason)), target.generation);
            } catch (JSONException ignored) { }
        }
    }

    private void sendSecondaryClosed(String reason, String route) {
        for (NavigationBridge target : new ArrayList<>(bridges.values())) {
            try {
                target.send(new JSONObject().put("version", "1.0").put("type", "event")
                        .put("event", "layout.secondaryClosed").put("timestamp", System.currentTimeMillis())
                        .put("webViewId", target.owner == primary ? "primary" : "secondary")
                        .put("data", new JSONObject().put("route", route == null ? "" : route)
                                .put("reason", reason)), target.generation);
            } catch (JSONException ignored) { }
        }
    }

    private void handleAuthRequest(NavigationBridge bridge, JSONObject request, long epoch) throws JSONException {
        String id = request.getString("id");
        String method = request.optString("method");
        if ("auth.refreshSession".equals(method)) {
            FoxSdkLogger.d(BRIDGE_LOG_TAG, "refresh received: requestId=" + id
                    + ", webViewId=" + (bridge.owner == primary ? "primary" : "secondary")
                    + ", epoch=" + epoch
                    + ", bridgeValid=" + bridge.valid(epoch));
        }
        if (bridge.authPending.contains(id)) {
            if ("auth.refreshSession".equals(method)) {
                FoxSdkLogger.w(BRIDGE_LOG_TAG, "refresh ignored: requestId=" + id
                        + ", reason=same_request_already_pending");
            }
            return; // 同一在途请求不重复弹窗/交换。
        }
        if (bridge.authCompleted.contains(id)) {
            if ("auth.refreshSession".equals(method)) {
                FoxSdkLogger.w(BRIDGE_LOG_TAG, "refresh rejected: requestId=" + id
                        + ", reason=request_already_completed");
            }
            reply(bridge, request, epoch, "DUPLICATE_REQUEST", null); return;
        }
        if (!"auth.getState".equals(method) && (!hostResumed || !isShown()
                || activity.isFinishing() || activity.isDestroyed())) {
            if ("auth.refreshSession".equals(method)) {
                FoxSdkLogger.w(BRIDGE_LOG_TAG, "refresh rejected: requestId=" + id
                        + ", reason=host_not_resumed"
                        + ", hostResumed=" + hostResumed
                        + ", viewShown=" + isShown()
                        + ", activityFinishing=" + activity.isFinishing()
                        + ", activityDestroyed=" + activity.isDestroyed());
            }
            reply(bridge, request, epoch, "HOST_NOT_RESUMED", null); return;
        }
        bridge.authPending.add(id);
        if ("auth.logout".equals(method)) {
            if (callback == null) {
                bridge.authPending.remove(id);
                reply(bridge, request, epoch, "METHOD_NOT_SUPPORTED", null);
                return;
            }
            callback.onLogoutRequested((code, data) -> post(() -> {
                bridge.authPending.remove(id);
                if (!bridge.valid(epoch)) return;
                try {
                    if ("OK".equals(code)) authChanged();
                    reply(bridge, request, epoch, code, data);
                    if ("OK".equals(code) && callback != null) {
                        postDelayed(() -> { if (!destroyed) callback.onClose(); }, 0L);
                    }
                } catch (JSONException ignored) { }
            }));
            return;
        }
        bridge.auth.handle(method, request.getJSONObject("params"), (code, data) -> {
            bridge.authPending.remove(id);
            if (!bridge.valid(epoch)) {
                if ("auth.refreshSession".equals(method)) {
                    FoxSdkLogger.w(BRIDGE_LOG_TAG, "refresh response dropped: requestId=" + id
                            + ", reason=invalid_webview_or_epoch");
                }
                return;
            }
            if ("auth.refreshSession".equals(method)) {
                FoxSdkLogger.d(BRIDGE_LOG_TAG, "refresh replying: requestId=" + id
                        + ", code=" + code
                        + ", dataPresent=" + (data != null)
                        + ", sessionTokenIncluded=" + (data != null && data.has("sessionToken"))
                        + ", expiresInIncluded=" + (data != null && data.has("expiresIn")));
            }
            try { reply(bridge, request, epoch, code, data); }
            catch (JSONException ignored) { }
            if (!"auth.getState".equals(method)) authChanged();
        });
    }

    private void handleMediaRequest(NavigationBridge bridge, JSONObject request, long epoch) throws JSONException {
        String id = request.optString("id");
        if (!(request.opt("id") instanceof String) || id.isEmpty() || id.length() > 64
                || !"request".equals(request.optString("type"))) return;
        if (bridge.responses.containsKey(id)) { bridge.send(bridge.responses.get(id), epoch); return; }
        if (!"1.0".equals(request.optString("version"))) {
            reply(bridge, request, epoch, "UNSUPPORTED_PROTOCOL_VERSION", null); return;
        }
        JSONObject params = request.optJSONObject("params");
        if (params == null) { reply(bridge, request, epoch, "INVALID_ARGUMENT", null); return; }
        String method = request.optString("method");
        if ("bridge.getCapabilities".equals(method)) { reply(bridge, request, epoch, "OK", capabilities()); return; }
        if ("bridge.ready".equals(method)) {
            JSONObject authState = bridge.auth.state();
            String webViewId = bridge.owner == primary ? "primary" : "secondary";
            JSONObject environmentData = environment(webViewId);
            FoxSdkLogger.d(BRIDGE_LOG_TAG, "ready received: requestId=" + id
                    + ", webViewId=" + webViewId
                    + ", epoch=" + epoch
                    + ", authStatus=" + authState.optString("status")
                    + ", orientation=" + environmentData.optString("orientation")
                    + ", navigationMode=" + environmentData.optString("navigationMode"));
            JSONObject readyData = new JSONObject().put("selectedProtocolVersion", "1.0")
                    .put("sdkVersion", com.wishfox.foxsdk.BuildConfig.XYH_GAME_SDK_VERSION_NAME)
                    .put("apiLevel", android.os.Build.VERSION.SDK_INT)
                    .put("appId", com.wishfox.foxsdk.core.WishFoxSdk.getConfig().getAppId())
                    .put("channelId", com.wishfox.foxsdk.core.WishFoxSdk.getConfig().getChannelId())
                    .put("isLoggedIn", "authenticated".equals(authState.optString("status")))
                    .put("orientation", environmentData.getString("orientation"))
                    .put("navigationMode", environmentData.getString("navigationMode"))
                    .put("safeInsetTop", environmentData.getInt("safeInsetTop"))
                    .put("safeInsetRight", environmentData.getInt("safeInsetRight"))
                    .put("safeInsetBottom", environmentData.getInt("safeInsetBottom"))
                    .put("safeInsetLeft", environmentData.getInt("safeInsetLeft"))
                    .put("safeInsetUnit", environmentData.optString("safeInsetUnit", "css_px"))
                    .put("sessionId", sessionId).put("webViewId", webViewId)
                    .put("bridgeMode", "restricted_js_interface").put("authState", authState)
                    .put("environment", environmentData)
                    .put("capabilities", capabilities());
            FoxSdkLogger.d(BRIDGE_LOG_TAG, "ready replying: requestId=" + id
                    + ", webViewId=" + webViewId
                    + ", isLoggedIn=" + readyData.optBoolean("isLoggedIn")
                    + ", appIdPresent=" + !TextUtils.isEmpty(readyData.optString("appId"))
                    + ", channelIdPresent=" + !TextUtils.isEmpty(readyData.optString("channelId"))
                    + ", safeInsets=" + readyData.optInt("safeInsetLeft") + "/"
                    + readyData.optInt("safeInsetTop") + "/"
                    + readyData.optInt("safeInsetRight") + "/"
                    + readyData.optInt("safeInsetBottom"));
            reply(bridge, request, epoch, "OK", readyData);
            if (bridge.owner == primary && initialRevealPending) {
                initialBridgeReady = true;
                maybeRevealInitialContent();
            }
            return;
        }
        if (method.startsWith("auth.")) {
            handleAuthRequest(bridge, request, epoch);
            return;
        }
        if ("environment.get".equals(method)) {
            reply(bridge, request, epoch, "OK", environment(bridge.owner == primary ? "primary" : "secondary"));
            return;
        }
        if ("miniProgram.openScheme".equals(method)) {
            String requestId = params.optString("requestId", "").trim();
            if (requestId.isEmpty()) requestId = UUID.randomUUID().toString();
            if (requestId.length() > 128) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            String appName = params.optString("appName",
                    params.optString("name", params.optString("miniProgramName", ""))).trim();
            JSONObject arguments = params.optJSONObject("params");
            if (arguments == null) arguments = params.optJSONObject("query");
            if (arguments == null) arguments = params.optJSONObject("map");
            if ((params.has("params") && !(params.opt("params") instanceof JSONObject))
                    || (params.has("query") && !(params.opt("query") instanceof JSONObject))
                    || (params.has("map") && !(params.opt("map") instanceof JSONObject))) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            if (arguments == null) arguments = new JSONObject();
            if (appName.isEmpty() || appName.length() > 128) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            final String finalRequestId = requestId;
            final JSONObject finalArguments = arguments;
            if (callback == null) {
                reply(bridge, request, epoch, "METHOD_NOT_SUPPORTED", null);
                return;
            }
            callback.onMiniProgramRequested(finalRequestId, appName, finalArguments, (code, data) ->
                    post(() -> {
                        if (!bridge.valid(epoch)) return;
                        try { reply(bridge, request, epoch, code, data); }
                        catch (JSONException ignored) { }
                    }));
            return;
        }
        if ("ui.toast".equals(method)) {
            String message = params.optString("message", "").trim();
            String duration = params.optString("duration", "short");
            if (message.length() < 1 || message.length() > 200
                    || (!"short".equals(duration) && !"long".equals(duration))) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            Toast.makeText(activity, message,
                    "long".equals(duration) ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            reply(bridge, request, epoch, "OK", new JSONObject().put("shown", true));
            return;
        }
        if ("clipboard.copyText".equals(method)) {
            if (!(params.opt("text") instanceof String)) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            String text = params.optString("text", "");
            if (text.trim().isEmpty() || text.length() > 8192) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            FoxSdkUtils.copyText(activity, text)
                    .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                    .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe(success -> {
                        if (!bridge.valid(epoch)) return;
                        try { reply(bridge, request, epoch, "OK", new JSONObject().put("copied", true)); }
                        catch (JSONException ignored) { }
                    }, error -> {
                        if (!bridge.valid(epoch)) return;
                        try { reply(bridge, request, epoch, "CLIPBOARD_FAILED", null); }
                        catch (JSONException ignored) { }
                    });
            return;
        }
        if ("ui.close".equals(method)) {
            if (params.has("force") && !(params.opt("force") instanceof Boolean)) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            reply(bridge, request, epoch, "OK", new JSONObject().put("accepted", true));
            postDelayed(() -> { if (!destroyed && callback != null) callback.onClose(); }, 0L);
            return;
        }
        if ("navigation.openInternal".equals(method)) {
            String url = params.optString("url", "");
            String target = params.optString("target", "auto");
            boolean extendedOpenInternal = params.has("needAppendUserId")
                    || params.has("appendUserId") || params.has("userIdKey");
            Object appendUserIdValue = params.has("needAppendUserId")
                    ? params.opt("needAppendUserId") : params.opt("appendUserId");
            boolean needAppendUserId = appendUserIdValue == null
                    ? false : (appendUserIdValue instanceof Boolean
                    && (Boolean) appendUserIdValue);
            String userIdKey = params.optString("userIdKey", "").trim();
            if (appendUserIdValue != null && !(appendUserIdValue instanceof Boolean)) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            if (needAppendUserId && (userIdKey.isEmpty() || userIdKey.length() > 128
                    || isReservedInternalParam(userIdKey))) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            if (url.length() < 1 || url.length() > 1024 || !"auto".equals(target)) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            JSONObject result = openInternalResult(
                    url, needAppendUserId, userIdKey, extendedOpenInternal);
            if (result == null) {
                reply(bridge, request, epoch, isTrusted(resolveUrl(url))
                        ? "INVALID_URL" : "ORIGIN_NOT_ALLOWED", null);
                return;
            }
            reply(bridge, request, epoch, "OK", result);
            return;
        }
        if ("navigation.openExternal".equals(method)) {
            String url = params.optString("url", "");
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            if (url.length() < 1 || url.length() > 2048
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                reply(bridge, request, epoch, "INVALID_URL", null);
                return;
            }
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                reply(bridge, request, epoch, "OK", new JSONObject().put("accepted", true));
            } catch (Throwable failure) {
                reply(bridge, request, epoch, "EXTERNAL_OPEN_FAILED", null);
            }
            return;
        }
        if ("navigation.updateState".equals(method)) {
            String route = params.optString("route", "");
            if (route.length() < 1 || route.length() > 512
                    || !route.startsWith("/")
                    || !(params.opt("canGoBack") instanceof Boolean)) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            bridge.route = route;
            bridge.title = params.optString("title", "");
            bridge.canGoBack = params.optBoolean("canGoBack", false);
            bridge.hasUnsavedChanges = params.optBoolean("hasUnsavedChanges", false);
            reply(bridge, request, epoch, "OK", new JSONObject().put("updated", true));
            return;
        }
        if ("navigation.resolveBack".equals(method)) {
            if (!(params.opt("backRequestId") instanceof String)
                    || !(params.opt("handled") instanceof Boolean)) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            boolean handled = params.optBoolean("handled", false);
            reply(bridge, request, epoch, "OK", new JSONObject().put("accepted", true));
            if (!handled) {
                postDelayed(() -> {
                    if (destroyed) return;
                    if (bridge.owner == secondary) closeSecondary();
                    else if (bridge.owner.canGoBack()) bridge.owner.goBack();
                    else if (callback != null) callback.onClose();
                }, 0L);
            }
            return;
        }
        if ("layout.closeSecondary".equals(method)) {
            String previous = layoutMode();
            String closedRoute = secondary == null ? "" : secondaryUrl;
            reply(bridge, request, epoch, "OK", new JSONObject()
                    .put("closed", secondary != null)
                    .put("actualMode", "single"));
            closeSecondary();
            if (!"single".equals(previous)) sendSecondaryClosed("js_close", closedRoute);
            return;
        }
        if ("layout.setMode".equals(method)) {
            String requested = params.optString("mode", "");
            if (!"single".equals(requested) && !"split".equals(requested)
                    && !"stacked".equals(requested)) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            String previous = layoutMode();
            if ("single".equals(requested)) {
                String closedRoute = secondary == null ? "" : secondaryUrl;
                reply(bridge, request, epoch, "OK", new JSONObject().put("requestedMode", requested)
                        .put("actualMode", "single").put("primaryWebViewId", "primary"));
                closeSecondary();
                if (!"single".equals(previous)) sendSecondaryClosed("mode_single", closedRoute);
            } else {
                JSONObject secondaryConfig = params.optJSONObject("secondary");
                String route = secondaryConfig == null ? "" : secondaryConfig.optString("route", "");
                JSONObject result = openInternalResult(route);
                if (result == null) {
                    reply(bridge, request, epoch, "INVALID_URL", null);
                    return;
                }
                String actual = isLandscape() ? "split" : "stacked";
                reply(bridge, request, epoch, "OK", new JSONObject().put("requestedMode", requested)
                        .put("actualMode", actual).put("primaryWebViewId", "primary")
                        .put("secondaryWebViewId", "secondary"));
            }
            if (!previous.equals(layoutMode())) sendLayoutChanged(previous, "js_request");
            return;
        }
        if ("media.closePreview".equals(method)) {
            if (preview == null || previewOwner != bridge || !TextUtils.equals(previewId, params.optString("previewId"))) {
                reply(bridge, request, epoch, "PREVIEW_NOT_FOUND", null); return;
            }
            reply(bridge, request, epoch, "OK", new JSONObject().put("accepted", true));
            closeMediaPreview("js_close"); return;
        }
        if ("media.saveImage".equals(method)) {
            String saveRequestId = params.optString("requestId", "").trim();
            if (saveRequestId.isEmpty() || saveRequestId.length() > 64) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            if (mediaSaveCoordinator.contains(saveRequestId)) {
                reply(bridge, request, epoch, "DUPLICATE_REQUEST", null);
                return;
            }
            String validationError = mediaSaveCoordinator.validate(params);
            if (validationError != null) {
                reply(bridge, request, epoch, validationError, null);
                return;
            }
            boolean accepted = mediaSaveCoordinator.start(saveRequestId, params,
                    new FSMediaSaveCoordinator.Callback() {
                        @Override public void onProgress(JSONObject data) {
                            mediaSaveEvent(bridge, epoch, "media.saveProgress", data);
                        }

                        @Override public void onResult(JSONObject data) {
                            mediaSaveEvent(bridge, epoch, "media.saveResult", data);
                        }
                    });
            if (!accepted) {
                reply(bridge, request, epoch, "DUPLICATE_REQUEST", null);
                return;
            }
            reply(bridge, request, epoch, "OK",
                    new JSONObject().put("requestId", saveRequestId).put("status", "accepted"));
            return;
        }
        if ("media.cancel".equals(method)) {
            String saveRequestId = params.optString("requestId", "").trim();
            if (saveRequestId.isEmpty() || saveRequestId.length() > 64) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            reply(bridge, request, epoch, "OK", new JSONObject()
                    .put("requestId", saveRequestId)
                    .put("cancelRequested", mediaSaveCoordinator.cancel(saveRequestId)));
            return;
        }
        if ("media.removeSandboxImage".equals(method)) {
            String assetId = params.optString("assetId", "").trim();
            if (assetId.isEmpty() || !assetId.matches("asset_[A-Za-z0-9]{16,64}")) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null);
                return;
            }
            mediaSaveCoordinator.removeSandboxImage(assetId, (removed, error) -> {
                if (!bridge.valid(epoch)) return;
                try {
                    reply(bridge, request, epoch, error == null ? "OK" : error,
                            new JSONObject().put("assetId", assetId).put("removed", removed));
                } catch (JSONException ignored) { }
            });
            return;
        }
        boolean video = "media.previewVideo".equals(method);
        if (!video && !"media.previewImage".equals(method)) {
            reply(bridge, request, epoch, "METHOD_NOT_SUPPORTED", null); return;
        }
        if (preview != null) { reply(bridge, request, epoch, "BUSY", null); return; }
        if (!hostResumed || activity.isFinishing() || activity.isDestroyed() || !isShown()) {
            reply(bridge, request, epoch, "HOST_NOT_RESUMED", null); return;
        }
        if (video && !isHardwareAccelerated()) {
            reply(bridge, request, epoch, "HARDWARE_ACCELERATION_REQUIRED", null); return;
        }
        String url = params.optString("url", "");
        if (!(params.opt("url") instanceof String)) { reply(bridge, request, epoch, "INVALID_ARGUMENT", null); return; }
        if (url.trim().isEmpty()) { reply(bridge, request, epoch, "INVALID_ARGUMENT", null); return; }
        Uri mediaUri = Uri.parse(url);
        FoxSdkLogger.d(BRIDGE_LOG_TAG, "media preview request: method=" + method
                + ", scheme=" + mediaUri.getScheme()
                + ", host=" + mediaUri.getHost()
                + ", video=" + video);
        for (String flag : new String[]{"muted", "autoPlay"}) {
            if (params.has(flag) && !(params.opt(flag) instanceof Boolean)) {
                reply(bridge, request, epoch, "INVALID_ARGUMENT", null); return;
            }
        }
        final String operation = UUID.randomUUID().toString();
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup)) { reply(bridge, request, epoch, "HOST_NOT_RESUMED", null); return; }
        boolean accepted = false;
        try {
            preview = new FSMediaPreviewView(activity, video, params.optBoolean("muted", false),
                    params.optBoolean("autoPlay", true), new FSMediaPreviewView.Listener() {
                @Override public void onState(String state, String reason, int positionMs) {
                    mediaEvent(bridge, epoch, operation, state, reason, positionMs);
                }
                @Override public void onClosed(String reason, int positionMs) {
                    FSMediaPreviewView previous = preview;
                    preview = null; previewOwner = null; previewId = null;
                    if (previous != null && previous.getParent() instanceof ViewGroup)
                        ((ViewGroup) previous.getParent()).removeView(previous);
                    if (bridge.valid(epoch)) bridge.owner.requestFocus();
                    mediaEvent(bridge, epoch, operation, "closed", reason, positionMs);
                }
            });
            previewId = operation; previewOwner = bridge;
            preview.setZ(getZ() + 1);
            ((ViewGroup) decor).addView(preview, new ViewGroup.LayoutParams(-1, -1));
            reply(bridge, request, epoch, "OK", new JSONObject().put("accepted", true).put("previewId", operation)
                    .put("type", video ? "video" : "image").put("fitMode", "contain")
                    .put("orientation", isLandscape() ? "landscape" : "portrait"));
            accepted = true;
            preview.load(url);
        } catch (RuntimeException failure) {
            if (accepted) mediaEvent(bridge, epoch, operation, "error", "PREVIEW_OPEN_FAILED", 0);
            closeMediaPreview("open_failed");
            if (!accepted) reply(bridge, request, epoch, "PREVIEW_OPEN_FAILED", null);
        }
    }
}
