package com.wishfox.demo;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
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
        enableImmersiveMode();
        setContentView(R.layout.activity_main);

        Log.e("FoxSdk","11111111111111111111");

        findViewById(R.id.tv_pay).setOnClickListener(v ->
                FoxSdkLongingPayUtilsV1.loginPay(
                        MainActivity.this,
                        "1",
                        "元宝",
                        "0.01",
                        "元宝",
                        System.currentTimeMillis(),
                        "284020251030173253512349491135453",
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
        enableImmersiveMode();
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
        }
    }

    /**
     * 让 Demo 宿主保持与三方游戏一致的全屏沉浸状态。
     * SDK Overlay 需要在不修改这些窗口标记的前提下覆盖到当前 Activity 上。
     */
    private void enableImmersiveMode() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            if (attributes != null) {
                attributes.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                window.setAttributes(attributes);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars()
                );
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        }
        int systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        window.getDecorView().setSystemUiVisibility(systemUiVisibility);
    }
}
