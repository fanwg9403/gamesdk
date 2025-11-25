package com.wishfox.foxsdk.utils.pay;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.hjq.toast.Toaster;
import com.kuaiqian.fusedpay.entity.FusedPayRequest;
import com.kuaiqian.fusedpay.sdk.FusedPayApiFactory;
import com.kuaiqian.fusedpay.sdk.IFusedPayApi;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.utils.FoxSdkLogger;
import com.youth.banner.util.LogUtils;

import io.reactivex.rxjava3.core.Observable;

/**
 * 快钱支付
 */
public class FoxSdkQuickMoneyPay {

    /**
     * 调起聚合支付sdk
     *
     * @param platform   支付平台 :1 --飞凡通支付   2 --支付宝支付     3 --微信支付  4.微信小程序 5.云闪付  7.支付宝支付定制版
     * @param mpayInfo   移动支付的信息
     */
    public static Observable<Boolean> invokeFusedPaySDK(Context context, String platform, String mpayInfo) {

        return Observable.fromCallable(() -> {
            boolean installed = FoxSdkAliPay.isAlipayAvailable(context);
            if (!installed) {
                Toaster.show(context.getString(R.string.fs_str_ali_pay));
                return false;
            } else {
                FoxSdkLogger.e("FoxSdk", "调用聚合支付sdk==="+platform+"---mpayInfo==="+mpayInfo);
                FusedPayRequest payRequest = new FusedPayRequest();
                payRequest.setPlatform(platform);
                payRequest.setMpayInfo(mpayInfo);
                // CallBackSchemeId可以自定义，自定义的结果页面需实现IKuaiqianEventHandler接口
                payRequest.setCallbackSchemeId("com.wishfox.foxsdk.wxapi.QuickMoneyPayResultActivity");
                IFusedPayApi payApi = FusedPayApiFactory.createPayApi(context);
                payApi.pay(payRequest);
                return true;
            }
        });
    }
}
