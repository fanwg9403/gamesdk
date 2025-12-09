package com.wishfox.foxsdk.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.util.Log;
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
public class FoxSdkLongingPayUtilsV1 {

    private static FSLoadingDialog loading;
    private static FSLoginDialog loginDialog;
    private static CompositeDisposable disposables = new CompositeDisposable();

    private static String mallIdFS = "";
    private static String mallNameFS = "";
    private static String priceFS = "";
    private static String priceContentFS = "";
    private static long orderTimeFS = 0;
    private static String cpOrderIdFS = "";

    /**
     * 登录弹窗
     *
     * @param mActivity
     * @param onLoginListener
     */
    public static void loginWishFox(Activity mActivity, OnLoginListener onLoginListener) {
        loginDialog = new FSLoginDialog(mActivity);
        loginDialog.setOnLoginClickListener((arg1, arg2, type,loadingDialog) -> {
//            loading = new FSLoadingDialog(mActivity);
//            loading.show();

            Disposable loginDisposable = login(arg1, arg2, type)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(success -> {
                        if (success) {
                            onLoginListener.onLogin(FSUserInfo.getInstance().getUserId(), FSLoginResult.getTokenEd());
                            loginDialog.dismiss();
//                            loading.dismiss();
                            loadingDialog.dismiss();
                        } else {
//                            loading.dismiss();
                            loadingDialog.dismiss();
                        }
                    }, throwable -> {
//                        loading.dismiss();
                        loadingDialog.dismiss();
                        Toaster.show(throwable.getMessage());
                    });

            disposables.add(loginDisposable);
        });
        loginDialog.show();
    }

    /**
     * 登录支付
     */
    public static void loginPay(
            Activity mActivity,
            String mallId,
            String mallName,
            String price,
            String priceContent,
            long orderTime,
            String cpOrderId,
            OnLoginListener onLoginListener,
            PayListener payListener
    ) {
        mallIdFS = mallId;
        mallNameFS = mallName;
        priceFS = price;
        priceContentFS = priceContent;
        orderTimeFS = orderTime;
        cpOrderIdFS = cpOrderId;

        if (FSUserInfo.getInstance() == null) { // 未登录
            loginDialog = new FSLoginDialog(mActivity);
            loginDialog.setOnLoginClickListener((arg1, arg2, type,loadingDialog) -> {
//                loading = new FSLoadingDialog(mActivity);
//                loading.show();

                Disposable loginDisposable = login(arg1, arg2, type)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(success -> {
                            if (success) {
                                onLoginListener.onLogin(FSUserInfo.getInstance().getUserId(), FSLoginResult.getTokenEd());
                                loginDialog.dismiss();
//                                loading.dismiss();
                                loadingDialog.dismiss();
                                getSdkConfig(mActivity, mallId, mallName, price, priceContent,
                                        orderTime, cpOrderId, payListener);
                            } else {
//                                loading.dismiss();
                                loadingDialog.dismiss();
                            }
                        }, throwable -> {
//                            loading.dismiss();
                            loadingDialog.dismiss();
                            Toaster.show(throwable.getMessage());
                        });

                disposables.add(loginDisposable);
            });
            loginDialog.show();
        } else { // 已登录调用支付
            onLoginListener.onLogin(FSUserInfo.getInstance().getUserId(), FSLoginResult.getTokenEd());
            getSdkConfig(mActivity, mallId, mallName, price, priceContent,
                    orderTime, cpOrderId, payListener);
        }
    }


    @SuppressLint("CheckResult")
    private static void getSdkConfig(Activity mActivity,
                                     String mallId,
                                     String mallName,
                                     String price,
                                     String priceContent,
                                     long orderTime,
                                     String cpOrderId,
                                     PayListener payListener) {
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
                                    payListener.onPayResult(payResult);
                                });

                    } else if (result.isError()) {
                        String errorMsg = result.getError() != null ? result.getError() : "支付失败";
                        Toaster.show(errorMsg);
                    } else if (result.isEmpty()) {
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
    private static Observable<Boolean> login(String phone, String code, int type) {
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
    private static Single<Boolean> getUserInfoObservable() {
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
    private static Single<Boolean> getUserVirtualInfoObservable() {
        FSHomeRepository repository = new FSHomeRepository();

        return repository.getUserVirtualInfo()
                .map(virtualInfoResult -> {
                    if (virtualInfoResult.isSuccess()) {
                        FSCoinInfo coinInfo = virtualInfoResult.getData();
                        // 保存虚拟货币信息
                        FSCoinInfo.save(coinInfo);
                    } else {
                        // 虚拟信息获取失败，记录日志但不影响登录流程
                        Log.w("FoxSdkLongingPayUtils", "获取用户虚拟信息失败: " +
                                (virtualInfoResult.getError() != null ? virtualInfoResult.getError() : "未知错误"));
                    }
                    // 虚拟信息获取成功或失败都不影响登录流程
                    Toaster.show("登录成功");
                    return true;
                })
                .onErrorReturn(throwable -> {
                    // 虚拟信息获取失败不影响登录成功
                    Log.w("FoxSdkLongingPayUtils", "获取用户虚拟信息异常: " + throwable.getMessage());
                    Toaster.show("登录成功");
                    return true;
                });
    }


    // 支付弹框
    public static void payDialog(
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
                    }
                });
        payDialog.show();
    }

    private static Disposable pollingDisposable;
    private static FSLoadingDialog loadings;

    private static boolean isCheckPay = false;
    public static void startPollingPaymentResult(Activity context, FSPayResult payResult, OnPayResultOperationListener mOnPayResultOperationListener) {
        if (payResult != null && payResult.isCheckPay()) {
            if(isCheckPay){
                return;
            }
            isCheckPay = true;
            if(loadings==null){
                loadings = new FSLoadingDialog(context);
                loadings.show();
            }
            // 禁止点击外部关闭
            loadings.setCanceledOnTouchOutside(false);
            // 禁止返回键关闭
            loadings.setCancelable(false);
            loadings.setOnKeyListener(new DialogInterface.OnKeyListener() {
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
                    .take(3)
                    .flatMap(attempt -> {
                        // 使用 RxJava 版本的支付结果查询
                        String orderId = payResult.getOrderId();
                        if (orderId == null || orderId.isEmpty()) {
                            FoxSdkLogger.e("FoxSdk", "订单ID为空");
                            return Observable.just(false);
                        }
                        return FoxSdkPayResult.payResult(context, orderId)
                                .subscribeOn(Schedulers.io())
                                .onErrorReturnItem(false);
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(success -> {
                        if (success) {
                            loadings.dismiss();
                            loadings=null;
                            isCheckPay = false;
                            showPaySuccessDialog(context,mOnPayResultOperationListener);
                            if (pollingDisposable != null && !pollingDisposable.isDisposed()) {
                                pollingDisposable.dispose();
                            }
                        }
                    }, throwable -> {
                        // 处理错误
                        loadings.dismiss();
                        loadings=null;
                        isCheckPay = false;
                        showPayFailedDialog(context, mOnPayResultOperationListener);
                    }, () -> {
                        // 完成时调用（5次尝试后）
                        if (loadings == null || !loadings.isShowing()) return; // 如果已经关闭就不重复处理
                        loadings.dismiss();
                        loadings=null;
                        isCheckPay = false;
                        showPayFailedDialog(context, mOnPayResultOperationListener);
                    });
        }
    }

    private static void showPaySuccessDialog(Activity context,OnPayResultOperationListener mOnPayResultOperationListener) {
        if (loading != null)
            loading.dismiss();
        FSAlertDialog.Builder builder = new FSAlertDialog.Builder(context)
                .setContentView(R.layout.fs_layout_pay_success)
                .setPositive("继续游戏", null)
                .setCancelable(false)
                .withClose()
                .setOnDismissListener(() -> {

                });
        builder.build().show();
        mOnPayResultOperationListener.payResultDialogShow();
    }

    private static void showPayFailedDialog(Activity context, OnPayResultOperationListener mOnPayResultOperationListener) {
        if (loading != null)
            loading.dismiss();
        FSAlertDialog.Builder builder = new FSAlertDialog.Builder(context)
                .setContentView(R.layout.fs_layout_pay_failed)
                .setPositive("重新购买", () -> {
                    mOnPayResultOperationListener.failedOperation(AginOperationType.REBUY);
                })
                .setOnDismissListener(() -> {
                    mOnPayResultOperationListener.failedOperation(AginOperationType.RETURN_GAME);
                })
                .setCancelable(false)
                .setNegative("返回游戏",
                        context.getResources().getColor(R.color.fs_primary),
                        null,
                        "outline")
                .withClose();
        builder.build().show();
        mOnPayResultOperationListener.payResultDialogShow();
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


    // 支付监听器接口
    public interface PayListener {
        void onPayResult(FSPayResult payResult);
    }

    public interface OnLoginListener {
        void onLogin(String userId, String token);
    }

    // 支付结果操作
    public interface OnPayResultOperationListener {
        //失败操作
        void failedOperation(AginOperationType aginOperationType);
        //结果弹窗显示
        void payResultDialogShow();
    }


    public enum AginOperationType {
        // 返回游戏
        RETURN_GAME,
        // 重新购买
        REBUY
    }
}
