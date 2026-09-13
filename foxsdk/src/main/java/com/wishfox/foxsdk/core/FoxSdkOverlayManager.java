package com.wishfox.foxsdk.core;

import android.app.Activity;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.wishfox.foxsdk.ui.view.activity.FSHomeActivity;
import com.wishfox.foxsdk.data.model.entity.FSMessage;
import com.wishfox.foxsdk.ui.view.widgets.FSGameRecordOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSHomeOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSMessageOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSOverlayPageView;
import com.wishfox.foxsdk.ui.view.widgets.FSRechargeRecordOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSStarterPackOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSWebOverlayView;
import com.wishfox.foxsdk.ui.view.widgets.FSWinFoxCoinOverlayView;
import com.wishfox.foxsdk.ui.viewstate.FSHomeViewState;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Owns every SDK page rendered inside the host Activity.
 *
 * <p>The page stack is view based. Switching from the home page to a record,
 * message, WebView, gift or fox-coin page therefore never starts another
 * Activity and cannot put the Unity/Cocos Activity into {@code onPause}.</p>
 */
public final class FoxSdkOverlayManager {

    public enum Page {
        GAME_RECORD,
        RECHARGE_RECORD,
        MESSAGE,
        STARTER_PACK,
        WIN_FOX_COIN,
        WEB
    }

    private static final Map<Activity, WeakReference<FoxSdkOverlayManager>> INSTANCES =
            new WeakHashMap<>();
    private static final String ROUTE_HOME = "home";

    private final WeakReference<Activity> activityReference;
    private ViewGroup hostRoot;
    private FSHomeOverlayView homeView;
    private FSOverlayPageView pageView;
    private Page currentPage;
    private boolean destroyed;
    private boolean fallbackStarted;
    private String webUrl;
    private String webHtml;
    private boolean webShowTitle;
    private boolean webShowReport;

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
            getOrCreate(activity).showHomeInternal();
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
                (manager.homeView != null || manager.pageView != null) &&
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
     * Application-level configuration callbacks do not carry an Activity
     * instance. Snapshot the currently registered managers first, then route
     * each rebuild through its host Activity's main thread.
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
             * Attach to the DecorView so an edge-to-edge/immersive host does
             * not leave its landscape navigation-bar inset outside the SDK
             * panel. The content root can already be offset by the host
             * window policy, which would make every overlay start several
             * dozen pixels from the physical screen edge.
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
        removeHomeView();
        removePageView();
        currentPage = null;
        webUrl = null;
        webHtml = null;
        webShowTitle = false;
        webShowReport = false;
        WindowLifecycleControl.showWindow(activity);
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
