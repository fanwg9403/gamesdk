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
 * WebView 页的宿主内 Overlay 实现，支持 URL、HTML 内容和 H5 返回交互。
 */
public final class FSWebOverlayView extends FSOverlayPageView {

    private final FsActivityWebDetailBinding binding;
    private final String url;
    private final String html;
    private final boolean showTitle;
    private final boolean showReport;
    private WebView webView;
    private boolean proxyBack;

    /**
     * 创建宿主内 WebView Overlay 页面。
     *
     * @param activity 宿主 Activity
     * @param callback Overlay 关闭回调
     * @param url 要加载的网页地址
     * @param html 要加载的 HTML 内容
     * @param showTitle 是否显示标题栏
     * @param showReport 是否显示举报入口
     */
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

    /**
     * 初始化标题栏、安全区和 WebView 入口参数。
     */
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
            // 管理器会在构造完成后立即挂载当前 View。
            // 这里延后关闭回调，避免页面替换过程发生重入。
            post(this::requestClose);
            return;
        }
        initWebView();
    }

    /**
     * 初始化 WebView 设置并加载 URL 或 HTML 内容。
     */
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

    /**
     * 提供给 H5 调用的关闭入口。
     */
    @JavascriptInterface
    public void finishActivity() {
        post(() -> {
            if (!isDestroyedForOverlay()) {
                requestClose();
            }
        });
    }

    /**
     * 代理页面返回逻辑，优先交给 H5 回调或 WebView 历史栈处理。
     */
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

    /**
     * 调用 H5 注册的方法。
     *
     * @param funName H5 方法名
     * @param data 传递给 H5 的参数
     * @param callbackFunction JavaScript 执行结果回调
     */
    @JavascriptInterface
    public void registerFunction(String funName, String data, ValueCallback<String> callbackFunction) {
        if (webView == null || TextUtils.isEmpty(funName)) {
            return;
        }
        String safeData = data == null ? "" : data.replace("\\", "\\\\").replace("\"", "\\\"");
        String script = "javascript:" + funName + "(\"" + safeData + "\")";
        webView.evaluateJavascript(script, callbackFunction);
    }

    /**
     * 处理宿主返回键并交由网页历史或 H5 回调处理。
     */
    @Override
    protected void handleBackPressed() {
        proxyBackPress();
    }

    /**
     * 销毁 WebView 并释放页面资源。
     */
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
                // Activity 销毁阶段 WebView 释放失败时忽略，避免影响宿主生命周期。
            }
            webView = null;
        }
    }
}
