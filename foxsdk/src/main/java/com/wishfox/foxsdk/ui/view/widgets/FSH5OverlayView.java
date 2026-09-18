package com.wishfox.foxsdk.ui.view.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import com.wishfox.foxsdk.media.FSMediaPolicy;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.Nullable;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.auth.FSH5AuthSession;

/**
 * H5 业务页面的 Overlay 容器。
 *
 * <p>H5 按普通页面/SPA 路由开发。内部链接不使用 target=_blank，
 * 由本容器根据真实屏幕方向创建或复用 Secondary WebView。</p>
 */
public final class FSH5OverlayView extends FrameLayout {

    public interface Callback {
        void onClose();
    }

    private final Activity activity;
    private final Callback callback;
    private final String trustedOrigin;
    private WebView primary;
    private WebView secondary;
    private String secondaryUrl;
    private boolean destroyed;
    private final Map<WebView, NavigationBridge> bridges = new HashMap<>();
    private final String sessionId = UUID.randomUUID().toString();
    private FSMediaPreviewView preview;
    private String previewId;
    private NavigationBridge previewOwner;
    private boolean hostResumed = true;

    public FSH5OverlayView(Activity activity, Callback callback, String homeUrl) {
        super(activity);
        this.activity = activity;
        this.callback = callback;
        FoxSdkConfig config = com.wishfox.foxsdk.core.WishFoxSdk.getConfig();
        this.trustedOrigin = FSMediaPolicy.origin(TextUtils.isEmpty(config.getH5TrustedOrigin())
                ? homeUrl : config.getH5TrustedOrigin());
        if (!isTrusted(homeUrl)) throw new IllegalArgumentException("Untrusted H5 home URL");
        setClickable(true);
        setFocusable(true);
        FSOverlayInsets.applyToPadding(activity, this);
        primary = createWebView();
        addView(primary, primaryParams());
        primary.loadUrl(homeUrl);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private WebView createWebView() {
        WebView view = new WebView(activity);
        view.setBackgroundColor(0xFF222222);
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
        if (isLandscape()) {
            LayoutParams params = new LayoutParams(landscapePrimaryWidth(), LayoutParams.MATCH_PARENT, Gravity.START);
            return params;
        }
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    private LayoutParams secondaryParams() {
        if (isLandscape()) {
            LayoutParams params = new LayoutParams(landscapeSecondaryWidth(), LayoutParams.MATCH_PARENT, Gravity.END);
            return params;
        }
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }

    private int landscapePrimaryWidth() {
        int width = getWidth();
        return width > 0 ? Math.round(width * 0.448f) : LayoutParams.MATCH_PARENT;
    }

    private int landscapeSecondaryWidth() {
        int width = getWidth();
        return width > 0 ? Math.round(width * 0.552f) : LayoutParams.MATCH_PARENT;
    }

    private boolean isTrusted(String url) {
        if (TextUtils.isEmpty(url)) return false;
        Uri uri = Uri.parse(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
        try { return trustedOrigin.equals(FSMediaPolicy.origin(url)); }
        catch (IllegalArgumentException ignored) { return false; }
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
        if (destroyed) return;
        final String url = resolveUrl(value);
        if (!isTrusted(url)) return;
        if (secondary == null) {
            secondary = createWebView();
            secondaryUrl = url;
            addView(secondary, secondaryParams());
        } else {
            secondaryUrl = url;
        }
        secondary.loadUrl(url);
        secondary.bringToFront();
        if (!isLandscape()) secondary.bringToFront();
    }

    public void closeSecondary() {
        if (secondary == null) return;
        removeView(secondary);
        destroyWebView(secondary);
        secondary = null;
        secondaryUrl = null;
        if (primary != null) primary.bringToFront();
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
        if (primary != null) primary.setLayoutParams(primaryParams());
        if (secondary != null) secondary.setLayoutParams(secondaryParams());
        requestLayout();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (isLandscape()) {
            if (primary != null) primary.setLayoutParams(primaryParams());
            if (secondary != null) secondary.setLayoutParams(secondaryParams());
        }
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
        destroyed = true;
        WebView oldPrimary = primary;
        WebView oldSecondary = secondary;
        primary = null;
        secondary = null;
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
            if (bridge != null) {
                if (previewOwner == bridge) closeMediaPreview("source_navigation");
                bridge.generation++;
                bridge.auth.reset();
                bridge.authPending.clear();
                bridge.authCompleted.clear();
                bridge.responses.clear();
            }
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
            if (message == null || message.length() > 32768) return;
            final long epoch = generation;
            post(() -> {
                if (!valid(epoch)) return;
                try { handleMediaRequest(this, new JSONObject(message), epoch); }
                catch (JSONException ignored) { /* Invalid envelope has no trustworthy request ID. */ }
            });
        }

        @JavascriptInterface
        public void openInternal(final String url) {
            long epoch = generation;
            post(() -> { if (valid(epoch)) FSH5OverlayView.this.openInternal(url); });
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
                .put("authGetState", true).put("authLogin", true).put("authLogout", false)
                .put("authRefreshSession", com.wishfox.foxsdk.core.WishFoxSdk.getConfig().getH5SessionTokenProvider() != null)
                .put("h5SessionExchange", com.wishfox.foxsdk.core.WishFoxSdk.getConfig().getH5SessionTokenProvider() != null)
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
        try {
            JSONObject data = new JSONObject().put("previewId", id).put("state", state)
                    .put("reason", reason).put("positionMs", position);
            bridge.send(new JSONObject().put("version", "1.0").put("type", "event")
                    .put("event", "media.previewChanged").put("timestamp", System.currentTimeMillis())
                    .put("webViewId", bridge.owner == primary ? "primary" : "secondary").put("data", data), epoch);
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

    private void handleAuthRequest(NavigationBridge bridge, JSONObject request, long epoch) throws JSONException {
        String id = request.getString("id");
        String method = request.optString("method");
        if (bridge.authPending.contains(id)) return; // 同一在途请求不重复弹窗/交换。
        if (bridge.authCompleted.contains(id)) {
            reply(bridge, request, epoch, "DUPLICATE_REQUEST", null); return;
        }
        if (!"auth.getState".equals(method) && (!hostResumed || !isShown()
                || activity.isFinishing() || activity.isDestroyed())) {
            reply(bridge, request, epoch, "HOST_NOT_RESUMED", null); return;
        }
        bridge.authPending.add(id);
        bridge.auth.handle(method, request.getJSONObject("params"), (code, data) -> {
            bridge.authPending.remove(id);
            if (!bridge.valid(epoch)) return;
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
            reply(bridge, request, epoch, "OK", new JSONObject().put("selectedProtocolVersion", "1.0")
                    .put("sessionId", sessionId).put("webViewId", bridge.owner == primary ? "primary" : "secondary")
                    .put("bridgeMode", "restricted_js_interface").put("authState", bridge.auth.state())
                    .put("capabilities", capabilities())); return;
        }
        if (method.startsWith("auth.")) {
            handleAuthRequest(bridge, request, epoch);
            return;
        }
        if ("media.closePreview".equals(method)) {
            if (preview == null || previewOwner != bridge || !TextUtils.equals(previewId, params.optString("previewId"))) {
                reply(bridge, request, epoch, "PREVIEW_NOT_FOUND", null); return;
            }
            reply(bridge, request, epoch, "OK", new JSONObject().put("accepted", true));
            closeMediaPreview("js_close"); return;
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
        List<String> origins = new ArrayList<>(com.wishfox.foxsdk.core.WishFoxSdk.getConfig().getH5MediaOrigins());
        if (origins.isEmpty()) origins.add(trustedOrigin);
        if (!FSMediaPolicy.allowed(url, origins)) {
            reply(bridge, request, epoch, "MEDIA_URL_NOT_ALLOWED", null); return;
        }
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
            preview.load(url, origins);
        } catch (RuntimeException failure) {
            if (accepted) mediaEvent(bridge, epoch, operation, "error", "PREVIEW_OPEN_FAILED", 0);
            closeMediaPreview("open_failed");
            if (!accepted) reply(bridge, request, epoch, "PREVIEW_OPEN_FAILED", null);
        }
    }
}
