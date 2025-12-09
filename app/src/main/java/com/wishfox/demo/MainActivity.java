package com.wishfox.demo;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wishfox.foxsdk.data.model.entity.FSPayResult;
import com.wishfox.foxsdk.utils.FoxSdkLongingPayUtilsV1;
import com.wishfox.foxsdk.utils.FoxSdkUtils;

public class MainActivity extends AppCompatActivity {

    private FSPayResult fsPayResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e("FoxSdk","11111111111111111111");

        findViewById(R.id.tv_pay).setOnClickListener(v ->
                FoxSdkLongingPayUtilsV1.loginPay(
                        MainActivity.this,
                        "1",
                        "元宝",
                        "1",
                        "元宝",
                        System.currentTimeMillis(),
                        "284020251030173253512349495",
                        (userId, token) -> {
                            ((TextView) findViewById(R.id.tv_info)).setText("userId: " + userId + "\ntoken: " + token);
                        }, (payResult) -> {
                            // 预下单返回结果
                            fsPayResult = payResult;
                        }));

//        findViewById(R.id.tv_pay).postDelayed(FoxSdkUtils::hideFloatX, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
            FoxSdkLongingPayUtilsV1.startPollingPaymentResult(this, fsPayResult,new FoxSdkLongingPayUtilsV1.OnPayResultOperationListener() {

                @Override
                public void failedOperation(FoxSdkLongingPayUtilsV1.AginOperationType aginOperationType) {
                    if(aginOperationType == FoxSdkLongingPayUtilsV1.AginOperationType.REBUY){
                        // 重新购买,重走下单逻辑
                    }
                }

                @Override
                public void payResultDialogShow() {
                    // 支付结果弹窗显示,接收到支付结果后,将下单返回的参数置空
                    fsPayResult = null;
                }
            });
    }
}