package com.wishfox.foxsdk.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.data.model.entity.FSPayResult;
import com.wishfox.foxsdk.data.model.entity.FSSdkConfig;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;
import com.wishfox.foxsdk.data.model.entity.FSUserProfile;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.network.FoxSdkNetworkExecutor;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;
import com.wishfox.foxsdk.data.repository.FSHomeRepository;
import com.wishfox.foxsdk.ui.view.dialog.FSAlertDialog;
import com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog;
import com.wishfox.foxsdk.ui.view.dialog.FSPayDialog;
import com.wishfox.foxsdk.ui.view.widgets.FSLoadingDialog;
import com.wishfox.foxsdk.utils.pay.FoxSdkPayResult;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.adapter.rxjava3.HttpException;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月29日 17:45
 */
public class FoxSdkLongingPayUtils {

    private FSLoadingDialog loading;
    private FSLoginDialog loginDialog;
    private CompositeDisposable disposables = new CompositeDisposable();

    private String mallIdFS = "";
    private String mallNameFS = "";
    private String priceFS = "";
    private String priceContentFS = "";
    private long orderTimeFS = 0;
    private String cpOrderIdFS = "";

    /**
     * 登录弹窗
     *
     * @param mActivity
     * @param onLoginListener
     */
    public void loginWishFox(Activity mActivity, OnLoginListener onLoginListener) {
        loginDialog = new FSLoginDialog(mActivity);
        loginDialog.setOnLoginClickListener((arg1, arg2, type) -> {
            loading = new FSLoadingDialog(mActivity);
            loading.show();

            Disposable loginDisposable = login(arg1, arg2, type)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(success -> {
                        if (success) {
                            onLoginListener.onLogin(FSUserInfo.getInstance().getUserId(), FSLoginResult.getTokenEd());
                            loginDialog.dismiss();
                            loading.dismiss();
                        } else {
                            loading.dismiss();
                        }
                    }, throwable -> {
                        loading.dismiss();
                        Toaster.show(throwable.getMessage());
                    });

            disposables.add(loginDisposable);
        });
        loginDialog.show();
    }

    /**
     * 登录支付
     */
    public void loginPay(
            Activity mActivity,
            String mallId,
            String mallName,
            String price,
            String priceContent,
            long orderTime,
            String cpOrderId,
            OnLoginListener onLoginListener
    ) {
        mallIdFS = mallId;
        mallNameFS = mallName;
        priceFS = price;
        priceContentFS = priceContent;
        orderTimeFS = orderTime;
        cpOrderIdFS = cpOrderId;

        if (FSUserInfo.getInstance() == null) { // 未登录
            loginDialog = new FSLoginDialog(mActivity);
            loginDialog.setOnLoginClickListener((arg1, arg2, type) -> {
                loading = new FSLoadingDialog(mActivity);
                loading.show();

                Disposable loginDisposable = login(arg1, arg2, type)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(success -> {
                            if (success) {
                                onLoginListener.onLogin(FSUserInfo.getInstance().getUserId(), FSLoginResult.getTokenEd());
                                loginDialog.dismiss();
                                loading.dismiss();
                                getSdkConfig(mActivity, mallId, mallName, price, priceContent,
                                        orderTime, cpOrderId);
                            } else {
                                loading.dismiss();
                            }
                        }, throwable -> {
                            loading.dismiss();
                            Toaster.show(throwable.getMessage());
                        });

                disposables.add(loginDisposable);
            });
            loginDialog.show();
        } else { // 已登录调用支付
            onLoginListener.onLogin(FSUserInfo.getInstance().getUserId(), FSLoginResult.getTokenEd());
            getSdkConfig(mActivity, mallId, mallName, price, priceContent,
                    orderTime, cpOrderId);
        }
    }


    @SuppressLint("CheckResult")
    private void getSdkConfig(Activity mActivity,
                              String mallId,
                              String mallName,
                              String price,
                              String priceContent,
                              long orderTime,
                              String cpOrderId) {
        loading = new FSLoadingDialog(mActivity);
        loading.show();
        FoxSdkNetworkExecutor.execute(() ->
                FoxSdkRetrofitManager.getApiService().getSdkConfig().blockingGet()
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    loading.dismiss();
                    if (result.isSuccess()) {
                        FSSdkConfig data = result.getData();
                        payDialog(mActivity, mallId, mallName, price, priceContent, orderTime, cpOrderId, data,
                                payResult -> {
                                });

                    } else if (result.isError()) {
                        loading.dismiss();
                        String errorMsg = result.getError() != null ? result.getError() : "支付失败";
                        Toaster.show(errorMsg);
                    } else if (result.isEmpty()) {
                        loading.dismiss();
                        Toaster.show("支付结果为空");
                    }
                }, throwable -> {
                    loading.dismiss();
                    String errorMsg = "网络请求失败";
                    if (throwable instanceof IOException) {
                        errorMsg = "网络连接失败，请检查网络";
                    } else if (throwable instanceof SocketTimeoutException) {
                        errorMsg = "网络连接超时，请重试";
                    } else if (throwable instanceof HttpException) {
                        errorMsg = "服务器错误，请稍后重试";
                    }
                    Toaster.show(errorMsg);
                });
    }

    /**
     * 登录
     */
    private Observable<Boolean> login(String phone, String code, int type) {
        FSHomeRepository repository = new FSHomeRepository();

        Single<FoxSdkNetworkResult<FSLoginResult>> loginSingle;
        if (type == 1) {
            loginSingle = repository.loginByPassword(phone, code);
        } else {
            loginSingle = repository.loginByVerifyCode(phone, code);
        }

        return loginSingle
                .flatMap(loginResult -> {
                    if (loginResult.isSuccess()) {
                        FSLoginResult data = loginResult.getData();
                        // 保存登录结果
                        FSLoginResult.save(data);
                        FoxSdkSPUtils.getInstance().put(FoxSdkConstant.AUTHORIZATION, data.getToken());

                        // 继续获取用户信息
                        return getUserInfoObservable();
                    } else if (loginResult.isError()) {
                        String error = loginResult.getError();
                        return Single.error(new RuntimeException(error != null ? error : "登录失败"));
                    } else if (loginResult.isEmpty()) {
                        return Single.error(new RuntimeException("登录结果为空"));
                    }
                    return Single.error(new RuntimeException("未知登录错误"));
                })
                .toObservable();
    }

    /**
     * 获取用户信息 Observable
     */
    private Single<Boolean> getUserInfoObservable() {
        FSHomeRepository repository = new FSHomeRepository();

        return repository.getUserInfo()
                .flatMap(userInfoResult -> {
                    if (userInfoResult.isSuccess()) {
                        FSUserProfile userProfile = userInfoResult.getData();
                        // 保存用户信息
                        FSUserProfile.save(userProfile);

                        // 继续获取虚拟货币信息
                        return getUserVirtualInfoObservable()
                                .onErrorReturnItem(true); // 虚拟信息获取失败不影响登录成功
                    } else if (userInfoResult.isError()) {
                        String error = userInfoResult.getError();
                        return Single.error(new RuntimeException(error != null ? error : "获取用户信息失败"));
                    } else if (userInfoResult.isEmpty()) {
                        return Single.error(new RuntimeException("用户信息为空"));
                    }
                    return Single.error(new RuntimeException("获取用户信息时发生未知错误"));
                });
    }

    /**
     * 获取用户虚拟信息 Observable
     */
    private Single<Boolean> getUserVirtualInfoObservable() {
        FSHomeRepository repository = new FSHomeRepository();

        return repository.getUserVirtualInfo()
                .map(virtualInfoResult -> {
                    if (virtualInfoResult.isSuccess()) {
                        FSCoinInfo coinInfo = virtualInfoResult.getData();
                        // 保存虚拟货币信息
                        FSCoinInfo.save(coinInfo);
                    } else {
                        // 虚拟信息获取失败，记录日志但不影响登录流程
                        FoxSdkLogger.w("FoxSdkLongingPayUtils", "获取用户虚拟信息失败: " +
                                (virtualInfoResult.getError() != null ? virtualInfoResult.getError() : "未知错误"));
                    }
                    // 虚拟信息获取成功或失败都不影响登录流程
                    Toaster.show("登录成功");
                    return true;
                })
                .onErrorReturn(throwable -> {
                    // 虚拟信息获取失败不影响登录成功
                    FoxSdkLogger.w("FoxSdkLongingPayUtils", "获取用户虚拟信息异常: " + throwable.getMessage());
                    Toaster.show("登录成功");
                    return true;
                });
    }

    // 支付回调参数
    private FSPayResult fsPayResult;

    // 支付弹框
    public void payDialog(
            Activity context,
            String mallId,
            String mallName,
            String price,
            String priceContent,
            long orderTime,
            String cpOrderId,
            FSSdkConfig sdkConfig,
            PayListener payListener
    ) {
        FSPayDialog payDialog = new FSPayDialog(context)
                .setPayName(mallName)
                .setPayInfo(priceContent)
                .setPayType(FoxSdkPayEnum.FOX_COIN)
                .setFSSdkConfig(sdkConfig)
                .setPayParams(mallId, mallName, price, orderTime, cpOrderId)
                .setOnPayCreateListener(new FSPayDialog.OnPayCreateListener() {
                    @Override
                    public void onPayCreate(FSPayResult payResult) {
                        // 调用查询支付结果，根据需求在适当位置进行调用
                        payListener.onPayResult(payResult);
                        fsPayResult = payResult;
                        if (fsPayResult != null && fsPayResult.getPayType() == FoxSdkPayEnum.FOX_COIN) {
                            if (fsPayResult.isCheckPay()) {
                                startPollingPaymentResult(context, fsPayResult);
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                context.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
                            } else {
                                context.getApplication().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
                            }
                        }
                    }
                });
        payDialog.show();
    }

    private Disposable pollingDisposable;

    private void startPollingPaymentResult(Activity context, FSPayResult payResult) {
        loading = new FSLoadingDialog(context);
        loading.show();
        // 禁止点击外部关闭
        loading.setCanceledOnTouchOutside(false);
        // 禁止返回键关闭
        loading.setCancelable(false);
        loading.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    // 返回true表示消费掉返回键事件，这样对话框就不会关闭
                    return true;
                }
                return false;
            }
        });

        pollingDisposable = Observable.interval(0, 3, TimeUnit.SECONDS)
                .take(10)
                .flatMap(attempt -> {
                    // 使用 RxJava 版本的支付结果查询
                    String orderId = payResult.getOrderId();
                    if (orderId == null || orderId.isEmpty()) {
                        return Observable.just(false);
                    }
                    return FoxSdkPayResult.payResult(context, orderId)
                            .subscribeOn(Schedulers.io())
                            .onErrorReturnItem(false);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (success) {
                        loading.dismiss();
                        showPaySuccessDialog(context);
                        if (pollingDisposable != null && !pollingDisposable.isDisposed()) {
                            pollingDisposable.dispose();
                        }
                    }
                }, throwable -> {
                    // 处理错误
                    loading.dismiss();
                    showPayFailedDialog(context);
                }, () -> {
                    // 完成时调用（10次尝试后）
                    if (loading == null || !loading.isShowing()) return; // 如果已经关闭就不重复处理
                    loading.dismiss();
                    showPayFailedDialog(context);
                });
    }

    private void showPaySuccessDialog(Activity context) {
        if (loading != null)
            loading.dismiss();
        FSAlertDialog.Builder builder = new FSAlertDialog.Builder(context)
                .setContentView(R.layout.fs_layout_pay_success)
                .setPositive("继续游戏", null)
                .setCancelable(false)
                .withClose()
                .setOnDismissListener(() -> {
                    if (fsPayResult != null && fsPayResult.getPayType() == FoxSdkPayEnum.FOX_COIN) {
                        return;
                    }
                    unregisterLifecycleCallbacks(context);
                });
        builder.build().show();
    }

    private void showPayFailedDialog(Activity context) {
        if (loading != null)
            loading.dismiss();
        FSAlertDialog.Builder builder = new FSAlertDialog.Builder(context)
                .setContentView(R.layout.fs_layout_pay_failed)
                .setPositive("重新购买", () -> {
                    getSdkConfig(context, mallIdFS, mallNameFS, priceFS, priceContentFS,
                            orderTimeFS, cpOrderIdFS);
                })
                .setOnDismissListener(() -> {
                    if (fsPayResult != null && fsPayResult.getPayType() == FoxSdkPayEnum.FOX_COIN) {
                        return;
                    }
                    unregisterLifecycleCallbacks(context);
                })
                .setCancelable(false)
                .setNegative("返回游戏",
                        context.getResources().getColor(R.color.fs_primary),
                        null,
                        "outline")
                .withClose();
        builder.build().show();
    }

    private void unregisterLifecycleCallbacks(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
        } else {
            context.getApplication().unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
        }
    }

    // 需要时可以取消轮询
    public void stopPolling() {
        if (pollingDisposable != null && !pollingDisposable.isDisposed()) {
            pollingDisposable.dispose();
        }
    }

    public void dispose() {
        disposables.clear();
        stopPolling();
    }

    private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                }

                @Override
                public void onActivityPaused(Activity activity) {
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    if (fsPayResult != null && fsPayResult.isCheckPay()) {
                        startPollingPaymentResult(activity, fsPayResult);
                    }
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                }

                @Override
                public void onActivityStarted(Activity activity) {
                }

                @Override
                public void onActivityStopped(Activity activity) {
                }
            };

    // 支付监听器接口
    public interface PayListener {
        void onPayResult(FSPayResult payResult);
    }

    public interface OnLoginListener {
        void onLogin(String userId, String token);
    }
}
