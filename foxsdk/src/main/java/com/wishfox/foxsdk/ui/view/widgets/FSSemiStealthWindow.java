package com.wishfox.foxsdk.ui.view.widgets;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.hjq.window.EasyWindow;
import com.hjq.window.OnWindowLifecycleCallback;
import com.hjq.window.OnWindowViewClickListener;
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

    private boolean mAnimatingFlag;
    private boolean mDraggingFlag;
    private final WeakReference<Activity> hostActivity;

    public FSSemiStealthWindow(@NonNull Activity activity) {
        super(activity);
        hostActivity = new WeakReference<>(activity);
    }

    @Override
    protected void initWindow(@NonNull Context context) {
        super.initWindow(context);

        setContentView(R.layout.fs_floating_view);

        setWindowLocation(0, 200);

        SpringBackWindowDraggableRule springBackWindowDraggableRule = new SpringBackWindowDraggableRule(
                SpringBackWindowDraggableRule.ORIENTATION_HORIZONTAL);
        springBackWindowDraggableRule.setAllowMoveToScreenSafeArea(false);
        springBackWindowDraggableRule.setOnWindowDraggingCallback(this);
        springBackWindowDraggableRule.setOnSpringBackAnimCallback(this);
        setWindowDraggableRule(springBackWindowDraggableRule);

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

    private final Runnable mStayEdgeRunnable = () -> {
        if (mAnimatingFlag || mDraggingFlag) {
            return;
        }

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
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule == null) {
            return;
        }
        View windowRootLayout = getWindowRootLayout();
        if (windowRootLayout == null) {
            return;
        }

        windowRootLayout.setAlpha(0.5f);
        int viewWidth = getWindowViewWidth();
        int viewHeight = getWindowViewHeight();

        // 创建一个矩形来定义裁剪区域
        Rect clipBounds = new Rect();
        switch (gravity) {
            case Gravity.LEFT:
                Rect safeInsetRect = windowDraggableRule.getSafeInsetRect(this);
                if (safeInsetRect != null && safeInsetRect.left > 0) {
                    WindowManager.LayoutParams windowParams = getWindowParams();
                    windowDraggableRule.updateLocation(windowParams.x - viewWidth / 2f, windowParams.y, true);
                } else {
                    int offSet = getWindowViewWidth() / 2;
                    clipBounds.set(offSet, 0, viewWidth, viewHeight);
                    // 设置画板偏移
                    windowRootLayout.setTranslationX(-offSet);
                    windowRootLayout.setTranslationY(0);
                    // 设置裁剪区域
                    windowRootLayout.setClipBounds(clipBounds);
                }
                break;
            case Gravity.RIGHT:
                int offSet = viewWidth / 2;
                clipBounds.set(0, 0, viewWidth - offSet, viewHeight);
                // 设置画板偏移
                windowRootLayout.setTranslationX(offSet);
                windowRootLayout.setTranslationY(0);
                // 设置裁剪区域
                windowRootLayout.setClipBounds(clipBounds);
                break;
            default:
                break;
        }
    }

    private void showFullView() {
        View rootLayout = getWindowRootLayout();
        if (rootLayout == null) {
            return;
        }
        rootLayout.setAlpha(1f);
        int viewWidth = rootLayout.getWidth();
        int viewHeight = rootLayout.getHeight();
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule == null) {
            return;
        }
        Rect safeInsetRect = windowDraggableRule.getSafeInsetRect(this);
        if (safeInsetRect != null && safeInsetRect.left > 0) {
            WindowManager.LayoutParams windowParams = getWindowParams();
            windowDraggableRule.updateLocation(windowParams.x + viewWidth / 2f, windowParams.y, false);
        }
        // 设置画板偏移
        rootLayout.setTranslationX(0);
        rootLayout.setTranslationY(0);
        // 设置裁剪区域
        rootLayout.setClipBounds(new Rect(0, 0, viewWidth, viewHeight));
    }

    /**
     * View 是否全屏显示
     */
    private boolean isFullShowView() {
        View view = getWindowRootLayout();
        IWindowDraggableRule windowDraggableRule = getWindowDraggableRule();
        if (windowDraggableRule == null) {
            return true;
        }
        Rect safeInsetRect = windowDraggableRule.getSafeInsetRect(this);
        if (safeInsetRect != null && safeInsetRect.left > 0) {
            if (getWindowParams().x < safeInsetRect.left) {
                return false;
            }
        }
        if (view == null) {
            return true;
        }
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        Rect clipBounds = view.getClipBounds();
        if (view.getTranslationX() != 0 || view.getTranslationY() != 0) {
            return false;
        }
        if (clipBounds == null) {
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
        return (getWindowParams().x + getWindowViewWidth() / 2f) < getScreenWidth() / 2f;
    }

    @Override
    public void onWindowDraggingStart(@NonNull EasyWindow<?> easyWindow) {
        mDraggingFlag = true;
        if (!isFullShowView()) {
            showFullView();
        }
    }

    @Override
    public void onWindowDraggingStop(@NonNull EasyWindow<?> easyWindow) {
        mDraggingFlag = false;
    }

    @Override
    public void onSpringBackAnimationStart(@NonNull EasyWindow<?> easyWindow, @NonNull Animator animator) {
        mAnimatingFlag = true;
    }

    @Override
    public void onSpringBackAnimationEnd(@NonNull EasyWindow<?> easyWindow, @NonNull Animator animator) {
        mAnimatingFlag = false;
        postStayEdgeRunnable();
    }

    @Override
    public void onClick(@NonNull EasyWindow<?> easyWindow, @NonNull View view) {
        if (!isFullShowView()) {
            showFullView();
            postStayEdgeRunnable();
            return;
        } else {
            Activity activity = hostActivity.get();
            FoxSdkOverlayManager.show(activity);
        }
    }

    /**
     * EasyWindow 15.8 exposes the content view as the stable root accessor.
     * Keeping this small adapter also makes the floating ball resilient when
     * the library returns a null content view during teardown.
     */
    private View getWindowRootLayout() {
        View contentView = getContentView();
        return contentView;
    }

    /**
     * {@link OnWindowLifecycleCallback}
     */

    @Override
    public void onWindowShow(@NonNull EasyWindow<?> easyWindow) {
        postStayEdgeRunnable();
    }
}
