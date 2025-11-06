package com.wishfox.foxsdk.core;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.hjq.toast.Toaster;
import com.petterp.floatingx.FloatingX;
import com.petterp.floatingx.assist.FxDisplayMode;
import com.petterp.floatingx.assist.FxScopeType;
import com.petterp.floatingx.assist.helper.FxAppHelper;
import com.tencent.bugly.crashreport.CrashReport;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.ui.view.activity.FSHomeActivity;
import com.wishfox.foxsdk.utils.FoxSdkCommonExt;
import com.wishfox.foxsdk.utils.FoxSdkLogger;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;
import com.wishfox.foxsdk.utils.customerservice.QiyukfHelper;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:23
 */
@SuppressLint("StaticFieldLeak")
public class WishFoxSdk {

    private static final String TAG = "FoxSdk[Init]";
    private static boolean isInitialized = false;
    private static FoxSdkConfig config;
    private static Context context;

    private static boolean floatActive = true;

    public static void setFloatActive(boolean floatActive) {
        WishFoxSdk.floatActive = floatActive;
    }

    /**
     * 初始化SDK
     * 需要在Application中进行
     */
    public static void initialize(Context context, FoxSdkConfig config) {
        if (!(context instanceof Application)) {
            throw new IllegalStateException("WishFoxSdk 初始化需要在Application中进行");
        }

        WishFoxSdk.context = context.getApplicationContext();
        WishFoxSdk.config = config;
        WishFoxSdk.isInitialized = true;

        FoxSdkLogger.setDebug(config.isEnableLog());

        // 初始化网络组件
        FoxSdkRetrofitManager.initialize(config);

        // 初始化Toast工具
        Toaster.init((Application) context);

        // 初始化bugly
        CrashReport.initCrashReport(
                context,
                FoxSdkUtils.BUGLY_APPID,
                config.isEnableLog()
        );

        // 初始化七鱼客服
        QiyukfHelper.getInstance().init(context, FoxSdkBaseMviActivity.class);
        QiyukfHelper.getInstance().initKFSDK();

        // 初始化悬浮窗
        initFloatingWindow(context);

        if (config.isEnableLog()) {
            FoxSdkLogger.d(TAG, "WishFoxSDK 初始化成功！");
        }
    }

    /**
     * 初始化悬浮窗
     */
    private static void initFloatingWindow(Context context) {
        FxAppHelper helper = new FxAppHelper.Builder()
                .setTag(FoxSdkUtils.FloatXTag)
                .setContext(context)
                .setLayout(R.layout.fs_floating_view)
                .setOffsetXY(0, FoxSdkCommonExt.dp2px(context, config.getFloatXxOffset()))
                .setScopeType(FxScopeType.APP)
                .setDisplayMode(FxDisplayMode.ClickOnly)
                .setEnableAnimation(true)
                .addInstallBlackClass(
                        "com.wishfox.foxsdk.ui.view.activity.FSHomeActivity",
                        "com.wishfox.foxsdk.ui.view.activity.FSStarterPackActivity",
                        "com.wishfox.foxsdk.ui.view.activity.FSWinFoxCoinActivity",
                        "com.wishfox.foxsdk.ui.view.activity.FSRechargeRecordActivity",
                        "com.wishfox.foxsdk.ui.view.activity.FSGameRecordActivity",
                        "com.qiyukf.unicorn.ui.activity.ServiceMessageActivity",
                        "com.wishfox.foxsdk.ui.view.activity.FSMessageActivity",
                        "com.wishfox.foxsdk.ui.view.activity.FSWebActivity"
                )
                .setOnClickListener(v -> {
                    if (v != null && v.getContext() != null) {
                        FoxSdkViewExt.setOnClickListener(v, (cv) -> {
                            if (floatActive) {
                                Intent intent = new Intent(v.getContext(), FSHomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);

                                if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null) {
                                    FloatingX.control(FoxSdkUtils.FloatXTag).getView().postDelayed(() -> {
                                        FloatingX.configControl(FoxSdkUtils.FloatXTag).setEnableHalfHide(true, getConfig().getFloatXScale());
                                        if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null)
                                            FloatingX.control(FoxSdkUtils.FloatXTag).getView().setAlpha(0.5f);
                                        floatActive = false;
                                    }, 1500);
                                }
                            } else {
                                FloatingX.configControl(FoxSdkUtils.FloatXTag).setEnableHalfHide(false);
                                if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null)
                                    FloatingX.control(FoxSdkUtils.FloatXTag).getView().setAlpha(1f);
                                if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null) {
                                    FloatingX.control(FoxSdkUtils.FloatXTag).getView().postDelayed(() -> {
                                        floatActive = true;
                                        if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null) {
                                            FloatingX.control(FoxSdkUtils.FloatXTag).getView().postDelayed(() -> {
                                                FloatingX.configControl(FoxSdkUtils.FloatXTag).setEnableHalfHide(true, getConfig().getFloatXScale());
                                                if (FloatingX.control(FoxSdkUtils.FloatXTag).getView() != null)
                                                    FloatingX.control(FoxSdkUtils.FloatXTag).getView().setAlpha(0.5f);
                                                floatActive = false;
                                            }, 1500);
                                        }
                                    }, 150);
                                } else {
                                    floatActive = true;
                                }
                            }
                        });
                    }
                })
                .build();
        FloatingX.install(helper).show();
    }

    /**
     * 检查SDK是否已初始化
     */
    public static void requireInitialized() {
        if (!isInitialized) {
            throw new IllegalStateException("必须要先初始化WishFoxSDK. 初始化方法：WishFoxSDK.initialize()");
        }
    }

    /**
     * 获取配置信息
     */
    public static FoxSdkConfig getConfig() {
        requireInitialized();
        return config;
    }

    /**
     * 获取上下文
     */
    public static Context getContext() {
        requireInitialized();
        return context;
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
}
