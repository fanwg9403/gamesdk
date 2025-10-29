package com.wishfox.foxsdk.ui.base;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;
import com.wishfox.foxsdk.domain.intent.FoxSdkViewIntent;
import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.ui.view.widgets.FSLoadingDialog;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkViewState;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 14:51
 */
public abstract class FoxSdkBaseMviActivity<VS extends FoxSdkViewState, VI extends FoxSdkViewIntent, VB extends ViewBinding> extends AppCompatActivity {

    protected abstract FoxSdkBaseMviViewModel<VS, VI, FoxSdkUiEffect> getViewModel();
    protected VB binding;

    private CompositeDisposable disposables = new CompositeDisposable();
    private FSLoadingDialog loadingDialog;
    private boolean isLoadingShowing = false;

    protected abstract VB createBinding();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyScreenOrientation();
        super.onCreate(savedInstanceState);
        binding = createBinding();
        setContentView(binding.getRoot());

        initView();
        observeState();
        observeEffects();
        observeLoading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
        dismissLoading();
    }

    protected abstract void initView();

    private void observeState() {
        Disposable disposable = getViewModel().getViewState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::renderState);
        disposables.add(disposable);
    }

    private void observeEffects() {
        Disposable disposable = getViewModel().getUiEffect()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleEffect);
        disposables.add(disposable);
    }

    private void observeLoading() {
        Disposable disposable = getViewModel().getLoadingState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleLoadingState);
        disposables.add(disposable);
    }

    private void handleLoadingState(LoadingState loadingState) {
        if (loadingState instanceof LoadingState.Show) {
            showLoading(((LoadingState.Show) loadingState).getMessage());
        } else if (loadingState == LoadingState.DISMISS) {
            dismissLoading();
        }
        // Idle状态不做任何处理
    }

    protected abstract void renderState(VS state);

    protected void handleEffect(FoxSdkUiEffect effect) {
        if (effect instanceof FoxSdkUiEffect.ShowToast) {
            Toaster.show(((FoxSdkUiEffect.ShowToast) effect).getMessage());
        } else if (effect instanceof FoxSdkUiEffect.NavigateTo) {
            FoxSdkUiEffect.NavigateTo navigateTo = (FoxSdkUiEffect.NavigateTo) effect;
            navigateTo(navigateTo.getDestination(), navigateTo.getArgs());
        } else if (effect instanceof FoxSdkUiEffect.NavigateBack) {
            onBackPressed();
        }
    }

    protected void showLoading(String message) {
        if (!isFinishing() && !isDestroyed() && !isLoadingShowing) {
            loadingDialog = new FSLoadingDialog(this);
            loadingDialog.setMessage(message);
            loadingDialog.setCancelable(false);
            loadingDialog.setOnDismissListener(dialog -> isLoadingShowing = false);
            loadingDialog.show();
            isLoadingShowing = true;
        }
    }

    protected void dismissLoading() {
        if (isLoadingShowing && loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
            isLoadingShowing = false;
        }
    }

    protected void dispatch(VI intent) {
        getViewModel().dispatch(intent);
    }

    protected void navigateTo(String destination, Bundle args) {
        // 子类实现具体导航逻辑
    }

    private void applyScreenOrientation() {
        int orientation = getScreenOrientation();
        if (orientation == FoxSdkConfig.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (orientation == FoxSdkConfig.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    protected int getScreenOrientation() {
        return WishFoxSdk.getConfig().getScreenOrientation();
    }
}
