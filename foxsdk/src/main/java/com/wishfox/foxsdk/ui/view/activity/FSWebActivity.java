package com.wishfox.foxsdk.ui.view.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentActivity;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 20:26
 */
public class FSWebActivity extends FragmentActivity {
    private WebView mWebView;
    private String url;
    private String html;

    //是否显示title
    private boolean showTitle = false;
    private boolean isInit = false;
    private boolean proxyBack = false;
    private boolean needFinish = false;

    //是否显示右边的按钮
    private boolean showReport = false;
    private TextView title;
    private TextView tvBack;
    private TextView tvClose;
    private TextView tvReport;
    private View titleBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fs_activity_web_detail);

        url = getIntent().getStringExtra("url");
        html = getIntent().getStringExtra("html");
        showTitle = getIntent().getBooleanExtra("showTitle", false);
        showReport = getIntent().getBooleanExtra("showReport", false);

        title = findViewById(R.id.fs_tv_title);
        tvReport = findViewById(R.id.fs_tv_report);
        titleBar = findViewById(R.id.fs_title_bar);
        tvBack = findViewById(R.id.fs_tv_back);
        tvClose = findViewById(R.id.fs_tv_close);

        if (showTitle) {
            titleBar.setVisibility(View.VISIBLE);
            FoxSdkViewExt.setOnClickListener(tvBack, v -> onBackPressed());
            FoxSdkViewExt.setOnClickListener(tvClose, v -> finish());
        } else {
            titleBar.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(url) || !TextUtils.isEmpty(html)) {
            init(url != null ? url : "");
        } else {
            Toaster.show(R.string.fs_link_error);
            finish();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                proxyBackPress();
            }
        });
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    private void init(String url) {
        mWebView = findViewById(R.id.fs_web_view);

        WebSettings webSettings = mWebView.getSettings();
        //设置为可调用js方法
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setUseWideViewPort(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);

        mWebView.clearCache(true);

        mWebView.canGoBack();
        mWebView.canGoForward();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // 设置允许JS弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.addJavascriptInterface(this, "AndroidFunction");

        if (html != null) {
            String htmlContent = "<html>\n" +
                    "  <head>\n" +
                    "    <meta charset=\"UTF-8\" />\n" +
                    "    <title>入驻协议</title>\n" +
                    "  </head>\n" +
                    "  <body>\n" +
                    html +
                    " </body>\n" +
                    "<html>";
            mWebView.loadData(htmlContent, "text/html", "utf-8");
        } else {
            String copyUrl = url;
            if (!copyUrl.contains("alipay")) {
                if (copyUrl.contains("?") && !copyUrl.contains("os=app")) {
                    copyUrl = copyUrl + "&os=app";
                } else {
                    copyUrl = copyUrl + "?os=app";
                }
            }
            mWebView.loadUrl(copyUrl);
        }
        mWebView.postDelayed(() -> isInit = true, 500L);
    }

    @JavascriptInterface
    public void finishActivity() {
        this.finish();
    }

    private void proxyBackPress() {
        if (proxyBack) {
            registerFunction("nativeBack", "back", null);
            registerFunction("nativeHandlerBack", "back", null);
        } else {
            //告知web端app端触发了返回
            registerFunction("isAppBackFun", null, null);
            if (mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                finishActivity();
            }
        }
    }

    @JavascriptInterface
    public void registerFunction(String funName, String data, ValueCallback<String> callback) {
        String script = "javascript:" + funName + "(\"" + data + "\")";
        mWebView.evaluateJavascript(script, callback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needFinish) finish();
    }

    public static void startWithUrl(Context context, String url, boolean showTitle) {
        Intent intent = new Intent(context, FSWebActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("showTitle", showTitle);
        context.startActivity(intent);
    }

    public static void startWithUrl(Context context, String url) {
        startWithUrl(context, url, false);
    }

    public static void startWithHtml(Context context, String html, boolean showTitle) {
        Intent intent = new Intent(context, FSWebActivity.class);
        intent.putExtra("html", html);
        intent.putExtra("showTitle", showTitle);
        context.startActivity(intent);
    }

    public static void startWithHtml(Context context, String html) {
        startWithHtml(context, html, false);
    }
}
