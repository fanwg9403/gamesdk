package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.core.FoxSdkOverlayManager;
import com.wishfox.foxsdk.ui.base.LoadingState;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * 宿主 Activity 内渲染的 SDK 二级页面基类。
 *
 * <p>这些页面以 View 形式挂载，不再启动新的 Activity，因此可以让 Unity/Cocos
 * 宿主 Activity 保持运行状态，同时继续复用 SDK 现有的 XML、Adapter 和 ViewModel。</p>
 */
public abstract class FSOverlayPageView extends FrameLayout {

    /**
     * 二级页面与 Overlay 管理器之间的交互回调。
     */
    public interface Callback {
        /** 请求关闭当前页面并返回上一层。 */
        void onCloseRequested();

        /** 请求打开指定 SDK 页面。 */
        void onOpenPage(FoxSdkOverlayManager.Page page);

        /** 请求打开 SDK 内 WebView 页面。 */
        void onOpenWeb(String url, boolean showTitle);
    }

    protected final Activity activity;
    protected final Callback callback;
    protected final CompositeDisposable disposables = new CompositeDisposable();

    private FSLoadingDialog loadingDialog;
    private boolean destroyed;

    /**
     * 初始化通用的页面属性和返回键拦截。
     */
    protected FSOverlayPageView(Activity activity, Callback callback) {
        super(activity);
        this.activity = activity;
        this.callback = callback;
        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        setFocusableInTouchMode(true);
        setClickable(true);
        setOnKeyListener((view, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                    event != null &&
                    event.getAction() == KeyEvent.ACTION_UP) {
                handleBackPressed();
                return true;
            }
            return false;
        });
    }

    /**
     * 处理实体返回键或系统返回手势。
     */
    protected void handleBackPressed() {
        requestClose();
    }

    /**
     * 安全触发关闭回调，页面销毁后不再重复派发。
     */
    protected final void requestClose() {
        if (!destroyed && callback != null) {
            callback.onCloseRequested();
        }
    }

    /**
     * 旧 Activity 实现依赖 ImmersionBar 处理安全区。
     * Overlay 页面需要自行读取宿主窗口安全区，不能修改宿主窗口标记。
     */
    protected final void applyTopInset(View topSafeView) {
        applyWindowInsets(topSafeView, null, null);
    }

    /**
     * 应用宿主 Activity 当前真实的系统栏和挖孔屏安全区。
     * 横屏布局不能写死左侧间距：大部分游戏设备左侧没有挖孔，少数挖孔设备才需要动态留白。
     */
    protected final void applyWindowInsets(View topSafeView, View startSafeView) {
        applyWindowInsets(topSafeView, startSafeView, null);
    }

    /**
     * 应用安全区并移动页面内容根节点，避免把左侧安全区占位 View 加入横向权重链。
     */
    protected final void applyWindowInsets(
            View topSafeView,
            View startSafeView,
            View contentView
    ) {
        FSOverlayInsets.apply(
                activity,
                this,
                topSafeView,
                startSafeView,
                contentView
        );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Overlay 通常在宿主 Activity 首次分发安全区之后才挂载，
        // 这里主动再请求一次，保证运行时添加的页面能拿到真实的沉浸和挖孔参数。
        androidx.core.view.ViewCompat.requestApplyInsets(this);
    }

    /**
     * 绑定 ViewModel 的加载状态和一次性 UI 事件。
     */
    protected final void bindViewModel(FoxSdkBaseMviViewModel<?, ?, ?> viewModel) {
        if (viewModel == null) {
            return;
        }
        disposables.add(viewModel.getLoadingState().subscribe(
                this::renderLoadingState,
                throwable -> {
                    // 加载弹窗异常不能影响页面生命周期。
                }
        ));
        disposables.add(viewModel.getUiEffect().subscribe(
                effect -> handleEffect((FoxSdkUiEffect) effect),
                throwable -> {
                    // UI 事件属于辅助提示，异常时忽略即可。
                }
        ));
    }

    /**
     * 处理 ViewModel 发出的 UI 事件。
     */
    private void handleEffect(FoxSdkUiEffect effect) {
        if (effect instanceof FoxSdkUiEffect.ShowToast) {
            Toaster.show(((FoxSdkUiEffect.ShowToast) effect).getMessage());
        } else if (effect instanceof FoxSdkUiEffect.NavigateBack) {
            requestClose();
        }
    }

    /**
     * 根据加载状态展示或关闭加载弹窗。
     */
    private void renderLoadingState(LoadingState loadingState) {
        if (loadingState instanceof LoadingState.Show) {
            showLoading(((LoadingState.Show) loadingState).getMessage());
        } else if (loadingState == LoadingState.DISMISS) {
            dismissLoading();
        }
    }

    /**
     * 展示通用加载弹窗。
     */
    protected final void showLoading(String message) {
        if (destroyed || activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (loadingDialog == null) {
            loadingDialog = new FSLoadingDialog(activity);
            loadingDialog.setCancelable(false);
        }
        loadingDialog.setMessage(message);
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    /**
     * 关闭并释放通用加载弹窗。
     */
    protected final void dismissLoading() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    /**
     * 判断当前 Overlay 页面是否已经销毁。
     */
    public final boolean isDestroyedForOverlay() {
        return destroyed;
    }

    /**
     * 释放页面持有的订阅和弹窗资源。
     */
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        disposables.dispose();
        dismissLoading();
    }

    @Override
    protected void onDetachedFromWindow() {
        // 被宿主移除时兜底释放资源，避免页面订阅或弹窗泄漏。
        destroy();
        super.onDetachedFromWindow();
    }
}
