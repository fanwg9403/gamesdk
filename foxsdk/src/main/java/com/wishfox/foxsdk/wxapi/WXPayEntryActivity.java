package com.wishfox.foxsdk.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.wishfox.foxsdk.utils.pay.FoxSdkWechatService;
import com.youth.banner.util.LogUtils;

public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FoxSdkWechatService.getIwxapi().handleIntent(getIntent(), this);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        FoxSdkWechatService.getIwxapi().handleIntent(intent, this);

    }

    @Override
    public void onReq(BaseReq req) {
        LogUtils.e("微信支付回调: " + req.toString());
    }

    @Override
    public void onResp(BaseResp resp) {
        PayResp resp1 = (PayResp) resp;
        String extData = resp1.extData;

        finish();
    }
}