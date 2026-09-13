package com.wishfox.foxsdk.ui.view.widgets;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.hjq.window.EasyWindow;
import com.hjq.window.OnWindowLifecycleCallback;
import com.hjq.window.OnWindowViewClickListener;
import com.hjq.window.draggable.BaseWindowDraggableRule;
import com.hjq.window.draggable.IWindowDraggableRule;
import com.hjq.window.draggable.SpringBackWindowDraggableRule;
import com.hjq.window.draggable.callback.OnSpringBackAnimCallback;
import com.hjq.window.draggable.callback.OnWindowDraggingCallback;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.core.FoxSdkOverlayManager;

import java.lang.ref.WeakReference;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年12月11日 14:23
 */
public final class FSSemiStealthWindow extends EasyWindow<FSSemiStealthWindow>
        implements OnWindowDraggingCallback, OnSpringBackAnimCallback,
        OnWindowViewClickListener<View>, OnWindowLifecycleCallback {

    private static final int NO_HALF_HIDE_GRAVITY = 0;
    private static final String POSITION_PREFERENCES_NAME =
            "WishFoxSdkFloatingWindowPosition";
    private static final String POSITION_KEY_PREFIX = "floating_ball.";

    private boolean mAnimatingFlag;
    private boolean mDraggingFlag;
    private boolean mInitialPositionPending;
    private int mHalfHideGravity = NO_HALF_HIDE_GRAVITY;
    private SavedPosition mRestoredPosition;
    private final WeakReference<Activity> hostActivity;

    public FSSemiStealthWindow(@NonNull Activity activity) {
        super(activity);
        hostActivity = new WeakReference<>(activity);
    }

    @Override
    protected void initWindow(@NonNull Context context) {
        super.initWindow(context);

        /*
         * EasyWindow supplies a default window animation (16973828). The
         * floating ball is recycled immediately before the SDK overlay is
         * attached, but WindowManager can still keep that window's exit
         * animation on screen. This makes the ball briefly cover the already
         * visible home page. The ball has its own drag spring-back animation,
         * so disable only the WindowManager enter/exit animation here.
         */
        setWindowAnim(0);

        setContentView(R.layout.fs_floating_view);

        mRestoredPosition = loadSavedPosition(context);
        if (mRestoredPosition == null) {
            setWindowLocation(0, 200);
        } else {
            setWindowLocation(mRestoredPosition.x, mRestoredPosition.y);
        }

        SpringBackWindowDraggableRule springBackWindowDraggableRule = new SpringBackWindowDraggableRule(
                SpringBackWindowDraggableRule.ORIENTATION_HORIZONTAL);
        springBackWindowDraggableRule.setAllowMoveToScreenSafeArea(!isLandscape(context));
        springBackWindowDraggableRule.setOnWindowDraggingCallback(this);
        springBackWindowDraggableRule.setOnSpringBackAnimCallback(this);
        setWindowDraggableRule(springBackWindowDraggableRule);
        configureFloatingWindowLayout();

        setOnClickListenerByView(android.R.id.icon, this);
        setOnWindowLifecycleCallback(this);
    }

    /**
     * 发送贴边显示任务
     */
    public void postStayEdgeRunnable() {
        cancelTask(mStayEdgeRunnable);
        sendTask(mStayEdgeRunnable, 3000);
    }

    private final Runnable mSettleInitialPositionRunnable = () -> {
        if (!mInitialPositionPending || !isShowing() || mAnimatingFlag || mDraggingFlag) {
            return;
        }
        settleInitialPosition();
    };

    private final Runnable mStayEdgeRunnable = () -> {
        if (mAnimatingFlag || mDraggingFlag) {
            return;
        }
        syncDragSafeAreaPolicy();

        if (isLeftShow()) {
            hideHalfView(Gravity.LEFT);
        } else {
            hideHalfView(Gravity.RIGHT);
        }
    };

    /**
     * 隐藏 View 一半显示
     */
    private void hideHalfView(int gravity) {
        View windowRootLayout = getWindowRootLayout();
        if (windowRootLayout == null) {
            return;
        }

        int viewWidth = getAvailableViewWidth(windowRootLayout);
        int viewHeight = getAvailableViewHeight(windowRootLayout);
        if (viewWidth <= 0 || viewHeight <= 0) {
            sendTask(() -> {
                if (!mAnimatingFlag && !mDraggingFlag && isShowing()) {
                    View retryRootLayout = getWindowRootLayout();
                    int retryWidth = getAvailableViewWidth(retryRootLayout);
                    int retryHeight = getAvailableViewHeight(retryRootLayout);
                    if (retryWidth > 0 && retryHeight > 0) {
                        hideHalfView(gravity);
                    }
                }
            }, 16);
            return;
        }

        syncDragSafeAreaPolicy();
        snapToPhysicalEdgeIfNeeded(gravity, viewWidth);

        windowRootLayout.setAlpha(0.5f);
        windowRootLayout.setTranslationY(0);
        Rect clipBounds = new Rect();
        switch (gravity) {
            case Gravity.LEFT:
                int leftOffset = viewWidth / 2;
                clipBounds.set(leftOffset, 0, viewWidth, viewHeight);
                windowRootLayout.setTranslationX(-leftOffset);
                windowRootLayout.setClipBounds(clipBounds);
                mHalfHideGravity = Gravity.LEFT;
                break;
            case Gravity.RIGHT:
                int rightOffset = viewWidth / 2;
                clipBounds.set(0, 0, viewWidth - rightOffset, viewHeight);
                windowRootLayout.setTranslationX(rightOffset);
                windowRootLayout.setClipBounds(clipBounds);
                mHalfHideGravity = Gravity.RIGHT;
                break;
            default:
                break;
        }
        saveCurrentPosition();
    }

    private void showFullView() {
        View rootLayout = getWindowRootLayout();
        if (rootLayout == null) {
            return;
        }
        syncDragSafeAreaPolicy();
        mHalfHideGravity = NO_HALF_HIDE_GRAVITY;
        rootLayout.setAlpha(1f);
        rootLayout.setTranslationX(0);
        rootLayout.setTranslationY(0);
        int viewWidth = getAvailableViewWidth(rootLayout);
        int viewHeight = getAvailableViewHeight(rootLayout);
        if (viewWidth > 0 && viewHeight > 0) {
            rootLayout.setClipBounds(new Rect(0, 0, viewWidth, viewHeight));
        } else {
            rootLayout.setClipBounds(null);
        }
    }

    /**
     * View 是否全屏显示
     */
    private boolean isFullShowView() {
        if (mHalfHideGravity != NO_HALF_HIDE_GRAVITY) {
            return false;
        }
        View view = getWindowRootLayout();
        if (view == null) {
            return true;
        }
        int viewWidth = getAvailableViewWidth(view);
        int viewHeight = getAvailableViewHeight(view);
        Rect clipBounds = view.getClipBounds();
        if (view.getTranslationX() != 0 || view.getTranslationY() != 0) {
            return false;
        }
        if (clipBounds == null) {
            return true;
        }
        if (viewWidth <= 0 || viewHeight <= 0) {
            return true;
        }
        return clipBounds.left == 0 && clipBounds.top == 0 &&
                clipBounds.right == viewWidth && clipBounds.bottom == viewHeight;
    }

    /**
     * 获取当前屏幕宽度
     */
    private int getScreenWidth() {
        Context context = getContext();
        if (context == null) {
            return 0;
        }
        Resources resources = context.getResources();
        if (resources == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        if (displayMetrics == null) {
            return 0;
        }
        return displayMetrics.widthPixels;
    }

    /**
     * 悬浮球是否靠左显示
     */
    private boolean isLeftShow() {
        WindowManager.LayoutParams windowParams = getWindowParams();
        int screenWidth = getScreenWidth();
        if (windowParams == null || screenWidth <= 0) {
            return true;
        }
        return (windowParams.x + getWindowViewWidth() / 2f) < screenWidth / 2f;
    }

    @Override
    public void onWindowDraggingStart(@NonNull EasyWindow<?> easyWindow) {
        mInitialPositionPending = false;
        cancelTask(mSettleInitialPositionRunnable);
        syncDragSafeAreaPolicy();
        cancelTask(mStayEdgeRunnable);
        mDraggingFlag = true;
        refreshDragMetrics();
        if (!isFullShowView()) {
            showFullView();
        }
    }

    @Override
    public void onWindowDraggingStop(@NonNull EasyWindow<?> easyWindow) {
        mDraggingFlag = false;
        saveCurrentPosition();
    }

    @Override
    public void onSpringBackAnimationStart(@NonNull EasyWindow<?> easyWindow, @NonNull Animator animator) {
        syncDragSafeAreaPolicy();
        refreshDragMetrics();
        mAnimatingFlag = true;
    }

    @Override
    public void onSpringBackAnimationEnd(@NonNull EasyWindow<?> easyWindow, @NonNull Animator animator) {
        syncDragSafeAreaPolicy();
        refreshDragMetrics();
        mAnimatingFlag = false;
        settleInitialPosition();
        if (isLandscape()) {
            View rootLayout = getWindowRootLayout();
            snapToPhysicalEdgeIfNeeded(
                    isLeftShow() ? Gravity.LEFT : Gravity.RIGHT,
                    getAvailableViewWidth(rootLayout)
            );
        }
        saveCurrentPosition();
        postStayEdgeRunnable();
    }

    @Override
    public void onClick(@NonNull EasyWindow<?> easyWindow, @NonNull View view) {
        settleInitialPosition();
        syncDragSafeAreaPolicy();
        if (!isFullShowView()) {
            showFullView();
            postStayEdgeRunnable();
            return;
        }
        Activity activity = hostActivity.get();
        cancelTask(mStayEdgeRunnable);
        FoxSdkOverlayManager.show(activity);
    }

    /**
     * Keep the root lookup in one place so the floating ball remains resilient
     * when EasyWindow returns a null view during teardown.
     */
    private View getWindowRootLayout() {
        View rootLayout = getRootLayout();
        if (rootLayout != null) {
            return rootLayout;
        }
        return getContentView();
    }

    private int getAvailableViewWidth(View view) {
        int viewWidth = view == null ? 0 : view.getWidth();
        return viewWidth > 0 ? viewWidth : getWindowViewWidth();
    }

    private int getAvailableViewHeight(View view) {
        int viewHeight = view == null ? 0 : view.getHeight();
        return viewHeight > 0 ? viewHeight : getWindowViewHeight();
    }

    private void syncDragSafeAreaPolicy() {
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule != null) {
            /*
             * Landscape games are usually immersive. On some devices EasyWindow
             * reports a non-zero left safe inset even when the host has hidden
             * the navigation/status bars. If the floating ball is clamped to
             * that inset, it cannot reach the physical edge and every click is
             * misread as a request to "show full" again. Keep portrait behavior
             * conservative, but let landscape drag/spring-back use the real edge.
            */
            windowDraggableRule.setAllowMoveToScreenSafeArea(isLandscape());
        }
        if (isLandscape()) {
            addWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            removeWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    private void configureFloatingWindowLayout() {
        /*
         * EasyWindow copies the host's current system-bar flags in its
         * Activity constructor. A host may apply immersive mode a little later
         * (for example from onWindowFocusChanged), while this floating window
         * is created from onActivityCreated. Explicitly make the floating
         * window edge-to-edge so its visible display frame starts at physical
         * screen coordinate 0 on the first show as well.
         */
        int systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            systemUiVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            systemUiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        setSystemUiVisibility(systemUiVisibility);
        addWindowFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        addWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        if (isLandscape()) {
            addWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            removeWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setLayoutInDisplayCutoutMode(
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            );
        }
    }

    private void refreshDragMetrics() {
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule instanceof BaseWindowDraggableRule) {
            /*
             * BaseWindowDraggableRule caches the visible display frame at
             * start(). Refresh it after WindowManager has attached the window;
             * this is essential when the host applies immersive flags after
             * Activity creation.
             */
            ((BaseWindowDraggableRule) windowDraggableRule).refreshWindowInfo();
            ((BaseWindowDraggableRule) windowDraggableRule).refreshScreenPhysicalSize();
        }
    }

    private void settleInitialPosition() {
        if (!mInitialPositionPending || !isShowing() || mAnimatingFlag || mDraggingFlag) {
            return;
        }
        refreshDragMetrics();
        syncDragSafeAreaPolicy();
        if (isLandscape()) {
            alignToInitialLandscapeEdge();
        }
        mInitialPositionPending = false;
    }

    private void snapToPhysicalEdgeIfNeeded(int gravity, int viewWidth) {
        if (!isLandscape()) {
            return;
        }
        refreshDragMetrics();
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        WindowManager.LayoutParams windowParams = getWindowParams();
        if (windowDraggableRule == null || windowParams == null) {
            return;
        }
        int targetX = windowParams.x;
        if (gravity == Gravity.LEFT) {
            targetX = -getScreenInvisibleWidth();
        } else if (gravity == Gravity.RIGHT) {
            int screenWidth = getScreenWidth();
            if (screenWidth > 0 && viewWidth > 0) {
                targetX = Math.max(0, screenWidth - viewWidth);
            }
        }
        if (targetX != windowParams.x) {
            windowDraggableRule.updateLocation(targetX, windowParams.y, true);
        }
    }

    private void alignToInitialLandscapeEdge() {
        if (!isLandscape()) {
            return;
        }
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        WindowManager.LayoutParams windowParams = getWindowParams();
        if (windowDraggableRule == null || windowParams == null) {
            return;
        }
        refreshDragMetrics();
        int targetX = -getScreenInvisibleWidth();
        if (windowParams.x != targetX) {
            windowDraggableRule.updateLocation(targetX, windowParams.y, true);
        }
    }

    private int getScreenInvisibleWidth() {
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule == null) {
            return 0;
        }
        return Math.max(0, windowDraggableRule.getScreenInvisibleWidth());
    }

    private boolean isLandscape() {
        return isLandscape(getContext());
    }

    private boolean isLandscape(Context context) {
        return context != null &&
                context.getResources() != null &&
                context.getResources().getConfiguration() != null &&
                context.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * {@link OnWindowLifecycleCallback}
     */

    @Override
    public void onWindowShow(@NonNull EasyWindow<?> easyWindow) {
        configureFloatingWindowLayout();
        syncDragSafeAreaPolicy();
        refreshDragMetrics();
        if (mRestoredPosition != null) {
            restorePositionWithinCurrentBounds(mRestoredPosition);
        }
        mInitialPositionPending = isLandscape() && mRestoredPosition == null;
        showFullView();
        if (mInitialPositionPending) {
            alignToInitialLandscapeEdge();
        }
        cancelTask(mSettleInitialPositionRunnable);
        sendTask(mSettleInitialPositionRunnable, 120);
        postStayEdgeRunnable();
    }

    @Override
    public void onWindowRecycle(@NonNull EasyWindow<?> easyWindow) {
        saveCurrentPosition();
    }

    private void restorePositionWithinCurrentBounds(@NonNull SavedPosition position) {
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule == null) {
            return;
        }

        int targetX = position.x;
        int targetY = position.y;
        if (windowDraggableRule instanceof BaseWindowDraggableRule) {
            BaseWindowDraggableRule baseWindowDraggableRule =
                    (BaseWindowDraggableRule) windowDraggableRule;
            int screenWidth = baseWindowDraggableRule.getScreenWidth();
            int screenHeight = baseWindowDraggableRule.getScreenHeight();
            int viewWidth = getWindowViewWidth();
            int viewHeight = getWindowViewHeight();
            if (screenWidth > 0 && viewWidth > 0) {
                int minX = -Math.max(0, baseWindowDraggableRule.getScreenInvisibleWidth());
                int maxX = Math.max(minX, screenWidth - viewWidth);
                targetX = clamp(position.x, minX, maxX);
            }
            if (screenHeight > 0 && viewHeight > 0) {
                int minY = -Math.max(0, baseWindowDraggableRule.getScreenInvisibleHeight());
                int maxY = Math.max(minY, screenHeight - viewHeight);
                targetY = clamp(position.y, minY, maxY);
            }
        }
        windowDraggableRule.updateLocation(targetX, targetY, true);
        if (targetX != position.x || targetY != position.y) {
            saveCurrentPosition();
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private void saveCurrentPosition() {
        Activity activity = hostActivity.get();
        WindowManager.LayoutParams windowParams = getWindowParams();
        if (activity == null || windowParams == null) {
            return;
        }

        try {
            SharedPreferences preferences = activity.getApplicationContext()
                    .getSharedPreferences(POSITION_PREFERENCES_NAME, Context.MODE_PRIVATE);
            String keyPrefix = getPositionKeyPrefix(activity);
            preferences.edit()
                    .putInt(keyPrefix + ".x", windowParams.x)
                    .putInt(keyPrefix + ".y", windowParams.y)
                    .apply();
        } catch (Throwable ignored) {
            // Position persistence must never affect the floating window lifecycle.
        }
    }

    private SavedPosition loadSavedPosition(Context context) {
        if (!(context instanceof Activity)) {
            return null;
        }
        try {
            Activity activity = (Activity) context;
            SharedPreferences preferences = activity.getApplicationContext()
                    .getSharedPreferences(POSITION_PREFERENCES_NAME, Context.MODE_PRIVATE);
            String keyPrefix = getPositionKeyPrefix(activity);
            String xKey = keyPrefix + ".x";
            String yKey = keyPrefix + ".y";
            if (!preferences.contains(xKey) || !preferences.contains(yKey)) {
                return null;
            }
            return new SavedPosition(
                    preferences.getInt(xKey, 0),
                    preferences.getInt(yKey, 200)
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String getPositionKeyPrefix(Activity activity) {
        return POSITION_KEY_PREFIX
                + activity.getClass().getName()
                + "."
                + (isLandscape(activity) ? "landscape" : "portrait");
    }

    private static final class SavedPosition {
        private final int x;
        private final int y;

        private SavedPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
