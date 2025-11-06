package com.wishfox.foxsdk.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


import com.google.gson.Gson;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.wishfox.foxsdk.utils.FoxSdkLogger;
import com.wishfox.foxsdk.utils.pay.FoxSdkWechatService;

import io.reactivex.rxjava3.annotations.Nullable;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IWXAPI iwxapi = FoxSdkWechatService.getIwxapi();
        if (iwxapi != null) iwxapi.handleIntent(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        FoxSdkWechatService.getIwxapi().handleIntent(intent, this);
    }

    private static final String TAG = "WXEntryActivity";

    @Override
    public void onReq(BaseReq baseReq) {
        FoxSdkLogger.d(TAG, "onReq: " + baseReq);
    }

    @Override
    public void onResp(BaseResp baseResp) {
        //{"extMsg":"success","errCode":0,"openId":"oh59S6ZXPln8ZsImBCs628992qC0"}
        Gson gson = new Gson();
        com.youth.banner.util.LogUtils.e("微信支付回调: extData=" + gson.toJson(baseResp));
        String str = gson.toJson(baseResp);
        finish();
    }
}
