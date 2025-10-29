package com.wishfox.foxsdk.ui.viewmodel;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.repository.FSMessageRepository;
import com.wishfox.foxsdk.domain.intent.FSMessageIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSMessageViewState;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;

import java.util.ArrayList;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 19:03
 */
public class FSMessageViewModel extends FoxSdkBaseMviViewModel<FSMessageViewState, FSMessageIntent, FoxSdkUiEffect> {

    private FSMessageRepository repository;
    private CompositeDisposable disposables = new CompositeDisposable();

    public FSMessageViewModel(FSMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    protected FSMessageViewState initialState() {
        return FSMessageViewState.Init();
    }

    @Override
    protected void handleIntent(FSMessageIntent intent) {
        if (intent instanceof FSMessageIntent.Refresh) {
            refreshMessages();
        } else if (intent instanceof FSMessageIntent.Read) {
            readMessage(((FSMessageIntent.Read) intent).getId());
        }
    }

    private void refreshMessages() {
        disposables.add(repository.getMessageList(1, 100,
                WishFoxSdk.getConfig().getChannelId(),
                WishFoxSdk.getConfig().getAppId())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        setState(FSMessageViewState.LoadList(
                                result.getData().getData() != null ? result.getData().getData() : new ArrayList<>(),
                                true,
                                false
                        ));
                    } else {
                        setState(FSMessageViewState.LoadList(new ArrayList<>(), true, false));
                        Toaster.show(result.getError());
                    }
                }, throwable -> {
                    setState(FSMessageViewState.LoadList(new ArrayList<>(), true, false));
                    Toaster.show(throwable.getMessage());
                }));
    }

    private void readMessage(Long id) {
        disposables.add(repository.read(id)
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        setState(FSMessageViewState.Read(id));
                    } else {
                        Toaster.show(result.getError());
                    }
                }, throwable -> {
                    Toaster.show(throwable.getMessage());
                }));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
    }
}
