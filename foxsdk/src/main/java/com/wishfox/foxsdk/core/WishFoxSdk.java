package com.wishfox.foxsdk.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.hjq.toast.Toaster;
//import com.petterp.floatingx.FloatingX;
//import com.petterp.floatingx.assist.FxDisplayMode;
//import com.petterp.floatingx.assist.FxScopeType;
//import com.petterp.floatingx.assist.helper.FxAppHelper;
//import com.petterp.floatingx.listener.IFxTouchListener;
//import com.petterp.floatingx.view.IFxInternalHelper;
import com.tencent.bugly.crashreport.CrashReport;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.ui.view.activity.FSHomeActivity;
import com.wishfox.foxsdk.ui.view.widgets.FSSemiStealthWindow;
import com.wishfox.foxsdk.utils.FoxSdkCommonExt;
import com.wishfox.foxsdk.utils.FoxSdkLogger;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;
import com.wishfox.foxsdk.utils.FSFloatImageManager;
import com.wishfox.foxsdk.data.model.entity.FSFloatIcon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:23
 */
@SuppressLint("StaticFieldLeak")
public class WishFoxSdk {

    private static final String TAG = "FoxSdk[Init]";
    private static boolean isInitialized = false;
    private static FoxSdkConfig config;
    private static Context context;
    private static Disposable floatImageDisposable;

    private static boolean floatMove = false;
    private static boolean floatActive = true;

    public static void setFloatActive(boolean floatActive) {
        WishFoxSdk.floatActive = floatActive;
        if (!floatActive) {
            WindowLifecycleControl.hideAllWindows();
        }
    }

    /** SDK 原生弹窗显示前隐藏悬浮球，避免弹窗与悬浮球同时可见。 */
    public static void hideFloatingWindow(@Nullable Activity activity) {
        if (!isInitialized || activity == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            WindowLifecycleControl.hideWindow(activity);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> WindowLifecycleControl.hideWindow(activity));
        }
    }

    /** SDK 原生弹窗关闭后恢复悬浮球；恢复时会重新启动半隐计时。 */
    public static void showFloatingWindow(@Nullable Activity activity) {
        if (!isInitialized || activity == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            WindowLifecycleControl.showWindow(activity);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> WindowLifecycleControl.showWindow(activity));
        }
    }

    static boolean isFloatActive() {
        return floatActive;
    }

    /**
     * 初始化SDK
     * 需要在Application中进行
     */
    public static void initialize(Context context, FoxSdkConfig config) {
        if (!(context instanceof Application)) {
            throw new IllegalStateException("WishFoxSdk 初始化需要在Application中进行");
        }

        WishFoxSdk.context = context.getApplicationContext();
        WishFoxSdk.config = config;
        WishFoxSdk.isInitialized = true;

        FoxSdkLogger.setDebug(config.isEnableLog());

        // 初始化网络组件
        FoxSdkRetrofitManager.initialize(config);

        // 初始化Toast工具
        Toaster.init((Application) context);

        // 初始化bugly
        CrashReport.initCrashReport(
                context,
                FoxSdkUtils.BUGLY_APPID,
                config.isEnableLog()
        );

        // 七鱼客服不再续费，客服 SDK 初始化已停用
//        QiyukfHelper.getInstance().init(context, FoxSdkBaseMviActivity.class);
//        QiyukfHelper.getInstance().initKFSDK();

        // 初始化悬浮窗
//        initFloatingWindow(context);
        WindowLifecycleControl.with((Application) context);

        // 悬浮球图片属于 SDK 全局资源，不依赖旧版 Home 首页；H5 首页场景也需要在
        // SDK 初始化完成后立即刷新，失败时由 FSFloatImageManager 保留旧缓存/预置图。
        refreshFloatImage();

        if (config.isEnableLog()) {
            FoxSdkLogger.d(TAG, "WishFoxSDK 初始化成功！");
        }
    }

    /** 初始化阶段获取悬浮球图片并刷新本地缓存。重复初始化时取消上一次请求。 */
    private static void refreshFloatImage() {
        if (floatImageDisposable != null) {
            floatImageDisposable.dispose();
        }
        floatImageDisposable = FoxSdkRetrofitManager.getApiService().getFloatImage()
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    FSFloatIcon icon = response != null && response.isSuccess()
                            ? response.getData() : null;
                    if (config.isEnableLog()) {
                        FoxSdkLogger.d(TAG, "悬浮球图片接口响应: code="
                                + (response == null ? "null" : response.getCode())
                                + ", version=" + (icon == null ? "null" : icon.getVersion())
                                + ", hasUrl=" + (icon != null && icon.getFloatIcon() != null));
                    }
                    String imageUrl = icon == null ? null : icon.getFloatIcon();
                    File cache = FSFloatImageManager.refreshCache(
                            context, imageUrl, icon == null ? null : icon.getVersion());
                    if (cache != null) {
                        // 请求/下载在线程池完成，View/Glide 刷新必须回到主线程；否则已显示的
                        // 悬浮球会继续保留创建时的旧图，直到下次 Activity 重建。
                        new Handler(Looper.getMainLooper())
                                .post(WindowLifecycleControl::refreshFloatImages);
                    }
                }, throwable -> {
                    // 请求或下载失败时保留上一次成功的缓存，由悬浮球加载时回退到预置图片。
                });
    }

    /**
     * 初始化悬浮窗
     */
    private static void initFloatingWindow(Context context) {
//        FxAppHelper helper = new FxAppHelper.Builder()
//                .setTag(FoxSdkUtils.FloatXTag)
//                .setContext(context)
//                .setLayout(R.layout.fs_floating_view)
//                .setOffsetXY(0, FoxSdkCommonExt.dp2px(context, config.getFloatXxOffset()))
//                .setScopeType(FxScopeType.APP)
//                .setDisplayMode(FxDisplayMode.Normal)
////                .setEnableAnimation(true)
//                .addInstallBlackClass(
//                        "com.wishfox.foxsdk.ui.view.activity.FSHomeActivity",
//                        "com.wishfox.foxsdk.ui.view.activity.FSStarterPackActivity",
//                        "com.wishfox.foxsdk.ui.view.activity.FSWinFoxCoinActivity",
//                        "com.wishfox.foxsdk.ui.view.activity.FSRechargeRecordActivity",
//                        "com.wishfox.foxsdk.ui.view.activity.FSGameRecordActivity",
//                        "com.wishfox.foxsdk.ui.view.activity.FSMessageActivity",
//                        "com.wishfox.foxsdk.ui.view.activity.FSWebActivity"
//                )
//                .setTouchListener(new IFxTouchListener() {
//                    @Override
//                    public void onDown() {
//                        View floatView = FloatingX.control(FoxSdkUtils.FloatXTag).getView();
//                        if (floatView != null)
//                            floatView.postDelayed(() -> {
//                                if (!floatMove && floatView != null && floatView.getContext() != null) {
//                                    Intent intent = new Intent(floatView.getContext(), FSHomeActivity.class);
//                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    context.startActivity(intent);
//                                }
//                            }, 350);
//                    }
//
//                    @Override
//                    public void onUp() {
//                        View floatView = FloatingX.control(FoxSdkUtils.FloatXTag).getView();
//                        if (floatView != null)
//                            floatView.postDelayed(() -> floatMove = false, 150);
//                    }
//
//                    @Override
//                    public void onDragIng(@NotNull MotionEvent motionEvent, float v, float v1) {
//                        floatMove = true;
//                    }
//
//                    @Override
//                    public boolean onTouch(@NotNull MotionEvent motionEvent, @Nullable IFxInternalHelper iFxInternalHelper) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean onInterceptTouchEvent(@NotNull MotionEvent motionEvent, @Nullable IFxInternalHelper iFxInternalHelper) {
//                        return true;
//                    }
//                })
//                .setOnClickListener(v -> {
//                    if (v != null && v.getContext() != null && !floatMove) {
//                        FoxSdkViewExt.setOnClickListener(v, (cv) -> {
////                            if (floatActive) {
//                            Intent intent = new Intent(v.getContext(), FSHomeActivity.class);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            context.startActivity(intent);
////
////                                if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null) {
////                                    FloatingX.control(FoxSdkUtils.FloatXTag).getView().postDelayed(() -> {
////                                        FloatingX.configControl(FoxSdkUtils.FloatXTag).setEnableHalfHide(true, getConfig().getFloatXScale());
////                                        if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null)
////                                            FloatingX.control(FoxSdkUtils.FloatXTag).getView().setAlpha(0.5f);
////                                        floatActive = false;
////                                    }, 1500);
////                                }
////                            } else {
////                                FloatingX.configControl(FoxSdkUtils.FloatXTag).setEnableHalfHide(false);
////                                if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null)
////                                    FloatingX.control(FoxSdkUtils.FloatXTag).getView().setAlpha(1f);
////                                if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null) {
////                                    FloatingX.control(FoxSdkUtils.FloatXTag).getView().postDelayed(() -> {
////                                        floatActive = true;
////                                        if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null) {
////                                            FloatingX.control(FoxSdkUtils.FloatXTag).getView().postDelayed(() -> {
////                                                FloatingX.configControl(FoxSdkUtils.FloatXTag).setEnableHalfHide(true, getConfig().getFloatXScale());
////                                                if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null)
////                                                    FloatingX.control(FoxSdkUtils.FloatXTag).getView().setAlpha(0.5f);
////                                                floatActive = false;
////                                            }, 1500);
////                                        }
////                                    }, 150);
////                                } else {
////                                    floatActive = true;
////                                }
////                            }
//                        });
//                    }
//                })
//                .build();
//        FloatingX.install(helper).show();
    }

    /**
     * 检查SDK是否已初始化
     */
    public static void requireInitialized() {
        if (!isInitialized) {
            throw new IllegalStateException("必须要先初始化WishFoxSDK. 初始化方法：WishFoxSDK.initialize()");
        }
    }

    /**
     * 获取配置信息
     */
    public static FoxSdkConfig getConfig() {
        requireInitialized();
        return config;
    }

    /**
     * 获取上下文
     */
    public static Context getContext() {
        requireInitialized();
        return context;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
}

final class WindowLifecycleControl
        implements Application.ActivityLifecycleCallbacks, ComponentCallbacks {

    private static final String SDK_PACKAGE_PREFIX = "com.wishfox.foxsdk.";
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Map<Activity, WeakReference<FSSemiStealthWindow>> WINDOWS = new WeakHashMap<>();

    static void with(Application application) {
        if (application != null && REGISTERED.compareAndSet(false, true)) {
            WindowLifecycleControl callbacks = new WindowLifecycleControl();
            application.registerActivityLifecycleCallbacks(callbacks);
            application.registerComponentCallbacks(callbacks);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @androidx.annotation.Nullable Bundle savedInstanceState) {
        if (isSdkActivity(activity) || activity instanceof FoxSdkBaseMviActivity ||
                !WishFoxSdk.isFloatActive()) {
            return;
        }
        showWindow(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        FoxSdkOverlayManager.onHostResumed(activity);
        if (!isSdkActivity(activity) &&
                !(activity instanceof FoxSdkBaseMviActivity) &&
                WishFoxSdk.isFloatActive()) {
            showWindow(activity);
        }
        if (FoxSdkOverlayManager.isShowing(activity)) {
            FoxSdkDiagnostics.record("host_resumed_with_overlay", activity, null);
            FoxSdkDiagnostics.putContext(activity, "home", "host_resumed");
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        FoxSdkOverlayManager.onHostPaused(activity);
        if (FoxSdkOverlayManager.isShowing(activity)) {
            FoxSdkDiagnostics.record("host_paused_with_overlay", activity, null);
            FoxSdkDiagnostics.putContext(activity, "home", "host_paused");
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        FoxSdkOverlayManager.onHostStopped(activity);
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        FoxSdkOverlayManager.onActivityDestroyed(activity);

        FSSemiStealthWindow window = null;
        synchronized (WINDOWS) {
            WeakReference<FSSemiStealthWindow> reference = WINDOWS.remove(activity);
            if (reference != null) {
                window = reference.get();
            }
        }
        if (window != null) {
            try {
                window.cancel();
            } catch (Throwable throwable) {
                FoxSdkDiagnostics.reportFailure(activity, "floating_ball", "cancel_failed", throwable);
            }
            try {
                window.recycle();
            } catch (Throwable throwable) {
                FoxSdkDiagnostics.reportFailure(activity, "floating_ball", "recycle_failed", throwable);
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        FoxSdkOverlayManager.onConfigurationChanged();
    }

    @Override
    public void onLowMemory() {
        // 进程级内存策略由宿主应用负责，SDK 不在这里主动释放宿主资源。
    }

    static void hideWindow(Activity activity) {
        if (activity == null) {
            return;
        }

        FSSemiStealthWindow window = null;
        synchronized (WINDOWS) {
            WeakReference<FSSemiStealthWindow> reference = WINDOWS.remove(activity);
            if (reference != null) {
                window = reference.get();
            }
        }
        if (window != null) {
            try {
                window.recycle();
                FoxSdkDiagnostics.record("float_hide", activity, null);
            } catch (Throwable throwable) {
                FoxSdkDiagnostics.reportFailure(activity, "floating_ball", "hide_failed", throwable);
            }
        }
    }

    static void showWindow(Activity activity) {
        if (activity == null ||
                !isActivityUsable(activity) ||
                isSdkActivity(activity) ||
                activity instanceof FoxSdkBaseMviActivity ||
                FoxSdkOverlayManager.isShowing(activity) ||
                !WishFoxSdk.isFloatActive()) {
            return;
        }
        synchronized (WINDOWS) {
            WeakReference<FSSemiStealthWindow> reference = WINDOWS.get(activity);
            if (reference != null && reference.get() != null) {
                return;
            }
        }

        try {
            FSSemiStealthWindow window = new FSSemiStealthWindow(activity);
            window.show();
            synchronized (WINDOWS) {
                WINDOWS.put(activity, new WeakReference<>(window));
            }
            FoxSdkDiagnostics.record("float_show", activity, null);
        } catch (Throwable throwable) {
            FoxSdkDiagnostics.reportFailure(activity, "floating_ball", "show_failed", throwable);
        }
    }

    static void hideAllWindows() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(WindowLifecycleControl::hideAllWindows);
            return;
        }
        List<FSSemiStealthWindow> windows = new ArrayList<>();
        synchronized (WINDOWS) {
            for (WeakReference<FSSemiStealthWindow> reference : WINDOWS.values()) {
                FSSemiStealthWindow window = reference == null ? null : reference.get();
                if (window != null) {
                    windows.add(window);
                }
            }
            WINDOWS.clear();
        }
        for (FSSemiStealthWindow window : windows) {
            try {
                window.recycle();
            } catch (Throwable throwable) {
                FoxSdkDiagnostics.reportFailure(null, "floating_ball", "hide_all_failed", throwable);
            }
        }
    }

    /** 在主线程重新加载当前存活悬浮球的图片缓存。 */
    static void refreshFloatImages() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(WindowLifecycleControl::refreshFloatImages);
            return;
        }
        List<FSSemiStealthWindow> windows = new ArrayList<>();
        synchronized (WINDOWS) {
            for (WeakReference<FSSemiStealthWindow> reference : WINDOWS.values()) {
                FSSemiStealthWindow window = reference == null ? null : reference.get();
                if (window != null) windows.add(window);
            }
        }
        for (FSSemiStealthWindow window : windows) {
            try {
                window.reloadFloatImage();
            } catch (Throwable throwable) {
                FoxSdkDiagnostics.reportFailure(null, "floating_ball", "image_reload_failed", throwable);
            }
        }
    }

    private static boolean isSdkActivity(Activity activity) {
        String className = activity == null ? "" : activity.getClass().getName();
        return className.startsWith(SDK_PACKAGE_PREFIX);
    }

    private static boolean isActivityUsable(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed();
    }
}
