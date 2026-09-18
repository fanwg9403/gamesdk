package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.media.FSMediaPolicy;

/** 登录协议专用只读 Overlay，不参与首页双 WebView 路由，也不注入 JS Bridge。 */
public final class FSLoginAgreementView extends LinearLayout implements Application.ActivityLifecycleCallbacks {
    private final Activity host;
    private final Runnable onReturn;
    private final Runnable onHostGone;
    private WebView web;
    private boolean closed;
    private androidx.activity.OnBackPressedCallback legacyBack;
    private Object backDispatcher;
    private Object backCallback;

    public FSLoginAgreementView(Activity host, String url, String title, Runnable onReturn, Runnable onHostGone) {
        super(host);
        this.host = host;
        this.onReturn = onReturn;
        this.onHostGone = onHostGone;
        String origin = FSMediaPolicy.origin(url);
        setOrientation(VERTICAL);
        setBackgroundColor(0xFFFFFFFF);
        setClickable(true);
        setFocusableInTouchMode(true);
        FSOverlayInsets.applyToPadding(host, this);
        FrameLayout bar = new FrameLayout(host);
        int barHeight = getResources().getDimensionPixelSize(R.dimen.dp_48);
        int backPadding = getResources().getDimensionPixelSize(R.dimen.dp_14);
        ImageView back = new ImageView(host);
        back.setImageResource(R.drawable.fs_right_back);
        back.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        back.setPadding(backPadding, backPadding, backPadding, backPadding);
        back.setContentDescription(getResources().getString(R.string.fs_back));
        back.setFocusable(true);
        back.setOnClickListener(v -> back());
        bar.addView(back, new FrameLayout.LayoutParams(barHeight, barHeight, android.view.Gravity.START));
        TextView caption = new TextView(host);
        caption.setText(title);
        caption.setTextColor(0xFF222222);
        caption.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.size_15));
        caption.setSingleLine(true);
        caption.setEllipsize(android.text.TextUtils.TruncateAt.END);
        caption.setGravity(android.view.Gravity.CENTER);
        // 两侧等宽留白使标题居中；右侧不再放置关闭按钮。
        FrameLayout.LayoutParams captionParams = new FrameLayout.LayoutParams(-1, -1);
        captionParams.leftMargin = barHeight;
        captionParams.rightMargin = barHeight;
        bar.addView(caption, captionParams);
        addView(bar, new LinearLayout.LayoutParams(-1, barHeight));
        web = new WebView(host);
        web.getSettings().setJavaScriptEnabled(false);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.getSettings().setSupportMultipleWindows(false);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient() {
            private boolean blocked(String value) {
                try { return !origin.equals(FSMediaPolicy.origin(value)); }
                catch (IllegalArgumentException ignored) { return true; }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String value) { return blocked(value); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return blocked(request.getUrl().toString());
            }
            @Override public void onReceivedError(WebView view, int code, String description, String failingUrl) {
                caption.setText("加载失败，请返回后重试");
            }
            @Override public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler,
                                                      android.net.http.SslError error) {
                handler.cancel();
                caption.setText("安全连接失败，请返回");
            }
            @Override public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                // API 26+：只回收此 WebView；不重启、终止宿主进程。
                close(true);
                return true;
            }
        });
        addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        web.loadUrl(url);
    }

    public void back() {
        if (closed) return;
        if (web != null && web.canGoBack()) web.goBack();
        else close(true);
    }

    /** restore=false 用于登录结束/宿主销毁，不重新弹出登录窗口。 */
    public void close(boolean restore) {
        if (closed) return;
        closed = true;
        host.getApplication().unregisterActivityLifecycleCallbacks(this);
        if (legacyBack != null) { legacyBack.remove(); legacyBack = null; }
        if (backDispatcher != null && backCallback != null) {
            try {
                Class.forName("android.window.OnBackInvokedDispatcher")
                        .getMethod("unregisterOnBackInvokedCallback", Class.forName("android.window.OnBackInvokedCallback"))
                        .invoke(backDispatcher, backCallback);
            } catch (Exception ignored) { }
        }
        backDispatcher = backCallback = null;
        if (web != null) {
            WebView previous = web;
            web = null;
            removeView(previous);
            try { previous.stopLoading(); }
            catch (RuntimeException ignored) { }
            try { previous.destroy(); }
            catch (RuntimeException ignored) { }
        }
        if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(this);
        if (restore && !host.isFinishing() && !host.isDestroyed()) onReturn.run();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) back();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        host.getApplication().registerActivityLifecycleCallbacks(this);
        requestFocus();
        if (host instanceof androidx.activity.ComponentActivity) {
            legacyBack = new androidx.activity.OnBackPressedCallback(true) {
                @Override public void handleOnBackPressed() { back(); }
            };
            ((androidx.activity.ComponentActivity) host).getOnBackPressedDispatcher().addCallback(legacyBack);
        }
        // 与媒体 Overlay 一致，反射兼容项目 compileSdk 30。
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Class<?> type = Class.forName("android.window.OnBackInvokedCallback");
                backDispatcher = Activity.class.getMethod("getOnBackInvokedDispatcher").invoke(host);
                backCallback = java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                        (proxy, method, args) -> {
                            if ("onBackInvoked".equals(method.getName())) { post(this::back); return null; }
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                            if ("equals".equals(method.getName())) return proxy == args[0];
                            if ("toString".equals(method.getName())) return "WishFoxAgreementBack";
                            return null;
                        });
                Class.forName("android.window.OnBackInvokedDispatcher")
                        .getMethod("registerOnBackInvokedCallback", int.class, type)
                        .invoke(backDispatcher, 1000000, backCallback);
            } catch (Exception ignored) { backDispatcher = backCallback = null; }
        }
    }

    @Override protected void onDetachedFromWindow() {
        boolean unexpected = !closed;
        close(false);
        if (unexpected) onHostGone.run();
        super.onDetachedFromWindow();
    }
    @Override public void onActivityPaused(Activity activity) { if (activity == host && web != null) web.onPause(); }
    @Override public void onActivityResumed(Activity activity) { if (activity == host && web != null) web.onResume(); }
    @Override public void onActivityDestroyed(Activity activity) {
        if (activity == host) { close(false); onHostGone.run(); }
    }
    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
}
