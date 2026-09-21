package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.core.view.DisplayCutoutCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 为挂载在三方宿主 Activity 内的 SDK 页面应用系统栏和挖孔屏安全区。
 *
 * <p>左侧安全区占位 View 不参与 ConstraintLayout 的横向权重链，
 * 页面内容通过动态起始外边距偏移，避免零宽度链元素压缩页面，
 * 也避免横屏时出现固定 1dp 空隙。</p>
 */
final class FSOverlayInsets {

    /** 与原生 Overlay 页面使用相同规则计算出的窗口安全区快照。 */
    static final class Snapshot {
        final int left;
        final int top;
        final int right;
        final int bottom;
        final String navigationMode;

        Snapshot(int left, int top, int right, int bottom, String navigationMode) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.navigationMode = navigationMode;
        }
    }

    private FSOverlayInsets() {
    }

    /**
     * 为顶部安全区、左侧安全区和页面内容根节点应用当前窗口安全区。
     */
    static void apply(
            Activity activity,
            View host,
            View topSafeView,
            View startSafeView,
            View contentView
    ) {
        if (host == null || (topSafeView == null && startSafeView == null &&
                contentView == null)) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(host, (view, insets) -> {
            DisplayCutoutCompat cutout = insets.getDisplayCutout();

            int topInset = resolveTopInset(activity, insets, cutout);
            if (topSafeView != null) {
                topSafeView.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams params = topSafeView.getLayoutParams();
                if (params != null && params.height != topInset) {
                    params.height = topInset;
                    topSafeView.setLayoutParams(params);
                }
            }

            int startInset = resolveStartInset(activity, insets, cutout);
            if (startSafeView != null) {
                ViewGroup.LayoutParams params = startSafeView.getLayoutParams();
                if (params != null && params.width != startInset) {
                    params.width = startInset;
                    startSafeView.setLayoutParams(params);
                }
                startSafeView.setVisibility(
                        startInset > 0 ? View.VISIBLE : View.INVISIBLE
                );
                startSafeView.setAlpha(startInset > 0 ? 1f : 0f);
            }

            if (contentView != null &&
                    contentView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) contentView.getLayoutParams();
                if (params.getMarginStart() != startInset) {
                    params.setMarginStart(startInset);
                    contentView.setLayoutParams(params);
                }
            }

            return insets;
        });
        ViewCompat.requestApplyInsets(host);
    }

    /**
     * 为 WebView 这类全屏子页面应用四边安全区。
     * 对应系统栏隐藏时忽略系统栏安全区，但始终保留挖孔屏安全区。
     */
    static void applyToPadding(Activity activity, View host) {
        if (host == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(host, (view, insets) -> {
            DisplayCutoutCompat cutout = insets.getDisplayCutout();
            view.setPadding(
                    resolveStartInset(activity, insets, cutout),
                    resolveTopInset(activity, insets, cutout),
                    resolveEndInset(activity, insets, cutout),
                    resolveBottomInset(activity, insets, cutout)
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(host);
    }

    /**
     * 读取安全区但不修改 View。H5 Overlay 使用该结果透传给页面，避免原生 padding
     * 与 H5 CSS safe-area 同时应用造成双重留白。
     */
    static Snapshot snapshot(Activity activity, View host) {
        WindowInsetsCompat insets = host == null ? null : ViewCompat.getRootWindowInsets(host);
        DisplayCutoutCompat cutout = insets == null ? null : insets.getDisplayCutout();
        int left = insets == null ? 0 : resolveStartInset(activity, insets, cutout);
        int top = insets == null ? 0 : resolveTopInset(activity, insets, cutout);
        int right = insets == null ? 0 : resolveEndInset(activity, insets, cutout);
        int bottom = insets == null ? 0 : resolveBottomInset(activity, insets, cutout);
        String navigationMode = isNavigationBarHidden(activity, insets)
                ? "fullscreen" : "virtual_keys";
        return new Snapshot(left, top, right, bottom, navigationMode);
    }

    /**
     * 计算顶部需要预留的安全距离。
     */
    private static int resolveTopInset(
            Activity activity,
            WindowInsetsCompat insets,
            DisplayCutoutCompat cutout
    ) {
        int topInset = cutout == null ? 0 : cutout.getSafeInsetTop();

        /*
         * SDK 当前 AppCompat 版本间接使用的 WindowInsetsCompat 1.3
         * 只能拿到旧版安全区值，不能直接读取系统栏可见性。
         * Android 11 及以上使用平台可见性 API，Android 7-10 使用旧版全屏标记，
         * 避免沉浸式宿主在二级页标题上方出现一段假的状态栏高度。
         */
        if (!isStatusBarHidden(activity, insets)) {
            topInset = Math.max(topInset, insets.getSystemWindowInsetTop());
        }
        return Math.max(0, topInset);
    }

    /**
     * 计算起始方向需要预留的安全距离。
     */
    private static int resolveStartInset(
            Activity activity,
            WindowInsetsCompat insets,
            DisplayCutoutCompat cutout
    ) {
        int startInset = cutout == null ? 0 : cutout.getSafeInsetLeft();

        /*
         * 隐藏状态的导航栏不能被当成永久左侧安全区。
         * 横屏沉浸设备上，旧版 WindowInsets 仍可能上报导航栏宽度；
         * 只有导航栏可见时，才为非沉浸宿主保留这部分安全区。
         */
        if (!isNavigationBarHidden(activity, insets)) {
            startInset = Math.max(startInset, insets.getSystemWindowInsetLeft());
        }
        return Math.max(0, startInset);
    }

    /**
     * 计算结束方向需要预留的安全距离。
     */
    private static int resolveEndInset(
            Activity activity,
            WindowInsetsCompat insets,
            DisplayCutoutCompat cutout
    ) {
        int endInset = cutout == null ? 0 : cutout.getSafeInsetRight();
        if (!isNavigationBarHidden(activity, insets)) {
            endInset = Math.max(endInset, insets.getSystemWindowInsetRight());
        }
        return Math.max(0, endInset);
    }

    /**
     * 计算底部需要预留的安全距离。
     */
    private static int resolveBottomInset(
            Activity activity,
            WindowInsetsCompat insets,
            DisplayCutoutCompat cutout
    ) {
        int bottomInset = cutout == null ? 0 : cutout.getSafeInsetBottom();
        if (!isNavigationBarHidden(activity, insets)) {
            bottomInset = Math.max(bottomInset, insets.getSystemWindowInsetBottom());
        }
        return Math.max(0, bottomInset);
    }

    /**
     * 判断宿主当前是否处于隐藏状态栏的沉浸模式。
     */
    private static boolean isStatusBarHidden(
            Activity activity,
            WindowInsetsCompat insets
    ) {
        View decorView = getDecorView(activity);
        if (decorView != null &&
                (decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0) {
            return true;
        }
        if (activity != null && activity.getWindow() != null) {
            WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
            if (attributes != null &&
                    (attributes.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
                return true;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && insets != null) {
            WindowInsets platformInsets = insets.toWindowInsets();
            return platformInsets != null &&
                    !platformInsets.isVisible(WindowInsets.Type.statusBars());
        }
        return false;
    }

    /**
     * 判断宿主当前是否处于隐藏导航栏的沉浸模式。
     */
    private static boolean isNavigationBarHidden(
            Activity activity,
            WindowInsetsCompat insets
    ) {
        View decorView = getDecorView(activity);
        if (decorView != null &&
                (decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && insets != null) {
            WindowInsets platformInsets = insets.toWindowInsets();
            return platformInsets != null &&
                    !platformInsets.isVisible(WindowInsets.Type.navigationBars());
        }
        return false;
    }

    /**
     * 安全获取宿主 DecorView。
     */
    private static View getDecorView(Activity activity) {
        if (activity == null || activity.getWindow() == null) {
            return null;
        }
        return activity.getWindow().getDecorView();
    }
}
