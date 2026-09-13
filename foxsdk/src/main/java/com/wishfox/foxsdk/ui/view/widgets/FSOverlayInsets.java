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
 * Applies system-bar and display-cutout insets to views hosted inside the
 * third-party Activity. The start safe-area view is deliberately kept out of
 * the horizontal ConstraintLayout weight chain; the page content receives a
 * dynamic start margin instead. This prevents a zero-width chain member from
 * collapsing the page and avoids a permanent 1dp landscape gap.
 */
final class FSOverlayInsets {

    private FSOverlayInsets() {
    }

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
     * Applies all four safe-area edges to a full-screen child such as the
     * SDK WebView page. System-bar insets are ignored while the corresponding
     * bar is hidden, but display-cutout insets are always retained.
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

    private static int resolveTopInset(
            Activity activity,
            WindowInsetsCompat insets,
            DisplayCutoutCompat cutout
    ) {
        int topInset = cutout == null ? 0 : cutout.getSafeInsetTop();

        /*
         * WindowInsetsCompat 1.3 (pulled by the SDK's AppCompat version)
         * exposes the legacy inset values but not visibility masks. Use the
         * platform visibility API on Android 11+ and the legacy fullscreen
         * flag on Android 7-10 so an immersive host does not get a phantom
         * status-bar height above every secondary-page title.
         */
        if (!isStatusBarHidden(activity, insets)) {
            topInset = Math.max(topInset, insets.getSystemWindowInsetTop());
        }
        return Math.max(0, topInset);
    }

    private static int resolveStartInset(
            Activity activity,
            WindowInsetsCompat insets,
            DisplayCutoutCompat cutout
    ) {
        int startInset = cutout == null ? 0 : cutout.getSafeInsetLeft();

        /*
         * A hidden navigation bar must not be treated as a permanent left
         * safe-area inset. This matters on immersive landscape devices where
         * legacy WindowInsets may still report the navigation bar width.
         * When navigation is visible, retain its inset for non-immersive hosts.
         */
        if (!isNavigationBarHidden(activity, insets)) {
            startInset = Math.max(startInset, insets.getSystemWindowInsetLeft());
        }
        return Math.max(0, startInset);
    }

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

    private static View getDecorView(Activity activity) {
        if (activity == null || activity.getWindow() == null) {
            return null;
        }
        return activity.getWindow().getDecorView();
    }
}
