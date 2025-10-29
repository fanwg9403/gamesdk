package com.wishfox.foxsdk.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.domain.intent.FoxSdkViewIntent;
import com.wishfox.foxsdk.ui.view.widgets.FSLoadingDialog;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkViewState;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:54
 */
public abstract class FoxSdkBaseMviFragment<VS extends FoxSdkViewState, VI extends FoxSdkViewIntent, VB extends ViewBinding>
        extends Fragment {

    protected abstract FoxSdkBaseMviViewModel<VS, VI, FoxSdkUiEffect> getViewModel();
    protected VB binding;

    private CompositeDisposable disposables = new CompositeDisposable();
    private FSLoadingDialog loadingDialog;
    private boolean isLoadingShowing = false;

    protected abstract VB createBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = createBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView();
        observeState();
        observeEffects();
        observeLoading();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.dispose();
        dismissLoading();
    }

    protected abstract void initView();

    private void observeState() {
        Disposable stateDisposable = getViewModel().getViewState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::renderState);
        disposables.add(stateDisposable);
    }

    private void observeEffects() {
        Disposable effectDisposable = getViewModel().getUiEffect()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleEffect);
        disposables.add(effectDisposable);
    }

    private void observeLoading() {
        Disposable loadingDisposable = getViewModel().getLoadingState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loadingState -> {
                    if (loadingState instanceof LoadingState.Show) {
                        showLoading(((LoadingState.Show) loadingState).getMessage());
                    } else if (loadingState == LoadingState.DISMISS) {
                        dismissLoading();
                    }
                    // LoadingState.Idle 什么都不做
                });
        disposables.add(loadingDisposable);
    }

    protected abstract void renderState(VS state);

    protected void handleEffect(FoxSdkUiEffect effect) {
        if (effect instanceof FoxSdkUiEffect.ShowToast) {
            Toaster.show(((FoxSdkUiEffect.ShowToast) effect).getMessage());
        } else if (effect instanceof FoxSdkUiEffect.NavigateTo) {
            FoxSdkUiEffect.NavigateTo navigateTo = (FoxSdkUiEffect.NavigateTo) effect;
            navigateTo(navigateTo.getDestination(), navigateTo.getArgs());
        } else if (effect instanceof FoxSdkUiEffect.NavigateBack) {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    protected void showLoading(String message) {
        if (isAdded() && !isRemoving() && !isLoadingShowing) {
            loadingDialog = new FSLoadingDialog(requireContext());
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
}
