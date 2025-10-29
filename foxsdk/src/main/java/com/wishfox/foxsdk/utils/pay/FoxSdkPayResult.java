package com.wishfox.foxsdk.utils.pay;

import android.content.Context;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.data.model.FoxSdkBaseResponse;
import com.wishfox.foxsdk.data.model.entity.FSCheckOrder;
import com.wishfox.foxsdk.ui.view.widgets.FSLoadingDialog;
import com.wishfox.foxsdk.data.network.FoxSdkApiService;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;
import com.wishfox.foxsdk.core.WishFoxSdk;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:06
 */
public class FoxSdkPayResult {

    private static FSLoadingDialog loading;

    public static void queryResult(Context context, String orderId, io.reactivex.rxjava3.functions.Consumer<Boolean> callback) {
        payResult(context, orderId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback, throwable -> {
                    try {
                        Toaster.show(throwable.getMessage());
                        callback.accept(false);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    public static Observable<Boolean> payResult(Context context, String orderId) {
        return Single.fromCallable(() -> {
            FoxSdkApiService service = FoxSdkRetrofitManager.getApiService();
            Map<String, Object> params = new HashMap<>();
            params.put("order_id", orderId);
            params.put("channel_id", WishFoxSdk.getConfig().getChannelId());
            params.put("app_id", WishFoxSdk.getConfig().getAppId());

            try {
                // 调用 suspend 函数，使用 blockingGet() 来在 RxJava 中调用
                FoxSdkBaseResponse<FSCheckOrder> response = service.getOrderDetail(params).blockingGet();
                return parsePayResult(response);
            } catch (Exception e) {
                throw new RuntimeException("查询支付结果失败: " + e.getMessage());
            }
        })
                .subscribeOn(Schedulers.io())
                .toObservable();
    }

    private static boolean parsePayResult(FoxSdkBaseResponse<FSCheckOrder> response) {
        try {
            if (response.getCode() == 200 && response.getData() != null) {
                FSCheckOrder orderDetail = response.getData();
                Integer status = orderDetail.getStatus();

                // 0待支付1已支付2已发货-1支付失败
                if (status != null) {
                    return status == 1 || status == 2; // 1已支付 2已发货
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
