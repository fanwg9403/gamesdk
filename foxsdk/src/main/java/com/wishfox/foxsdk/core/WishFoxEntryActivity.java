package com.wishfox.foxsdk.core;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.utils.FoxSdkUtils;

import java.io.ByteArrayOutputStream;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:32
 */
public class WishFoxEntryActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置沉浸式状态栏
        ImmersionBar.with(this)
                .transparentBar()
                .init();

        // 延迟处理Intent，确保UI初始化完成
        handler.postDelayed(() -> handleIntent(getIntent()), 200);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * 处理Intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case FoxSdkConfig.WishFoxActions.WISH_FOX_AUTH_ACTION:
                handleAuthAction();
                break;

            case FoxSdkConfig.WishFoxActions.WISH_FOX_AUTH_RESULT_ACTION:
                handleAuthResultAction(intent);
                break;
        }
    }

    /**
     * 处理认证请求
     */
    private void handleAuthAction() {
        handler.post(() -> {
            boolean installed = FoxSdkUtils.isWishFoxInstalled(this).blockingLast();

            if (installed) {
                startWishFoxAuth();
            } else {
                // 目标应用未安装则跳转H5下载页
                redirectToDownloadPage();
            }
        });
    }

    /**
     * 启动WishFox认证
     */
    private void startWishFoxAuth() {
        try {
            Intent sendIntent = new Intent(FoxSdkConfig.WishFoxActions.WISH_FOX_AUTH_ACTION);
            sendIntent.setComponent(new ComponentName(
                    FoxSdkConfig.WISH_FOX_PACKAGE_NAME,
                    FoxSdkConfig.WISH_FOX_AUTH_LOGIN_ACTIVITY
            ));

            // 设置额外参数
            sendIntent.putExtra("appName", "WishFoxSdkDemo");
            sendIntent.putExtra("package", getPackageName());

            // 添加图标
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_kf_icon);
            if (bitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                sendIntent.putExtra("icon", byteArray);
                bitmap.recycle();
            }

            startActivity(sendIntent);
            finish();

        } catch (ActivityNotFoundException e) {
            // 目标应用未安装则跳转H5下载页
            redirectToDownloadPage();
        }
    }

    /**
     * 跳转到下载页面
     */
    private void redirectToDownloadPage() {
        Intent downloadIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://world.wishfoxs.com/downloadApp.html")
        );
        startActivity(downloadIntent);
        finish();
    }

    /**
     * 处理认证结果
     */
    private void handleAuthResultAction(Intent intent) {
        String message = intent.getStringExtra("message");
        if (!TextUtils.isEmpty(message)) {
            Toaster.show(message);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
