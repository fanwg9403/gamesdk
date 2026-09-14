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
import android.widget.ImageView;

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
import com.wishfox.foxsdk.utils.FSFloatImageManager;

import java.lang.ref.WeakReference;

/**
 * 主要功能: SDK 半隐藏悬浮球窗口。
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

    /**
     * 创建绑定宿主 Activity 的悬浮球窗口。
     */
    public FSSemiStealthWindow(@NonNull Activity activity) {
        super(activity);
        hostActivity = new WeakReference<>(activity);
    }

    /**
     * 初始化悬浮球布局、拖拽规则和窗口参数。
     */
    @Override
    protected void initWindow(@NonNull Context context) {
        super.initWindow(context);

        /*
         * EasyWindow 默认会设置窗口动画 16973828。
         * 悬浮球在 SDK 首页挂载前会立即 recycle，但 WindowManager 仍可能继续播放
         * 这个窗口的退出动画，导致悬浮球短暂覆盖在已经展示的首页上。
         * 悬浮球自身拖拽回弹动画由拖拽规则负责，这里只关闭 WindowManager 进出场动画。
         */
        setWindowAnim(0);

        setContentView(R.layout.fs_floating_view);
        View floatImage = findViewById(android.R.id.icon);
        if (floatImage instanceof ImageView) {
            FSFloatImageManager.loadInto((ImageView) floatImage);
        }

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

    /** 首次展示后延迟校准横屏初始位置。 */
    private final Runnable mSettleInitialPositionRunnable = () -> {
        if (!mInitialPositionPending || !isShowing() || mAnimatingFlag || mDraggingFlag) {
            return;
        }
        settleInitialPosition();
    };

    /** 悬浮球空闲一段时间后自动半隐藏贴边。 */
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

    /**
     * 将半隐藏状态恢复为完整显示。
     */
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

    /**
     * 拖拽开始时取消半隐藏任务，并把半隐藏状态恢复成完整显示。
     */
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

    /**
     * 拖拽结束时保存当前位置。
     */
    @Override
    public void onWindowDraggingStop(@NonNull EasyWindow<?> easyWindow) {
        mDraggingFlag = false;
        saveCurrentPosition();
    }

    /**
     * 回弹动画开始时刷新拖拽边界。
     */
    @Override
    public void onSpringBackAnimationStart(@NonNull EasyWindow<?> easyWindow, @NonNull Animator animator) {
        syncDragSafeAreaPolicy();
        refreshDragMetrics();
        mAnimatingFlag = true;
    }

    /**
     * 回弹动画结束后保存最终位置并重新启动半隐藏任务。
     */
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

    /**
     * 点击悬浮球时，先从半隐藏恢复为完整显示，再打开 SDK 首页。
     */
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
     * 统一获取悬浮球根布局，避免 EasyWindow 销毁阶段返回空 View 时引发异常。
     */
    private View getWindowRootLayout() {
        View rootLayout = getRootLayout();
        if (rootLayout != null) {
            return rootLayout;
        }
        return getContentView();
    }

    /**
     * 获取可用的悬浮球宽度，布局未完成时回退到 EasyWindow 记录的宽度。
     */
    private int getAvailableViewWidth(View view) {
        int viewWidth = view == null ? 0 : view.getWidth();
        return viewWidth > 0 ? viewWidth : getWindowViewWidth();
    }

    /**
     * 获取可用的悬浮球高度，布局未完成时回退到 EasyWindow 记录的高度。
     */
    private int getAvailableViewHeight(View view) {
        int viewHeight = view == null ? 0 : view.getHeight();
        return viewHeight > 0 ? viewHeight : getWindowViewHeight();
    }

    /**
     * 根据当前横竖屏和沉浸状态同步拖拽安全区策略。
     */
    private void syncDragSafeAreaPolicy() {
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule != null) {
            /*
             * 横屏游戏通常处于沉浸模式。部分设备在宿主已经隐藏状态栏和导航栏后，
             * EasyWindow 仍会读到非零左侧安全区；如果悬浮球被限制在这个安全区内，
             * 就无法贴到物理屏幕边缘，点击时也容易被误判为只需要恢复完整显示。
             * 竖屏保持相对保守的安全区策略，横屏拖拽和回弹则允许使用物理边缘。
            */
            windowDraggableRule.setAllowMoveToScreenSafeArea(isLandscape());
        }
        if (isLandscape()) {
            addWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            removeWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    /**
     * 配置悬浮球窗口的全屏、沉浸和挖孔屏参数。
     */
    private void configureFloatingWindowLayout() {
        /*
         * EasyWindow 在 Activity 构造器中复制宿主当时的系统栏标记。
         * 但宿主可能稍后才进入沉浸模式，例如在 onWindowFocusChanged 中设置；
         * 悬浮球又是在 onActivityCreated 阶段创建的，所以这里主动把悬浮球窗口
         * 设置为边到边布局，保证首次显示时可见区域也从物理屏幕坐标 0 开始。
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

    /**
     * 刷新 EasyWindow 拖拽规则缓存的窗口和屏幕尺寸。
     */
    private void refreshDragMetrics() {
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule instanceof BaseWindowDraggableRule) {
            /*
             * BaseWindowDraggableRule 会在 start() 时缓存可见窗口区域。
             * WindowManager 挂载窗口后需要再次刷新；当宿主在 Activity 创建后
             * 才设置沉浸标记时，这一步可以避免使用过期边界。
             */
            ((BaseWindowDraggableRule) windowDraggableRule).refreshWindowInfo();
            ((BaseWindowDraggableRule) windowDraggableRule).refreshScreenPhysicalSize();
        }
    }

    /**
     * 首次展示时修正横屏初始位置，避免被旧安全区推离屏幕边缘。
     */
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

    /**
     * 横屏半隐藏前把悬浮球吸附到物理屏幕边缘。
     */
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

    /**
     * 横屏首次展示时把悬浮球对齐到左侧物理边缘。
     */
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

    /**
     * 获取 EasyWindow 计算出的屏幕不可见宽度，用于补偿沉浸式横屏坐标。
     */
    private int getScreenInvisibleWidth() {
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule == null) {
            return 0;
        }
        return Math.max(0, windowDraggableRule.getScreenInvisibleWidth());
    }

    /**
     * 判断当前窗口是否处于横屏。
     */
    private boolean isLandscape() {
        return isLandscape(getContext());
    }

    /**
     * 判断指定 Context 是否处于横屏。
     */
    private boolean isLandscape(Context context) {
        return context != null &&
                context.getResources() != null &&
                context.getResources().getConfiguration() != null &&
                context.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 窗口展示后恢复历史位置或校准首次横屏位置。
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

    /**
     * 窗口回收前保存当前位置，覆盖打开首页和宿主销毁等场景。
     */
    @Override
    public void onWindowRecycle(@NonNull EasyWindow<?> easyWindow) {
        saveCurrentPosition();
    }

    /**
     * 恢复历史位置，并按当前屏幕边界裁剪，防止尺寸变化后越界。
     */
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

    /**
     * 将数值限制在指定范围内。
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * 保存当前悬浮球坐标。保存失败时静默忽略，避免影响悬浮球生命周期。
     */
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
            // 位置持久化不能影响悬浮球生命周期。
        }
    }

    /**
     * 读取当前宿主页面和屏幕方向对应的历史悬浮球坐标。
     */
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

    /**
     * 生成悬浮球位置缓存键名前缀，按宿主 Activity 类名和横竖屏隔离。
     */
    private String getPositionKeyPrefix(Activity activity) {
        return POSITION_KEY_PREFIX
                + activity.getClass().getName()
                + "."
                + (isLandscape(activity) ? "landscape" : "portrait");
    }

    /**
     * 悬浮球历史坐标数据。
     */
    private static final class SavedPosition {
        private final int x;
        private final int y;

        private SavedPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
