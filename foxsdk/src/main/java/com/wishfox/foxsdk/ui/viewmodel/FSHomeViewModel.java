package com.wishfox.foxsdk.ui.viewmodel;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.data.model.entity.FSHomeBanner;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;
import com.wishfox.foxsdk.data.model.entity.FSUserProfile;
import com.wishfox.foxsdk.data.repository.FSHomeRepository;
import com.wishfox.foxsdk.domain.intent.FSHomeIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel;
import com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog;
import com.wishfox.foxsdk.ui.viewstate.FSHomeViewState;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;
import com.wishfox.foxsdk.utils.FoxSdkConstant;
import com.wishfox.foxsdk.utils.FoxSdkSPUtils;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:58
 */
public class FSHomeViewModel extends FoxSdkBaseMviViewModel<FSHomeViewState, FSHomeIntent, FoxSdkUiEffect> {

    private FSHomeRepository repository;
    private CompositeDisposable disposables = new CompositeDisposable();

    public FSHomeViewModel(FSHomeRepository repository) {
        this.repository = repository;
    }

    @Override
    protected FSHomeViewState initialState() {
        return new FSHomeViewState();
    }

    @Override
    protected void handleIntent(FSHomeIntent intent) {
        if (intent instanceof FSHomeIntent.Login) {
            handleLogin((FSHomeIntent.Login) intent);
        } else if (intent instanceof FSHomeIntent.Init) {
            handleInit();
        } else if (intent instanceof FSHomeIntent.Logout) {
            handleLogout();
        }
    }

    private void handleLogin(FSHomeIntent.Login loginIntent) {
        Disposable loginDisposable = repository.login(
                loginIntent.getPhone(),
                loginIntent.getCode(),
                loginIntent.getType()
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        FSLoginResult.save(result.getData());
                        getUserInfo();
                    } else {
                        FSLoginDialog.dismissInstance();
                        Toaster.show(result.getError());
                        setState(new FSHomeViewState(null, Collections.emptyList(), null, false));
                    }
                }, throwable -> {
                    FSLoginDialog.dismissInstance();
                    Toaster.show(throwable.getMessage());
                    setState(new FSHomeViewState(null, Collections.emptyList(), null, false));
                });

        disposables.add(loginDisposable);
    }

    private void handleInit() {
        // 获取广告列表
        Disposable initDisposable = repository.getAdvertiseList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adResult -> {
                    if (adResult.isSuccess()) {
                        List<FSHomeBanner> banners = adResult.getData();

                        // 如果用户已登录，获取用户虚拟信息
                        if (FSUserInfo.getInstance() != null) {
                            getUserVirtualInfoAndUpdateState(banners);
                        } else {
                            // 用户未登录，只更新广告数据
                            setState(new FSHomeViewState(
                                    FSUserInfo.getInstance(),
                                    banners,
                                    null,
                                    true
                            ));
                        }
                    } else {
                        // 广告获取失败，只更新用户信息
                        updateUserStateOnly();
                    }
                }, throwable -> {
                    // 初始化失败，只更新用户信息
                    updateUserStateOnly();
                });

        disposables.add(initDisposable);
    }

    private void getUserVirtualInfoAndUpdateState(List<FSHomeBanner> banners) {
        Disposable coinDisposable = repository.getUserVirtualInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(coinResult -> {
                    FSCoinInfo coinInfo = null;
                    if (coinResult.isSuccess()) {
                        coinInfo = coinResult.getData();
                        FSCoinInfo.save(coinInfo);
                    }

                    setState(new FSHomeViewState(
                            FSUserInfo.getInstance(),
                            banners,
                            coinInfo,
                            true
                    ));
                }, throwable -> {
                    // 获取虚拟信息失败，仍然更新状态
                    setState(new FSHomeViewState(
                            FSUserInfo.getInstance(),
                            banners,
                            null,
                            true
                    ));
                });

        disposables.add(coinDisposable);
    }

    private void updateUserStateOnly() {
        // 获取用户虚拟信息（如果已登录）
        if (FSUserInfo.getInstance() != null) {
            Disposable coinDisposable = repository.getUserVirtualInfo()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(coinResult -> {
                        FSCoinInfo coinInfo = null;
                        if (coinResult.isSuccess()) {
                            coinInfo = coinResult.getData();
                            FSCoinInfo.save(coinInfo);
                        }

                        setState(new FSHomeViewState(
                                FSUserInfo.getInstance(),
                                Collections.emptyList(),
                                coinInfo,
                                true
                        ));
                    }, throwable -> {
                        setState(new FSHomeViewState(
                                FSUserInfo.getInstance(),
                                Collections.emptyList(),
                                null,
                                true
                        ));
                    });

            disposables.add(coinDisposable);
        } else {
            setState(new FSHomeViewState(
                    null,
                    Collections.emptyList(),
                    null,
                    true
            ));
        }
    }

    private void handleLogout() {
        // 清除本地数据
        FSUserProfile.clear();
        FSLoginResult.clear();
        FoxSdkSPUtils.getInstance().remove(FoxSdkConstant.AUTHORIZATION);
        FSCoinInfo.clear();

        // 调用退出登录API
        Disposable logoutDisposable = repository.logout()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    // 无论API调用成功与否，都更新本地状态
                    updateStateAfterLogout();
                }, throwable -> {
                    // 即使API调用失败，也更新本地状态
                    updateStateAfterLogout();
                });

        disposables.add(logoutDisposable);
    }

    private void updateStateAfterLogout() {
        // 获取广告列表来更新状态
        Disposable adDisposable = repository.getAdvertiseList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adResult -> {
                    List<FSHomeBanner> banners = Collections.emptyList();
                    if (adResult.isSuccess()) {
                        banners = adResult.getData();
                    }

                    setState(new FSHomeViewState(
                            null,
                            banners,
                            null,
                            true
                    ));
                }, throwable -> {
                    setState(new FSHomeViewState(
                            null,
                            Collections.emptyList(),
                            null,
                            true
                    ));
                });

        disposables.add(adDisposable);
    }

    public void getUserInfo() {
        Disposable userInfoDisposable = repository.getUserInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        FSUserProfile.save(result.getData());

                        // 获取用户虚拟信息和广告列表
                        Single.zip(
                                repository.getUserVirtualInfo().subscribeOn(Schedulers.io()),
                                repository.getAdvertiseList().subscribeOn(Schedulers.io()),
                                (coinRes, adRes) -> {
                                    FSCoinInfo coinInfo = null;
                                    List<FSHomeBanner> banners = Collections.emptyList();

                                    if (coinRes.isSuccess()) {
                                        coinInfo = coinRes.getData();
                                        FSCoinInfo.save(coinInfo);
                                    }

                                    if (adRes.isSuccess()) {
                                        banners = adRes.getData();
                                    }

                                    return new FSHomeViewState(
                                            result.getData().getUserInfo(),
                                            banners,
                                            coinInfo,
                                            true
                                    );
                                }
                        )
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(newState -> {
                                    setState(newState);
                                    FSLoginDialog.dismissInstance();
                                    Toaster.show("登录成功");
                                }, throwable -> {
                                    // 部分失败，仍然更新状态
                                    setState(new FSHomeViewState(
                                            result.getData().getUserInfo(),
                                            Collections.emptyList(),
                                            null,
                                            true
                                    ));
                                    FSLoginDialog.dismissInstance();
                                    Toaster.show("登录成功");
                                });

                    } else {
                        FSLoginDialog.dismissInstance();
                        Toaster.show(result.getError());
                        setState(new FSHomeViewState(null, Collections.emptyList(), null, false));
                    }
                }, throwable -> {
                    FSLoginDialog.dismissInstance();
                    Toaster.show(throwable.getMessage());
                    setState(new FSHomeViewState(null, Collections.emptyList(), null, false));
                });

        disposables.add(userInfoDisposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
    }
}
