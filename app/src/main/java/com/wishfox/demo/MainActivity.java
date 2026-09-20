package com.wishfox.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wishfox.foxsdk.core.FoxSdkOverlayManager;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.entity.FSPayResult;
import com.wishfox.foxsdk.utils.FoxSdkLongingPayUtilsV1;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String DEBUG_PREFERENCES = "wishfox_demo_debug";
    private static final String KEY_H5_URL = "h5_url";

    private FSPayResult fsPayResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableImmersiveMode();
        setContentView(R.layout.activity_main);

        Log.e("FoxSdk","11111111111111111111");

        findViewById(R.id.btn_h5_url).setOnClickListener(v -> showH5UrlDialog());
        Button orientationButton = findViewById(R.id.btn_toggle_orientation);
        updateOrientationButton(orientationButton);
        orientationButton.setOnClickListener(v -> toggleOrientation());

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

    private void showH5UrlDialog() {
        SharedPreferences preferences = getSharedPreferences(DEBUG_PREFERENCES, Context.MODE_PRIVATE);
        String currentUrl = preferences.getString(
                KEY_H5_URL,
                WishFoxSdk.getConfig().getH5HomeUrl()
        );

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint(getString(R.string.h5_url_hint));
        input.setText(currentUrl);
        input.setSelectAllOnFocus(true);

        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.h5_url_dialog_title)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.open_h5, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String url = normalizeUrl(input.getText().toString());
                    String origin = getOrigin(url);
                    if (origin == null) {
                        input.setError(getString(R.string.h5_url_invalid));
                        return;
                    }

                    preferences.edit().putString(KEY_H5_URL, url).apply();
                    FoxSdkOverlayManager.hide(MainActivity.this);
                    ((MyApp) getApplication()).initializeWishFoxSdk(url, origin);
                    FoxSdkOverlayManager.showH5(MainActivity.this);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private String normalizeUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (!url.isEmpty() && !url.contains("://")) {
            url = "http://" + url;
        }
        return url;
    }

    private String getOrigin(String value) {
        if (value == null || value.length() > 4096) {
            return null;
        }
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || uri.getUserInfo() != null) {
            return null;
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return null;
        }
        int port = uri.getPort();
        if (port == 0 || port > 65535) {
            return null;
        }
        boolean defaultPort = port == -1
                || ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        return scheme + "://" + host.toLowerCase(Locale.ROOT)
                + (defaultPort ? "" : ":" + port);
    }

    private void toggleOrientation() {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        setRequestedOrientation(landscape
                ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // MainActivity 未声明 orientation/screenSize configChanges，系统会完整重建 Activity。
    }

    private void updateOrientationButton(Button button) {
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        button.setText(landscape
                ? R.string.switch_to_portrait
                : R.string.switch_to_landscape);
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
