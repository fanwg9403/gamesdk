package com.wishfox.foxsdk.ui.view.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.databinding.FsActivityWebDetailBinding;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

/**
 * WebView 页的宿主内 Overlay 实现。
 */
public final class FSWebOverlayView extends FSOverlayPageView {

    private final FsActivityWebDetailBinding binding;
    private final String url;
    private final String html;
    private final boolean showTitle;
    private final boolean showReport;
    private WebView webView;
    private boolean proxyBack;

    public FSWebOverlayView(
            Activity activity,
            Callback callback,
            String url,
            String html,
            boolean showTitle,
            boolean showReport
    ) {
        super(activity, callback);
        this.url = url;
        this.html = html;
        this.showTitle = showTitle;
        this.showReport = showReport;
        binding = FsActivityWebDetailBinding.inflate(LayoutInflater.from(activity), this, true);
        initView();
    }

    private void initView() {
        FSOverlayInsets.applyToPadding(activity, this);

        binding.fsTitleBar.setVisibility(showTitle ? View.VISIBLE : View.GONE);
        binding.fsTvReport.setVisibility(showReport ? View.VISIBLE : View.GONE);
        if (showTitle) {
            FoxSdkViewExt.setOnClickListener(binding.fsTvBack, v -> proxyBackPress());
            FoxSdkViewExt.setOnClickListener(binding.fsTvClose, v -> requestClose());
        }

        if (TextUtils.isEmpty(url) && TextUtils.isEmpty(html)) {
            Toaster.show(R.string.fs_link_error);
            // The manager attaches this view immediately after construction.
            // Post the close callback to avoid re-entrant page replacement.
            post(this::requestClose);
            return;
        }
        initWebView();
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    private void initWebView() {
        webView = binding.fsWebView;
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setUseWideViewPort(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLoadWithOverviewMode(false);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.clearCache(true);
        webView.addJavascriptInterface(this, "AndroidFunction");

        if (!TextUtils.isEmpty(html)) {
            String htmlContent = "<html><head><meta charset=\"UTF-8\" /></head><body>" +
                    html + "</body></html>";
            webView.loadData(htmlContent, "text/html", "utf-8");
        } else {
            String copyUrl = url;
            if (!copyUrl.contains("alipay")) {
                if (copyUrl.contains("?") && !copyUrl.contains("os=app")) {
                    copyUrl = copyUrl + "&os=app";
                } else if (!copyUrl.contains("os=app")) {
                    copyUrl = copyUrl + "?os=app";
                }
            }
            webView.loadUrl(copyUrl);
        }
    }

    @JavascriptInterface
    public void finishActivity() {
        post(() -> {
            if (!isDestroyedForOverlay()) {
                requestClose();
            }
        });
    }

    private void proxyBackPress() {
        if (webView == null) {
            requestClose();
            return;
        }
        if (proxyBack) {
            registerFunction("nativeBack", "back", null);
            registerFunction("nativeHandlerBack", "back", null);
        } else {
            registerFunction("isAppBackFun", null, null);
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                requestClose();
            }
        }
    }

    @JavascriptInterface
    public void registerFunction(String funName, String data, ValueCallback<String> callbackFunction) {
        if (webView == null || TextUtils.isEmpty(funName)) {
            return;
        }
        String safeData = data == null ? "" : data.replace("\\", "\\\\").replace("\"", "\\\"");
        String script = "javascript:" + funName + "(\"" + safeData + "\")";
        webView.evaluateJavascript(script, callbackFunction);
    }

    @Override
    protected void handleBackPressed() {
        proxyBackPress();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.removeJavascriptInterface("AndroidFunction");
                webView.loadUrl("about:blank");
                webView.clearHistory();
                webView.removeAllViews();
                webView.destroy();
            } catch (Throwable ignored) {
                // WebView teardown is best effort during Activity destruction.
            }
            webView = null;
        }
    }
}
