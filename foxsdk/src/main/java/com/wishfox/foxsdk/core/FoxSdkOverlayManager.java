package com.wishfox.foxsdk.core;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wishfox.foxsdk.data.model.entity.FSGameSchemeData;
import com.wishfox.foxsdk.data.model.entity.FSPayResult;
import com.wishfox.foxsdk.data.network.FoxSdkNetworkExecutor;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;
import com.wishfox.foxsdk.ui.view.activity.FSHomeActivity;
import com.wishfox.foxsdk.data.model.entity.FSMessage;
import com.wishfox.foxsdk.ui.view.activity.FSWinFoxCoinActivity;
import com.wishfox.foxsdk.ui.view.widgets.FSGameRecordOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSHomeOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSMessageOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSOverlayPageView;
import com.wishfox.foxsdk.ui.view.widgets.FSRechargeRecordOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSStarterPackOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSWebOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSH5OverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSLoadingDialog;
import com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.di.FoxSdkRepositoryContainer;
import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.ui.view.widgets.FSWinFoxCoinOverlayView;
import com.wishfox.foxsdk.ui.viewstate.FSHomeViewState;
import com.wishfox.foxsdk.data.model.entity.FSUserProfile;
import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.utils.FoxSdkAppJumpUtils;
import com.wishfox.foxsdk.utils.FoxSdkConstant;
import com.wishfox.foxsdk.utils.FoxSdkPayEnum;
import com.wishfox.foxsdk.utils.FoxSdkSPUtils;
import com.wishfox.foxsdk.utils.FoxSdkLogger;
import com.wishfox.foxsdk.ui.view.dialog.FSAlertDialog;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.pay.FoxSdkWxPay;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.adapter.rxjava3.HttpException;

/**
 * 统一管理挂载在宿主 Activity 内的所有 SDK 页面。
 *
 * <p>页面栈基于 View 实现，从首页切换到记录、消息、WebView、礼包或赢狐币页面时，
 * 不再启动新的 Activity，因此不会触发 Unity/Cocos 宿主 Activity 进入 {@code onPause}。</p>
 */
public final class FoxSdkOverlayManager {
    private static final String H5_LOG_TAG = "FoxSdk[H5]";

    public enum Page {
        GAME_RECORD,
        RECHARGE_RECORD,
        MESSAGE,
        STARTER_PACK,
        WIN_FOX_COIN,
        WEB
    }

    /**
     * H5 请求原生登录后的结果回调。回调始终在主线程触发。
     */
    public interface LoginCallback {
        /**
         * 登录成功，长期 Token 仅留在原生侧，调用方不得向 H5 暴露。
         */
        void onSuccess(FSLoginResult result);

        /**
         * 用户关闭登录弹窗且未完成登录。
         */
        void onCancelled();

        /**
         * 无法启动/继续登录操作；可重试的表单错误在原生弹窗内提示。
         */
        void onFailure(String code);
    }

    private static final Map<Activity, WeakReference<FoxSdkOverlayManager>> INSTANCES =
            new WeakHashMap<>();
    private static final String ROUTE_HOME = "home";

    private final WeakReference<Activity> activityReference;
    private ViewGroup hostRoot;
    private FSHomeOverlayView homeView;
    private FSOverlayPageView pageView;
    private FSH5OverlayView h5View;
    private Page currentPage;
    private boolean destroyed;
    private boolean fallbackStarted;
    private String webUrl;
    private String webHtml;
    private boolean webShowTitle;
    private boolean webShowReport;
    private LoginAttempt loginAttempt;
    private LogoutAttempt logoutAttempt;
    private io.reactivex.rxjava3.disposables.Disposable logoutRequest;
    /**
     * H5 小程序请求期间的原生 loading；只允许当前请求持有。
     */
    private FSLoadingDialog miniProgramLoading;

    private FoxSdkOverlayManager(Activity activity) {
        activityReference = new WeakReference<>(activity);
    }

    public static void show(Activity activity) {
        runOnMain(activity, () -> {
            if (!isActivityUsable(activity)) {
                FoxSdkDiagnostics.record("overlay_show_ignored", activity, "activity_unusable");
                return;
            }
            if (!WishFoxSdk.isInitialized()) {
                FoxSdkDiagnostics.record("overlay_show_ignored", activity, "sdk_not_initialized");
                return;
            }
            getOrCreate(activity).showH5OrLoginInternal();
        });
    }

    /**
     * 显式打开 H5 首页；适用于宿主已完成登录态确认的入口。
     */
    public static void showH5(Activity activity) {
        runOnMain(activity, () -> {
            if (isActivityUsable(activity) && WishFoxSdk.isInitialized()
                    && resolveH5HomeUrl() != null) {
                getOrCreate(activity).showH5Internal();
            } else {
                FoxSdkLogger.w(H5_LOG_TAG, "showH5 ignored: activityUsable=" + isActivityUsable(activity)
                        + ", sdkInitialized=" + WishFoxSdk.isInitialized()
                        + ", loginH5UrlAvailable=" + (WishFoxSdk.isInitialized()
                        && resolveH5HomeUrl() != null));
            }
        });
    }

    /**
     * Demo/H5 联调专用入口。Release AAR 中不会执行，也不会改变登录接口持久化的 h5Url。
     */
    public static void showDebugH5(Activity activity, String url) {
        runOnMain(activity, () -> {
            if (!com.wishfox.foxsdk.BuildConfig.DEBUG) {
                FoxSdkLogger.w(H5_LOG_TAG, "showDebugH5 ignored in release build");
                return;
            }
            if (!isActivityUsable(activity) || !WishFoxSdk.isInitialized()) {
                FoxSdkLogger.w(H5_LOG_TAG, "showDebugH5 ignored: invalid activity/sdk");
                return;
            }
            String homeUrl = validateH5HomeUrl(url);
            if (homeUrl == null) {
                FoxSdkLogger.w(H5_LOG_TAG, "showDebugH5 ignored: invalid url");
                return;
            }
            FoxSdkOverlayManager manager = getOrCreate(activity);
            manager.removeH5View();
            manager.showH5Internal(homeUrl);
        });
    }

    public static void showPage(Activity activity, Page page) {
        runOnMain(activity, () -> {
            if (!isActivityUsable(activity) || !WishFoxSdk.isInitialized() || page == null) {
                FoxSdkDiagnostics.record("overlay_page_ignored", activity,
                        page == null ? "page_null" : "activity_or_sdk_unusable");
                return;
            }
            getOrCreate(activity).showPageInternal(page);
        });
    }

    public static void showWeb(Activity activity, String url, boolean showTitle) {
        runOnMain(activity, () -> {
            if (!isActivityUsable(activity) || !WishFoxSdk.isInitialized()) {
                FoxSdkDiagnostics.record("overlay_web_ignored", activity, "activity_or_sdk_unusable");
                return;
            }
            getOrCreate(activity).showWebInternal(url, null, showTitle, false);
        });
    }

    public static void showWebHtml(Activity activity, String html, boolean showTitle) {
        runOnMain(activity, () -> {
            if (!isActivityUsable(activity) || !WishFoxSdk.isInitialized()) {
                FoxSdkDiagnostics.record("overlay_web_html_ignored", activity,
                        "activity_or_sdk_unusable");
                return;
            }
            getOrCreate(activity).showWebInternal(null, html, showTitle, false);
        });
    }

    public static void hide(Activity activity) {
        runOnMain(activity, () -> {
            FoxSdkOverlayManager manager = find(activity);
            if (manager != null) {
                manager.hideInternal();
            }
        });
    }

    public static boolean isShowing(Activity activity) {
        FoxSdkOverlayManager manager = find(activity);
        return manager != null &&
                (manager.homeView != null || manager.pageView != null || manager.h5View != null) &&
                !manager.destroyed;
    }

    public static void onActivityDestroyed(Activity activity) {
        FoxSdkOverlayManager manager = null;
        synchronized (INSTANCES) {
            WeakReference<FoxSdkOverlayManager> reference = INSTANCES.remove(activity);
            if (reference != null) {
                manager = reference.get();
            }
        }
        if (manager != null) {
            manager.destroyInternal();
        }
    }

    public static void onActivityConfigurationChanged(Activity activity) {
        runOnMain(activity, () -> {
            FoxSdkOverlayManager manager = find(activity);
            if (manager != null) {
                manager.rebuildForConfiguration();
            }
        });
    }

    /**
     * 应用级别的横竖屏变化回调不携带 Activity 实例。
     * 这里先快照当前注册的管理器，再把重建逻辑派发回各自宿主 Activity 的主线程。
     */
    public static void onConfigurationChanged() {
        List<Activity> activities = new ArrayList<>();
        synchronized (INSTANCES) {
            for (Map.Entry<Activity, WeakReference<FoxSdkOverlayManager>> entry
                    : INSTANCES.entrySet()) {
                WeakReference<FoxSdkOverlayManager> reference = entry.getValue();
                FoxSdkOverlayManager manager = reference == null ? null : reference.get();
                if (manager != null && entry.getKey() != null) {
                    activities.add(entry.getKey());
                }
            }
        }
        for (Activity activity : activities) {
            onActivityConfigurationChanged(activity);
        }
    }

    private static FoxSdkOverlayManager find(Activity activity) {
        if (activity == null) {
            return null;
        }
        synchronized (INSTANCES) {
            WeakReference<FoxSdkOverlayManager> reference = INSTANCES.get(activity);
            return reference == null ? null : reference.get();
        }
    }

    private static FoxSdkOverlayManager getOrCreate(Activity activity) {
        synchronized (INSTANCES) {
            WeakReference<FoxSdkOverlayManager> reference = INSTANCES.get(activity);
            FoxSdkOverlayManager manager = reference == null ? null : reference.get();
            if (manager == null) {
                manager = new FoxSdkOverlayManager(activity);
                INSTANCES.put(activity, new WeakReference<>(manager));
            }
            return manager;
        }
    }

    private static void runOnMain(Activity activity, Runnable action) {
        if (activity == null || action == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            activity.runOnUiThread(action);
        }
    }

    private static boolean isActivityUsable(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ||
                !activity.isDestroyed();
    }

    private void showHomeInternal() {
        showHomeInternal(null);
    }

    /**
     * H5 Bridge 请求展示原生登录弹窗。
     */
    public static void requestLogin(Activity activity, LoginCallback callback) {
        Runnable action = () -> {
            if (!isActivityUsable(activity) || !WishFoxSdk.isInitialized()) {
                if (callback != null) callback.onFailure("ACTIVITY_UNAVAILABLE");
                return;
            }
            getOrCreate(activity).showLoginInternal(callback);
        };
        if (Looper.myLooper() == Looper.getMainLooper()) action.run();
        else new android.os.Handler(Looper.getMainLooper()).post(action);
    }

    /**
     * 仅取消此调用方拥有的登录，避免页面销毁时误关宿主发起的登录。
     */
    public static void cancelLogin(Activity activity, LoginCallback callback) {
        runOnMain(activity, () -> {
            FoxSdkOverlayManager manager = find(activity);
            if (manager != null && manager.loginAttempt != null
                    && manager.loginAttempt.callback == callback) manager.cancelLoginInternal();
        });
    }

    public static void onHostPaused(Activity activity) {
        FoxSdkOverlayManager manager = find(activity);
        if (manager != null && manager.h5View != null) manager.h5View.onHostPaused();
    }

    public static void onHostResumed(Activity activity) {
        FoxSdkOverlayManager manager = find(activity);
        if (manager != null && manager.h5View != null) manager.h5View.onHostResumed();
    }

    public static void onHostStopped(Activity activity) {
        FoxSdkOverlayManager manager = find(activity);
        if (manager != null && manager.h5View != null)
            manager.h5View.closeMediaPreview("host_stopped");
    }

    /**
     * 游戏接管返回键时先调用；true 表示协议页或原生媒体预览已处理返回。
     */
    public static boolean onHostBackPressed(Activity activity) {
        if (FSLoginDialog.handleAgreementBack(activity)) return true;
        FoxSdkOverlayManager manager = find(activity);
        return manager != null && manager.h5View != null && manager.h5View.handleMediaBack();
    }

    private void showH5OrLoginInternal() {
        if (!TextUtils.isEmpty(FSLoginResult.getTokenEd())) {
            if (resolveH5HomeUrl() != null) {
                showH5Internal();
            } else {
                // 兼容升级前没有 h5Url 的旧登录缓存：要求重新登录以获取服务端下发的首页地址。
                Activity activity = activityReference.get();
                FoxSdkLogger.w(H5_LOG_TAG, "stored login has no valid h5Url; require re-login");
                FoxSdkDiagnostics.record("h5_login_required", activity, "missing_h5_url");
                clearLocalAuthState();
                showLoginInternal(null);
            }
            return;
        }
        showLoginInternal(null);
    }

    private static final class LoginAttempt {
        final LoginCallback callback;
        final long revision = FSLoginResult.getSessionRevision();
        FSLoginDialog dialog;
        io.reactivex.rxjava3.disposables.Disposable request;
        boolean submitting;

        LoginAttempt(LoginCallback callback) {
            this.callback = callback;
        }
    }

    private static final class LogoutAttempt {
        final FSH5OverlayView.LogoutCompletion completion;
        FSAlertDialog dialog;
        boolean completed;

        LogoutAttempt(FSH5OverlayView.LogoutCompletion completion) {
            this.completion = completion;
        }
    }

    private void cancelLoginInternal() {
        LoginAttempt previous = loginAttempt;
        loginAttempt = null;
        if (previous == null) return;
        if (previous.request != null) previous.request.dispose();
        if (previous.dialog != null) previous.dialog.dismiss();
    }

    private void showLoginInternal(LoginCallback callback) {
        Activity activity = activityReference.get();
        if (destroyed || !isActivityUsable(activity)) {
            if (callback != null) callback.onFailure("ACTIVITY_UNAVAILABLE");
            return;
        }
        if (loginAttempt != null || FSLoginDialog.hasActiveInstance()) {
            if (callback != null) callback.onFailure("BUSY");
            return;
        }
        LoginAttempt attempt = new LoginAttempt(callback);
        loginAttempt = attempt;
        try {
            attempt.dialog = new FSLoginDialog(activity).setOnLoginClickListener((phone, code, type, loading) -> {
                if (loginAttempt != attempt || attempt.submitting) {
                    if (loading != null) loading.dismiss();
                    return;
                }
                attempt.submitting = true;
                attempt.request = FoxSdkRepositoryContainer.getHomeRepository().login(phone, code, type)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (loginAttempt != attempt || destroyed || !isActivityUsable(activity))
                                return;
                            attempt.submitting = false;
                            if (loading != null) loading.dismiss();
                            if (attempt.revision != FSLoginResult.getSessionRevision()) {
                                cancelLoginInternal();
                                if (callback != null) callback.onFailure("AUTH_STATE_CHANGED");
                                return;
                            }
                            FSLoginResult data = result.getData();
                            if (result.isSuccess() && data != null && !TextUtils.isEmpty(data.getToken())) {
                                FoxSdkLogger.d(H5_LOG_TAG, "login success: h5UrlPresent="
                                        + !TextUtils.isEmpty(data.getH5Url()));
                                FSLoginResult.save(data);
                                cancelLoginInternal();
                                if (callback != null) callback.onSuccess(data);
                                else showH5Internal();
                            } else {
                                // 失败仍在同一弹窗内重试；只有关闭/成功才结束 Bridge Promise。
                                Toaster.show("登录失败，请检查输入后重试");
                            }
                        }, error -> {
                            if (loginAttempt != attempt || destroyed || !isActivityUsable(activity))
                                return;
                            attempt.submitting = false;
                            if (loading != null) loading.dismiss();
                            Toaster.show("登录请求失败，请稍后重试");
                        });
            });
            attempt.dialog.setOnDismissListener(ignored -> {
                if (loginAttempt != attempt) return;
                cancelLoginInternal();
                if (callback != null) callback.onCancelled();
            });
            attempt.dialog.show();
        } catch (RuntimeException failure) {
            cancelLoginInternal();
            if (callback != null) callback.onFailure("ACTIVITY_UNAVAILABLE");
            FoxSdkDiagnostics.reportFailure(activity, "h5_login", "dialog_show_failed", failure);
        }
    }

    private void showH5Internal() {
        showH5Internal(resolveH5HomeUrl());
    }

    private void showH5Internal(String homeUrl) {
        Activity activity = activityReference.get();
        if (destroyed || !isActivityUsable(activity))
            return;
        if (homeUrl == null) {
            FoxSdkLogger.e(H5_LOG_TAG, "showH5Internal rejected: login response h5Url is missing or invalid");
            FoxSdkDiagnostics.record("h5_overlay_show_failed", activity, "invalid_login_h5_url");
            Toaster.show("服务暂不可用，请稍后重试");
            return;
        }
        FoxSdkLogger.d(H5_LOG_TAG, "showH5Internal: loginH5UrlAvailable=true, existingView=" + (h5View != null));
        try {
            WindowLifecycleControl.hideWindow(activity);
            removeHomeView();
            removePageView();
            if (h5View != null) {
                h5View.setVisibility(View.VISIBLE);
                h5View.bringToFront();
                return;
            }
            hostRoot = resolveHostRoot(activity);
            h5View = new FSH5OverlayView(activity, new FSH5OverlayView.Callback() {
                @Override
                public void onClose() {
                    hideInternal();
                }

                @Override
                public void onLogoutRequested(FSH5OverlayView.LogoutCompletion completion) {
                    showLogoutInternal(completion);
                }

                @Override
                public void onMiniProgramRequested(
                        String requestId,
                        String appName,
                        JSONObject params,
                        FSH5OverlayView.MiniProgramCompletion completion
                ) {
                    handleMiniProgramRequest(activity, requestId, appName, params, completion);
                }
            }, homeUrl);
            attachView(h5View);
            FoxSdkDiagnostics.record("h5_overlay_show", activity, "home");
            FoxSdkLogger.d(H5_LOG_TAG, "showH5Internal: overlay attached");
        } catch (Throwable throwable) {
            FoxSdkLogger.e(H5_LOG_TAG, "showH5Internal failed: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage(), throwable);
            FoxSdkDiagnostics.reportFailure(activity, "h5_home", "create_failed", throwable);
            removeH5View();
            showHomeInternal();
        }
    }

    /**
     * H5 首页只取登录接口持久化结果，不再接受宿主初始化配置。
     * 正式环境要求 HTTPS；HTTP 仅在 SDK 自身显式开启调试开关时允许。
     */
    private static String resolveH5HomeUrl() {
        FSLoginResult login = FSLoginResult.getInstance();
        String value = login == null ? null : login.getH5Url();
        return validateH5HomeUrl(value);
    }

    private static String validateH5HomeUrl(String value) {
        if (TextUtils.isEmpty(value) || value.length() > 4096) return null;
        try {
            Uri uri = Uri.parse(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (TextUtils.isEmpty(scheme) || TextUtils.isEmpty(host)
                    || uri.getUserInfo() != null || uri.getPort() == 0 || uri.getPort() > 65535) {
                return null;
            }
            boolean https = "https".equalsIgnoreCase(scheme);
            boolean debugHttp = "http".equalsIgnoreCase(scheme)
                    && WishFoxSdk.getConfig().isAllowInsecureH5();
            return https || debugHttp ? value.trim() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void showHomeInternal(FSHomeViewState preservedState) {
        Activity activity = activityReference.get();
        if (destroyed || !isActivityUsable(activity)) {
            return;
        }

        try {
            WindowLifecycleControl.hideWindow(activity);
            removePageView();
            if (homeView != null) {
                homeView.setVisibility(View.VISIBLE);
                homeView.bringToFront();
                homeView.requestFocus();
                FoxSdkDiagnostics.record("overlay_show_existing", activity, ROUTE_HOME);
                FoxSdkDiagnostics.putContext(activity, ROUTE_HOME, "shown");
                return;
            }

            hostRoot = resolveHostRoot(activity);
            FSHomeOverlayView view = new FSHomeOverlayView(
                    activity,
                    new FSHomeOverlayView.Callback() {
                        @Override
                        public void onCloseRequested() {
                            hide(activity);
                        }

                        @Override
                        public void onOpenPage(Page page) {
                            showPage(activity, page);
                        }

                        @Override
                        public void onOpenWeb(String url, boolean showTitle) {
                            showWeb(activity, url, showTitle);
                        }
                    },
                    preservedState
            );
            attachView(view);
            homeView = view;
            FoxSdkDiagnostics.record("overlay_show", activity, ROUTE_HOME);
            FoxSdkDiagnostics.putContext(activity, ROUTE_HOME, "shown");
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(activity, ROUTE_HOME, "create_failed", throwable);
            removeHomeView();
            startLegacyFallback(activity);
        }
    }

    private void showPageInternal(Page page) {
        Activity activity = activityReference.get();
        if (destroyed || !isActivityUsable(activity) || page == null) {
            return;
        }

        try {
            WindowLifecycleControl.hideWindow(activity);
            setHomeVisibility(false);
            removePageView();
            hostRoot = resolveHostRoot(activity);
            FSOverlayPageView view = createPageView(activity, page);
            attachView(view);
            pageView = view;
            currentPage = page;
            FoxSdkDiagnostics.record("overlay_page_show", activity, page.name());
            FoxSdkDiagnostics.putContext(activity, page.name(), "shown");
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(
                    activity,
                    page.name(),
                    "page_create_failed",
                    throwable
            );
            removePageView();
            showHomeInternal();
        }
    }

    private void showWebInternal(String url, String html, boolean showTitle, boolean showReport) {
        Activity activity = activityReference.get();
        if (destroyed || !isActivityUsable(activity)) {
            return;
        }
        try {
            webUrl = url;
            webHtml = html;
            webShowTitle = showTitle;
            webShowReport = showReport;
            WindowLifecycleControl.hideWindow(activity);
            setHomeVisibility(false);
            removePageView();
            hostRoot = resolveHostRoot(activity);
            FSWebOverlayView view = new FSWebOverlayView(
                    activity,
                    createPageCallback(activity),
                    url,
                    html,
                    showTitle,
                    showReport
            );
            attachView(view);
            pageView = view;
            currentPage = Page.WEB;
            FoxSdkDiagnostics.record("overlay_page_show", activity, Page.WEB.name());
            FoxSdkDiagnostics.putContext(activity, Page.WEB.name(), "shown");
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(activity, Page.WEB.name(), "page_create_failed", throwable);
            removePageView();
            showHomeInternal();
        }
    }

    private FSOverlayPageView createPageView(Activity activity, Page page) {
        FSOverlayPageView.Callback callback = createPageCallback(activity);
        switch (page) {
            case GAME_RECORD:
                return new FSGameRecordOverlayView(activity, callback);
            case RECHARGE_RECORD:
                return new FSRechargeRecordOverlayView(activity, callback);
            case MESSAGE:
                return new FSMessageOverlayView(activity, callback);
            case STARTER_PACK:
                return new FSStarterPackOverlayView(activity, callback);
            case WIN_FOX_COIN:
                return new FSWinFoxCoinOverlayView(activity, callback);
            case WEB:
            default:
                return new FSWebOverlayView(activity, callback, "", null, false, false);
        }
    }

    private FSOverlayPageView.Callback createPageCallback(Activity activity) {
        return new FSOverlayPageView.Callback() {
            @Override
            public void onCloseRequested() {
                showHomeInternal();
            }

            @Override
            public void onOpenPage(Page page) {
                showPage(activity, page);
            }

            @Override
            public void onOpenWeb(String url, boolean showTitle) {
                showWeb(activity, url, showTitle);
            }
        };
    }

    private void rebuildForConfiguration() {
        Activity activity = activityReference.get();
        if (destroyed || !isActivityUsable(activity)) {
            return;
        }

        if (h5View != null) {
            h5View.onConfigurationChanged();
            return;
        }

        boolean hadHome = homeView != null;
        FSHomeViewState homeState = hadHome ? homeView.getCurrentState() : null;
        Page page = currentPage;
        FSMessage detailMessage = null;
        boolean detailShown = false;
        if (page == Page.MESSAGE && pageView instanceof FSMessageOverlayView) {
            FSMessageOverlayView messageView = (FSMessageOverlayView) pageView;
            detailMessage = messageView.getDetailMessage();
            detailShown = messageView.isDetailShown();
        }

        removePageView();
        if (hadHome) {
            removeHomeView();
            showHomeInternal(homeState);
        }

        if (page == null) {
            return;
        }

        if (page == Page.WEB) {
            showWebInternal(webUrl, webHtml, webShowTitle, webShowReport);
        } else if (page == Page.MESSAGE) {
            showMessageInternal(detailMessage, detailShown);
        } else {
            showPageInternal(page);
        }
    }

    private void showMessageInternal(FSMessage detailMessage, boolean detailShown) {
        Activity activity = activityReference.get();
        if (destroyed || !isActivityUsable(activity)) {
            return;
        }
        try {
            WindowLifecycleControl.hideWindow(activity);
            setHomeVisibility(false);
            removePageView();
            hostRoot = resolveHostRoot(activity);
            FSMessageOverlayView view = new FSMessageOverlayView(
                    activity,
                    createPageCallback(activity),
                    detailMessage,
                    detailShown
            );
            attachView(view);
            pageView = view;
            currentPage = Page.MESSAGE;
            FoxSdkDiagnostics.record("overlay_page_show", activity, Page.MESSAGE.name());
            FoxSdkDiagnostics.putContext(activity, Page.MESSAGE.name(), "shown");
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(
                    activity,
                    Page.MESSAGE.name(),
                    "page_create_failed",
                    throwable
            );
            removePageView();
            showHomeInternal();
        }
    }

    private ViewGroup resolveHostRoot(Activity activity) {
        View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decor instanceof ViewGroup) {
            /*
             * 直接挂载到 DecorView，避免沉浸式或边到边宿主在横屏时
             * 把导航栏安全区留在 SDK 面板外侧。内容根节点可能已经被宿主
             * 窗口策略偏移，如果挂到内容根节点上，会导致 SDK 页面距离物理屏幕边缘
             * 多出几十像素。
             */
            return (ViewGroup) decor;
        }
        View content = activity.findViewById(android.R.id.content);
        if (content instanceof ViewGroup) {
            return (ViewGroup) content;
        }
        throw new IllegalStateException("Host Activity has no attachable content root");
    }

    private void attachView(View view) {
        if (view == null || hostRoot == null) {
            throw new IllegalStateException("SDK overlay has no host root");
        }
        view.setClickable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setZ(1000f);
        }
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        hostRoot.addView(view);
        view.bringToFront();
        view.requestFocus();
    }

    private void hideInternal() {
        Activity activity = activityReference.get();
        if (homeView != null || pageView != null) {
            FoxSdkDiagnostics.record(
                    "overlay_hide",
                    activity,
                    currentPage == null ? ROUTE_HOME : currentPage.name()
            );
            FoxSdkDiagnostics.putContext(
                    activity,
                    currentPage == null ? ROUTE_HOME : currentPage.name(),
                    "hidden"
            );
        }
        removeH5View();
        removeHomeView();
        removePageView();
        currentPage = null;
        webUrl = null;
        webHtml = null;
        webShowTitle = false;
        webShowReport = false;
        WindowLifecycleControl.showWindow(activity);
    }

    /**
     * H5 发起的登出必须由原生确认；确认后清理本地登录态并关闭所有 H5 页面。
     */
    private void showLogoutInternal(FSH5OverlayView.LogoutCompletion completion) {
        Activity activity = activityReference.get();
        if (completion == null) return;
        if (destroyed || !isActivityUsable(activity)) {
            completion.complete("ACTIVITY_UNAVAILABLE", null);
            return;
        }
        if (logoutAttempt != null) {
            completion.complete("BUSY", null);
            return;
        }
        LogoutAttempt attempt = new LogoutAttempt(completion);
        logoutAttempt = attempt;
        try {
            attempt.dialog = new FSAlertDialog.Builder(activity)
                    .setTitle("提示")
                    .setMessage("确定要退出登录吗？")
                    .setPositive("确定", () -> {
                        if (logoutAttempt != attempt || attempt.completed) return;
                        attempt.completed = true;
                        logoutAttempt = null;
                        String token = FSLoginResult.getTokenEd();
                        startServerLogout(activity, token);
                        clearLocalAuthState();
                        try {
                            JSONObject data = new JSONObject()
                                    .put("status", "anonymous")
                                    .put("user", JSONObject.NULL)
                                    .put("sessionMode", "short_token")
                                    .put("sessionStatus", "none")
                                    .put("sessionExpiresIn", 0);
                            completion.complete("OK", data);
                        } catch (JSONException ignored) {
                            completion.complete("OK", null);
                        }
                    })
                    .setNegative("取消", null)
                    .setOnDismissListener(() -> {
                        if (logoutAttempt != attempt || attempt.completed) return;
                        attempt.completed = true;
                        logoutAttempt = null;
                        completion.complete("USER_CANCELLED", null);
                    })
                    .build();
            attempt.dialog.show();
        } catch (RuntimeException failure) {
            logoutAttempt = null;
            completion.complete("ACTIVITY_UNAVAILABLE", null);
            FoxSdkDiagnostics.reportFailure(activity, "h5_logout", "dialog_show_failed", failure);
        }
    }

    /**
     * 接收 H5 的小程序启动请求。
     *
     * <p>小程序参数由原生统一接管，必要时由原生换取 encrypted URL Scheme 并拉起微信；
     * 耗时期间展示 loading，最终通过 completion 将结果回传给 H5。</p>
     */
    private void handleMiniProgramRequest(
            Activity activity,
            String requestId,
            String appName,
            JSONObject params,
            FSH5OverlayView.MiniProgramCompletion completion
    ) {
        FoxSdkLogger.d(H5_LOG_TAG, "miniProgram request received: requestId=" + requestId
                + ", appName=" + appName
                + ", paramCount=" + (params == null ? 0 : params.toString()));

        if (completion == null) return;
        if (params == null) {
            completion.complete("INVALID_ARGUMENT", null);
            return;
        }

        // completion 可能由同步分支、Rx 异步分支或异常分支触发，统一保证只回调一次；
        // 同时在回调前关闭 loading，避免 H5 已收到结果但 loading 仍遮挡页面。
        AtomicBoolean completionSent = new AtomicBoolean(false);
        FSH5OverlayView.MiniProgramCompletion finish = (code, data) -> {
            if (!completionSent.compareAndSet(false, true)) return;
            runOnMain(activity, () -> {
                dismissMiniProgramLoading();
                completion.complete(code, data);
            });
        };

        showMiniProgramLoading(activity);
        try {
            Map<String, Object> map = new Gson().fromJson(
                    params.toString(),
                    new TypeToken<Map<String, Object>>() {
                    }.getType()
            );
            if (map == null) {
                finish.complete("INVALID_ARGUMENT", null);
                return;
            }

            if ("xuyuanhu".equals(appName)
                    && FoxSdkUtils.isWishFoxInstalled(activity).blockingLast()) {
                // 如果是许愿狐小程序，则优先判断是否安装了许愿狐 APP；
                // 已安装时跳转 APP，否则继续走微信小程序分支。
                Gson gson = new Gson();
                FSGameSchemeData gameSchemeData = new FSGameSchemeData(
                        "wish_game",
                        map.get("taskNumber") == null ? "" : map.get("taskNumber").toString(),
                        map.get("typeName") == null ? "" : map.get("typeName").toString(),
                        true,
                        "",
                        WishFoxSdk.getConfig().getAppId(),
                        WishFoxSdk.getConfig().getChannelId()
                );
                String json = gson.toJson(gameSchemeData);
                FoxSdkAppJumpUtils.launchByDeepLink(
                        activity,
                        "sohugloba://app/game_sdk?gameData=" + json,
                        map.get("url") == null ? "https://www.wishfosx.com" : map.get("url").toString()
                );
                finish.complete("OK", miniProgramResult(requestId, "accepted_to_launch"));
            } else {
                Pair<Boolean, String> pair = FoxSdkWxPay.wXMiniProgramAllinpayPayment(activity, map);
                if (pair != null && Boolean.TRUE.equals(pair.first)) {
                    String scene = map.get("typeName") == null ? "payment" : (map.get("typeName").toString().equals("通用") || map.get("typeName").toString().equals("流量")) ? "Task" : map.get("typeName").toString().equals("服务") ? "Service" : "Wish";
                    startWechatForSchemeEncrypted(activity, pair.second, appName, scene, requestId, finish);
                } else {
                    FoxSdkAppJumpUtils.byWebOpenH5(activity,
                            map.get("url") == null ? "https://www.wishfosx.com" : map.get("url").toString());
                    finish.complete("OK", miniProgramResult(requestId, "accepted_to_launch"));
                }
            }
        } catch (Throwable failure) {
            FoxSdkLogger.e(H5_LOG_TAG, "miniProgram request failed: requestId=" + requestId,
                    failure);
            finish.complete("INTERNAL_ERROR",
                    miniProgramResult(requestId, "launch_failed"));
        }
    }

    private JSONObject miniProgramResult(String requestId, String status) {
        try {
            return new JSONObject().put("requestId", requestId).put("status", status);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private void showMiniProgramLoading(Activity activity) {
        dismissMiniProgramLoading();
        if (!isActivityUsable(activity)) return;
        try {
            miniProgramLoading = new FSLoadingDialog(activity);
            miniProgramLoading.setCancelable(false);
            miniProgramLoading.setCanceledOnTouchOutside(false);
            miniProgramLoading.setMessage("正在跳转...");
            miniProgramLoading.show();
        } catch (Throwable failure) {
            miniProgramLoading = null;
            FoxSdkLogger.e(H5_LOG_TAG, "miniProgram loading show failed", failure);
        }
    }

    private void dismissMiniProgramLoading() {
        if (miniProgramLoading == null) return;
        try {
            miniProgramLoading.dismiss();
        } catch (Throwable failure) {
            FoxSdkLogger.e(H5_LOG_TAG, "miniProgram loading dismiss failed", failure);
        } finally {
            miniProgramLoading = null;
        }
    }

    // 启动微信小程序
    private void startWechatForSchemeEncrypted(
            Activity activity,
            String query,
            String app_name,
            String scene,
            String requestId,
            FSH5OverlayView.MiniProgramCompletion completion
    ) {
        FoxSdkNetworkExecutor.execute(() ->
                FoxSdkRetrofitManager.getApiService().getWechatSchemeEncrypted(
                        query,
                        WishFoxSdk.getConfig().isWechatTest() ? "trial" : "release",
                        app_name,
                        scene
                ).blockingGet()
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        try {
                            String scheme = result.getData() == null ? null : result.getData().getScheme();
                            if (TextUtils.isEmpty(scheme)) {
                                Toaster.show("跳转失败");
                                completion.complete("SCHEME_REJECTED",
                                        miniProgramResult(requestId, "scheme_rejected"));
                                return;
                            }
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                            completion.complete("OK", miniProgramResult(requestId, "accepted_to_launch"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toaster.show("打开微信失败，请联系客服");
                            completion.complete("ACTIVITY_NOT_FOUND",
                                    miniProgramResult(requestId, "launch_failed"));
                        }
                    } else if (result.isError()) {
                        String errorMsg = result.getError() != null ? result.getError() : "跳转失败";
                        Toaster.show(errorMsg);
                        completion.complete("SCHEME_REJECTED",
                                miniProgramResult(requestId, "scheme_rejected"));
                    } else if (result.isEmpty()) {
                        Toaster.show("跳转失败");
                        completion.complete("SCHEME_REJECTED",
                                miniProgramResult(requestId, "scheme_rejected"));
                    } else {
                        Toaster.show("跳转失败");
                        completion.complete("SCHEME_REJECTED",
                                miniProgramResult(requestId, "scheme_rejected"));
                    }
                }, throwable -> {
                    String errorMsg = "网络请求失败";
                    if (throwable instanceof IOException) {
                        errorMsg = "网络连接失败，请检查网络";
                    } else if (throwable instanceof SocketTimeoutException) {
                        errorMsg = "网络连接超时，请重试";
                    } else if (throwable instanceof HttpException) {
                        errorMsg = "服务器错误，请稍后重试";
                    }
                    Toaster.show(errorMsg);
                    completion.complete("NETWORK_ERROR",
                            miniProgramResult(requestId, "launch_failed"));
                });
    }

    /**
     * 清理长期登录态、兼容旧存储键和所有进程内 H5 短 Token。
     */
    private void clearLocalAuthState() {
        FSUserProfile.clear();
        FSLoginResult.clear();
        FSCoinInfo.clear();
        FoxSdkSPUtils.getInstance().remove(FoxSdkConstant.AUTHORIZATION);
        // H5 短 Token 只存在各 FSH5AuthSession 的内存中；随后销毁整个 H5 Overlay，
        // 会调用每个 WebView bridge.auth.reset()，从而同步清空 expiresAt 和在途交换。
    }

    /**
     * 服务端登出是尽力而为，不阻塞 H5 响应和 Overlay 关闭；本地状态已经立即清理。
     */
    private void startServerLogout(Activity activity, String token) {
        try {
            if (TextUtils.isEmpty(token)) return;
            if (logoutRequest != null) logoutRequest.dispose();
            logoutRequest = FoxSdkRepositoryContainer.getHomeRepository().logout(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> logoutRequest = null,
                            error -> {
                                logoutRequest = null;
                                FoxSdkDiagnostics.record("h5_logout_server_failed", activity,
                                        error == null ? "unknown" : error.getClass().getSimpleName());
                            });
        } catch (RuntimeException failure) {
            FoxSdkDiagnostics.reportFailure(activity, "h5_logout", "server_logout_start_failed", failure);
        }
    }

    private void removeH5View() {
        dismissMiniProgramLoading();
        if (h5View == null) return;
        FSH5OverlayView view = h5View;
        h5View = null;
        try {
            view.destroy();
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(activityReference.get(), "h5_home", "destroy_failed", throwable);
        }
        removeFromParent(view);
    }

    private void removeHomeView() {
        if (homeView == null) {
            return;
        }
        FSHomeOverlayView view = homeView;
        homeView = null;
        try {
            view.destroy();
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(
                    activityReference.get(),
                    ROUTE_HOME,
                    "destroy_failed",
                    throwable
            );
        }
        removeFromParent(view);
    }

    private void setHomeVisibility(boolean visible) {
        if (homeView != null) {
            homeView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void removePageView() {
        if (pageView == null) {
            return;
        }
        FSOverlayPageView view = pageView;
        pageView = null;
        try {
            view.destroy();
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(
                    activityReference.get(),
                    currentPage == null ? "page" : currentPage.name(),
                    "page_destroy_failed",
                    throwable
            );
        }
        removeFromParent(view);
        currentPage = null;
    }

    private void removeFromParent(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    private void startLegacyFallback(Activity activity) {
        if (fallbackStarted || !isActivityUsable(activity)) {
            return;
        }
        fallbackStarted = true;
        try {
            FoxSdkDiagnostics.record("overlay_fallback_activity", activity, ROUTE_HOME);
            WindowLifecycleControl.hideWindow(activity);
            FSHomeActivity.startLegacyFallback(activity);
        } catch (Throwable throwable) {
            WindowLifecycleControl.showWindow(activity);
            FoxSdkDiagnostics.reportFailure(
                    activity,
                    ROUTE_HOME,
                    "fallback_start_failed",
                    throwable
            );
        }
    }

    private void destroyInternal() {
        destroyed = true;
        dismissMiniProgramLoading();
        if (logoutRequest != null) {
            logoutRequest.dispose();
            logoutRequest = null;
        }
        if (logoutAttempt != null) {
            LogoutAttempt pendingLogout = logoutAttempt;
            logoutAttempt = null;
            pendingLogout.completed = true;
            if (pendingLogout.dialog != null) pendingLogout.dialog.dismiss();
            pendingLogout.completion.complete("ACTIVITY_UNAVAILABLE", null);
        }
        LoginCallback pendingLogin = loginAttempt == null ? null : loginAttempt.callback;
        cancelLoginInternal();
        if (pendingLogin != null) pendingLogin.onFailure("ACTIVITY_UNAVAILABLE");
        removeH5View();
        removeHomeView();
        removePageView();
        hostRoot = null;
        currentPage = null;
        webUrl = null;
        webHtml = null;
        webShowTitle = false;
        webShowReport = false;
        FoxSdkDiagnostics.record("overlay_host_destroyed", activityReference.get(), ROUTE_HOME);
    }
}
