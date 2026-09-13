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
 * Common lifecycle boundary for pages rendered inside the host Activity.
 *
 * <p>These pages are views instead of Activities. They therefore keep the
 * Unity/Cocos Activity resumed while still reusing the SDK's existing XML
 * layouts, adapters and ViewModels.</p>
 */
public abstract class FSOverlayPageView extends FrameLayout {

    public interface Callback {
        void onCloseRequested();

        void onOpenPage(FoxSdkOverlayManager.Page page);

        void onOpenWeb(String url, boolean showTitle);
    }

    protected final Activity activity;
    protected final Callback callback;
    protected final CompositeDisposable disposables = new CompositeDisposable();

    private FSLoadingDialog loadingDialog;
    private boolean destroyed;

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

    protected void handleBackPressed() {
        requestClose();
    }

    protected final void requestClose() {
        if (!destroyed && callback != null) {
            callback.onCloseRequested();
        }
    }

    /**
     * The old Activity implementation used ImmersionBar to size this view.
     * Overlay pages must apply the host window inset themselves without
     * changing the host window flags.
     */
    protected final void applyTopInset(View topSafeView) {
        applyWindowInsets(topSafeView, null, null);
    }

    /**
     * Applies the host Activity's actual system-bar and display-cutout insets.
     * Landscape layouts must not reserve a fixed left margin: most game
     * devices have no left cutout at all, while a notched device may need one.
     */
    protected final void applyWindowInsets(View topSafeView, View startSafeView) {
        applyWindowInsets(topSafeView, startSafeView, null);
    }

    /**
     * Applies insets and shifts the page content root without making the
     * start-safe-area view a member of the horizontal weight chain.
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
        // The overlay is attached after the host Activity has already
        // dispatched its first insets pass. Request one more pass so pages
        // added at runtime receive the actual immersive/cutout values.
        androidx.core.view.ViewCompat.requestApplyInsets(this);
    }

    protected final void bindViewModel(FoxSdkBaseMviViewModel<?, ?, ?> viewModel) {
        if (viewModel == null) {
            return;
        }
        disposables.add(viewModel.getLoadingState().subscribe(
                this::renderLoadingState,
                throwable -> {
                    // Loading UI must never break the page lifecycle.
                }
        ));
        disposables.add(viewModel.getUiEffect().subscribe(
                effect -> handleEffect((FoxSdkUiEffect) effect),
                throwable -> {
                    // Effects are best-effort diagnostics/UI notifications.
                }
        ));
    }

    private void handleEffect(FoxSdkUiEffect effect) {
        if (effect instanceof FoxSdkUiEffect.ShowToast) {
            Toaster.show(((FoxSdkUiEffect.ShowToast) effect).getMessage());
        } else if (effect instanceof FoxSdkUiEffect.NavigateBack) {
            requestClose();
        }
    }

    private void renderLoadingState(LoadingState loadingState) {
        if (loadingState instanceof LoadingState.Show) {
            showLoading(((LoadingState.Show) loadingState).getMessage());
        } else if (loadingState == LoadingState.DISMISS) {
            dismissLoading();
        }
    }

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

    protected final void dismissLoading() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    public final boolean isDestroyedForOverlay() {
        return destroyed;
    }

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
        destroy();
        super.onDetachedFromWindow();
    }
}
