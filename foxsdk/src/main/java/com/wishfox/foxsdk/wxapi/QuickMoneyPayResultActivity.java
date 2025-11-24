package com.wishfox.foxsdk.wxapi;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.hjq.toast.Toaster;
import com.kuaiqian.fusedpay.entity.FusedPayResult;
import com.kuaiqian.fusedpay.sdk.FusedPayApiFactory;
import com.kuaiqian.fusedpay.sdk.IFusedPayApi;
import com.kuaiqian.fusedpay.sdk.IFusedPayEventHandler;
import com.wishfox.foxsdk.utils.FoxSdkLogger;

/**
 * 调用聚合支付sdk的结果回调页
 */
public class QuickMoneyPayResultActivity extends Activity implements IFusedPayEventHandler {
    /**
     * 飞凡支付成功
     */
    public static final String RESULT_FFAN_PAY_OK = "100";

    /**
     * 微信支付成功
     */
    public static final String RESULT_WECHAT_PAY_OK = "0";
    /**
     * 微信小程序支付成功
     */
    public static final String RESULT_WECHAT_MINI_PROGRAM_PAY_OK = "00";

    public static final String KEY_IS_ALIPAY = "IS_ALIPAY";
    private boolean isAlipay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isAlipay = getIntent().getBooleanExtra(KEY_IS_ALIPAY, false);
        if (isAlipay) {
//            商户接入实际操作是去向商户后台查询支付结果
           // payResult.setText("支付宝支付查询结果:交易处理中");
            FoxSdkLogger.e("QuickMoneyPayResultActivity","支付宝支付查询结果:交易处理中");
        }
        IFusedPayApi api = FusedPayApiFactory.createPayApi(this);
        api.handleIntent(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        IFusedPayApi api = FusedPayApiFactory.createPayApi(this);
        api.handleIntent(getIntent(), this);
    }

    @Override
    public void onResponse(FusedPayResult paramResp) {
        FoxSdkLogger.e("QuickMoneyPayResultActivity","支付结果："+paramResp);

        String payResultCode = paramResp.getResultStatus();
        String payResultMessage = paramResp.getResultMessage();
        if (RESULT_FFAN_PAY_OK.equals(payResultCode) || RESULT_WECHAT_PAY_OK.equals(payResultCode) || RESULT_WECHAT_MINI_PROGRAM_PAY_OK.equals(payResultCode)) {
            Toaster.show("支付成功");
        } else {
            FoxSdkLogger.e("QuickMoneyPayResultActivity","支付结果：11111111"+paramResp);
            Toaster.show(payResultMessage);
        }
        finish();
    }
}
