package com.wishfox.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wishfox.foxsdk.core.FoxSdkOverlayManager;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.entity.FSPayResult;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.utils.FoxSdkLongingPayUtilsV1;
import com.wishfox.foxsdk.media.FSMediaFeedback;
import com.wishfox.foxsdk.media.FSMediaSaveCoordinator;
import com.wishfox.foxsdk.media.FSMediaShareCoordinator;
import com.wishfox.foxsdk.media.FSShareTargetResolver;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String DEBUG_PREFERENCES = "wishfox_demo_debug";
    private static final String KEY_H5_URL = "h5_url";
    private static final String SHARE_TAG = "WishFoxDemo[Share]";
    private static final String SHARE_IMAGE_URL =
            "https://b0.bdstatic.com/ugc/personal_page_creator/aj7fsR4L-exmhvLeqz2oPgf6ce7355efe3a42a06761baec5847d31.jpg";
    private static final String STATE_SHARE_ASSET = "demo_share_asset";
    private final int[] shareButtonIds = {
            R.id.btn_share_wechat, R.id.btn_share_wechat_timeline,
            R.id.btn_share_qq,
            R.id.btn_share_douyin, R.id.btn_share_xiaohongshu
    };
    private FSMediaSaveCoordinator demoImageSaver;
    private FSMediaShareCoordinator demoImageSharer;
    private TextView shareStatus;
    private ProgressBar shareProgress;
    private String cachedShareAssetId;
    private String activeSaveId;
    private String activeShareId;
    private boolean shareBusy;
    private boolean demoResumed;

    private FSPayResult fsPayResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().getBooleanExtra(DemoSplashActivity.EXTRA_ENTERED_FROM_SPLASH, false)) {
            startActivity(new Intent(this, DemoSplashActivity.class));
            finish();
            return;
        }
        enableImmersiveMode();
        setContentView(R.layout.activity_main);

        Log.e("FoxSdk","11111111111111111111");

        initEnvironmentButtons();
        initShareButtons(savedInstanceState);
        findViewById(R.id.btn_h5_url).setOnClickListener(v -> showH5UrlDialog());
        Button orientationButton = findViewById(R.id.btn_toggle_orientation);
        updateOrientationButton(orientationButton);
        orientationButton.setOnClickListener(v -> toggleOrientation());

        findViewById(R.id.btn_login).setOnClickListener(v ->
                FoxSdkLongingPayUtilsV1.loginWishFox(
                        MainActivity.this,
                        (userId, token) -> ((TextView) findViewById(R.id.tv_info))
                                .setText("userId: " + userId + "\ntoken: " + token)
                ));

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

    private void updateDynamicParamDisplay() {
        TextView valueView = findViewById(R.id.tv_dynamic_param);
        if (valueView == null) return;
        if (!WishFoxSdk.isInitialized()) {
            valueView.setText(R.string.dynamic_param_not_initialized);
            return;
        }
        // 与请求头和 H5 使用同一份 SDK 数据，原样展示，不缓存、不解析或重排 JSON。
        String dynamicParam = WishFoxSdk.getConfig().getDynamicParam();
        if (dynamicParam == null || dynamicParam.isEmpty()) {
            valueView.setText(R.string.dynamic_param_empty);
        } else {
            valueView.setText(dynamicParam);
        }
    }

    private void initShareButtons(Bundle state) {
        demoImageSaver = new FSMediaSaveCoordinator(this);
        demoImageSharer = new FSMediaShareCoordinator(this);
        shareStatus = findViewById(R.id.tv_share_status);
        shareProgress = findViewById(R.id.progress_share);
        if (state != null && SHARE_IMAGE_URL.equals(state.getString("demo_share_image_url"))) {
            cachedShareAssetId = state.getString(STATE_SHARE_ASSET);
        }
        String[] targets = {"wechat_friend", "wechat_timeline", "qq_friend", "douyin", "xiaohongshu"};
        for (int i = 0; i < shareButtonIds.length; i++) {
            final String target = targets[i];
            findViewById(shareButtonIds[i]).setOnClickListener(v -> beginImageShare(target));
        }
    }

    private boolean canShareNow() {
        return demoResumed && !isFinishing() && !isDestroyed();
    }

    private void beginImageShare(String targetId) {
        if (shareBusy || !canShareNow()) return;
        FSShareTargetResolver.Target target = FSShareTargetResolver.find(targetId);
        String availability = FSShareTargetResolver.check(this, target, "image/jpeg");
        if (availability != null) {
            showShareError(target, availability);
            return;
        }
        setShareBusy(true);
        if (cachedShareAssetId != null) {
            launchSavedImage(target, cachedShareAssetId);
            return;
        }
        final String saveId = "demo-save-" + UUID.randomUUID();
        activeSaveId = saveId;
        setShareStatus("正在下载测试图片，请稍候…");
        try {
            JSONObject params = new JSONObject().put("requestId", saveId)
                    .put("source", new JSONObject().put("type", "https_url").put("value", SHARE_IMAGE_URL))
                    .put("fileName", "wishfox-share-test.jpg").put("mimeType", "image/jpeg")
                    .put("saveToGallery", false).put("legacyGalleryMode", "none");
            String error = demoImageSaver.validate(params);
            if (error != null) {
                activeSaveId = null;
                showShareError(target, error);
                return;
            }
            boolean accepted = demoImageSaver.start(saveId, params, new FSMediaSaveCoordinator.Callback() {
                @Override public void onProgress(JSONObject data) {
                    if (!saveId.equals(activeSaveId)) return;
                    if ("downloading".equals(data.optString("stage"))) {
                        String progress = data.isNull("percent") ? "已下载 " + data.optLong("bytesReceived") / 1024 + " KB"
                                : data.optInt("percent") + "%";
                        setShareStatus("正在下载测试图片：" + progress);
                    } else {
                        setShareStatus("正在校验并保存测试图片…");
                    }
                }

                @Override public void onResult(JSONObject data) {
                    if (!saveId.equals(activeSaveId)) return;
                    activeSaveId = null;
                    if (!"saved".equals(data.optString("sandboxStatus"))) {
                        JSONObject error = data.optJSONObject("error");
                        showShareError(target, error == null ? "DOWNLOAD_FAILED" : error.optString("code"));
                        return;
                    }
                    cachedShareAssetId = data.optString("assetId", null);
                    Log.d(SHARE_TAG, "image ready: bytes=" + data.optLong("sizeBytes")
                            + ", mime=" + data.optString("mimeType") + ", sha256=" + data.optString("sha256"));
                    if (!canShareNow()) {
                        showShareError(target, "HOST_NOT_RESUMED");
                        return;
                    }
                    launchSavedImage(target, cachedShareAssetId);
                }
            });
            if (!accepted) {
                activeSaveId = null;
                showShareError(target, "BUSY");
            }
        } catch (JSONException error) {
            activeSaveId = null;
            showShareError(target, "INVALID_ARGUMENT");
        }
    }

    private void launchSavedImage(FSShareTargetResolver.Target target, String assetId) {
        final String shareId = "demo-share-" + UUID.randomUUID();
        activeShareId = shareId;
        setShareStatus("正在准备分享图片…");
        String error = demoImageSharer.start(shareId, target.id, assetId, new FSMediaShareCoordinator.Callback() {
            @Override public boolean canLaunch() { return canShareNow() && shareId.equals(activeShareId); }

            @Override public void onProgress(JSONObject data) {
                if (!shareId.equals(activeShareId)) return;
                String stage = data.optString("stage");
                setShareStatus("launching".equals(stage)
                        ? "正在打开" + target.appName + "…" : "正在准备分享图片…");
            }

            @Override public void onResult(JSONObject data) {
                if (!shareId.equals(activeShareId)) return;
                activeShareId = null;
                if ("launched".equals(data.optString("status"))) {
                    setShareBusy(false);
                    Log.d(SHARE_TAG, "share launched: galleryStatus=" + data.optString("galleryStatus"));
                    setShareStatus("已发起 " + target.id + " 分享（" + target.appName + "）。\n"
                            + "请在目标应用检查图片或自行选图；此状态不代表发送或发布成功。");
                } else {
                    JSONObject error = data.optJSONObject("error");
                    showShareError(target, error == null ? "LAUNCH_FAILED" : error.optString("code"));
                }
            }
        });
        if (error != null) {
            activeShareId = null;
            showShareError(target, error);
        }
    }

    private void showShareError(FSShareTargetResolver.Target target, String code) {
        if ("IMAGE_NOT_FOUND".equals(code)) cachedShareAssetId = null;
        setShareBusy(false);
        String message = FSMediaFeedback.message(code, target == null ? null : target.appName);
        setShareStatus(message + "\n错误码：" + code);
        Log.w(SHARE_TAG, "failed: target=" + (target == null ? "unknown" : target.id) + ", code=" + code);
        if (canShareNow()) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setShareStatus(String message) {
        if (!isDestroyed() && shareStatus != null) shareStatus.setText(message);
    }

    private void setShareBusy(boolean busy) {
        shareBusy = busy;
        if (isDestroyed() || shareProgress == null) return;
        shareProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        for (int id : shareButtonIds) findViewById(id).setEnabled(!busy);
    }

    private void initEnvironmentButtons() {
        MyApp app = (MyApp) getApplication();
        TextView label = findViewById(R.id.tv_environment_label);
        Button test = findViewById(R.id.btn_env_test);
        Button pre = findViewById(R.id.btn_env_pre);
        Button production = findViewById(R.id.btn_env_production);
        updateEnvironmentLabel(label, app.getEnvironment());
        test.setOnClickListener(v -> switchEnvironment(app, label, MyApp.Environment.TEST));
        pre.setOnClickListener(v -> switchEnvironment(app, label, MyApp.Environment.PRE));
        production.setOnClickListener(v -> switchEnvironment(app, label, MyApp.Environment.PRODUCTION));
    }

    private void switchEnvironment(MyApp app, TextView label, MyApp.Environment environment) {
        if (app.getEnvironment() == environment) return;
        app.switchEnvironment(environment);
        updateDynamicParamDisplay();
        updateEnvironmentLabel(label, environment);
        android.widget.Toast.makeText(this,
                "已切换到" + environment.label + "环境，请重新登录",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void updateEnvironmentLabel(TextView label, MyApp.Environment environment) {
        label.setText("API 环境：" + environment.label + "\n" + environment.baseUrl);
    }

    private void showH5UrlDialog() {
        SharedPreferences preferences = getSharedPreferences(DEBUG_PREFERENCES, Context.MODE_PRIVATE);
        FSLoginResult login = FSLoginResult.getInstance();
        String defaultUrl = login == null ? "" : login.getH5Url();
        String currentUrl = preferences.getString(KEY_H5_URL, defaultUrl);

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
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.h5_url_dialog_title)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.open_h5, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String url = normalizeUrl(input.getText().toString());
                    if (!isDebugUrl(url)) {
                        input.setError(getString(R.string.h5_url_invalid));
                        return;
                    }
                    preferences.edit().putString(KEY_H5_URL, url).apply();
                    FoxSdkOverlayManager.showDebugH5(MainActivity.this, url);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private String normalizeUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (!url.isEmpty() && !url.contains("://")) url = "http://" + url;
        return url;
    }

    private boolean isDebugUrl(String value) {
        if (value == null || value.length() > 4096) return false;
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || uri.getUserInfo() != null) return false;
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) return false;
        int port = uri.getPort();
        return port != 0 && port <= 65535;
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
        updateDynamicParamDisplay();
        demoResumed = true;
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
    protected void onPause() {
        demoResumed = false;
        // 下载时离开页面不在回来后自动唤端；再次点击即可重新发起。
        if (activeSaveId != null && demoImageSaver != null) {
            String saveId = activeSaveId;
            activeSaveId = null;
            demoImageSaver.cancel(saveId);
            setShareBusy(false);
            setShareStatus("图片下载已取消，返回后请重新点击分享按钮。");
        }
        if (demoImageSharer != null) demoImageSharer.onHostPaused();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cachedShareAssetId != null) outState.putString(STATE_SHARE_ASSET, cachedShareAssetId);
        outState.putString("demo_share_image_url", SHARE_IMAGE_URL);
    }

    @Override
    protected void onDestroy() {
        demoResumed = false;
        activeSaveId = null;
        activeShareId = null;
        if (demoImageSaver != null) demoImageSaver.destroy();
        if (demoImageSharer != null) demoImageSharer.destroy();
        super.onDestroy();
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
