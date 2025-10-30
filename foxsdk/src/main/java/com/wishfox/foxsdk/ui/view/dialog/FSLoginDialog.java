package com.wishfox.foxsdk.ui.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import com.hjq.toast.Toaster;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.core.WishFoxEntryActivity;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;
import com.wishfox.foxsdk.databinding.FsDialogLoginBinding;
import com.wishfox.foxsdk.ui.view.activity.FSWebActivity;
import com.wishfox.foxsdk.ui.view.widgets.FSLoadingDialog;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;
import com.wishfox.foxsdk.utils.custom.CustomTextWatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:02
 */
public class FSLoginDialog extends Dialog {

    private static FSLoginDialog _instance;

    private Context ctx;
    private FsDialogLoginBinding binding;
    private FSLoadingDialog loading;
    private OnLoginClickListener mListener;
    private Timer timeouter;
    private int time = 0;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private String phoneValue = "";
    private String verifyCodeValue = "";
    private String passwordValue = "";

    public FSLoginDialog(@NonNull Context context) {
        super(context, R.style.FSLoadingDialog);
        this.ctx = context;
        _instance = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FsDialogLoginBinding.inflate(LayoutInflater.from(ctx));
        setContentView(binding.getRoot());

        initViews();
        setupClickListeners();
        setupTextWatchers();
    }

    private void initViews() {
        binding.fsEtPhone.setText(phoneValue);
        binding.fsEtPassword.setText(passwordValue);
        binding.fsEtVerifyCode.setText(verifyCodeValue);

        // 初始化登录类型
        changeLoginType();

        // 检查WishFox是否安装
        boolean installed = FoxSdkUtils.isWishFoxInstalled(ctx).blockingLast();
        binding.fsAuthLogin.setVisibility(View.GONE);
        binding.fsPrimaryLogin.setVisibility(View.VISIBLE);
        binding.fsIvBack.setVisibility(View.GONE);
        binding.fsVRight.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        // 验证码登录
        FoxSdkViewExt.setOnClickListener(binding.fsTvVerifyCodeLogin, v -> {
            if (binding.fsTvVerifyCodeLogin.getAlpha() == 0.5f) {
                binding.fsTvVerifyCodeLogin.setAlpha(1.0f);
                binding.fsTvPasswordLogin.setAlpha(0.5f);
                changeLoginType();
            }
        });

        // 密码登录
        FoxSdkViewExt.setOnClickListener(binding.fsTvPasswordLogin, v -> {
            if (binding.fsTvPasswordLogin.getAlpha() == 0.5f) {
                binding.fsTvPasswordLogin.setAlpha(1.0f);
                binding.fsTvVerifyCodeLogin.setAlpha(0.5f);
                changeLoginType();
            }
        });

        // 同意协议
        FoxSdkViewExt.setOnClickListener(binding.fsFlAgree, v -> {
            binding.fsCtvAgree.setChecked(!binding.fsCtvAgree.isChecked());
            checkCanLogin();
        });

        // 登录按钮
        FoxSdkViewExt.setOnClickListener(binding.fsTvLogin, v -> {
            if (binding.fsTvLogin.getAlpha() != 1.0f) return;

            loading = new FSLoadingDialog(ctx);
            loading.show();

            if (binding.fsLlVerifyCode.getVisibility() == View.VISIBLE) {
                if (mListener != null) {
                    mListener.onLoginClick(phoneValue, verifyCodeValue, 2);
                }
            } else {
                if (mListener != null) {
                    mListener.onLoginClick(phoneValue, passwordValue, 1);
                }
            }
        });

        // 发送验证码
        FoxSdkViewExt.setOnClickListener(binding.fsTvSendVerifyCode, v -> {
            if (timeouter != null) return;

            if (TextUtils.isEmpty(phoneValue)) {
                Toaster.show("请输入手机号");
                return;
            } else if (phoneValue.length() != 11 || !isValidPhone(phoneValue)) {
                Toaster.show("请输入正确的手机号");
                return;
            }

            loading = new FSLoadingDialog(ctx);
            loading.show();

            sendSmsCode(phoneValue);
        });

        // 返回按钮
        FoxSdkViewExt.setOnClickListener(binding.fsIvBack, v -> {
            binding.fsAuthLogin.setVisibility(View.VISIBLE);
            binding.fsPrimaryLogin.setVisibility(View.GONE);
        });

        // 其他登录方式
        FoxSdkViewExt.setOnClickListener(binding.fsTvOtherLogin, v -> {
            binding.fsAuthLogin.setVisibility(View.GONE);
            binding.fsPrimaryLogin.setVisibility(View.VISIBLE);
        });

        // 认证登录
        FoxSdkViewExt.setOnClickListener(binding.fsTvAuthLogin, v -> ctx.startActivity(new Intent(ctx, WishFoxEntryActivity.class)
                .setAction(FoxSdkConfig.WishFoxActions.WISH_FOX_AUTH_ACTION)));

        // 用户协议
        FoxSdkViewExt.setOnClickListener(binding.fsTvUserAgreement, v -> FSWebActivity.startWithUrl(ctx, "https://world.wishfoxs.com/gameOfUser.html"));

        // 隐私协议
        FoxSdkViewExt.setOnClickListener(binding.fsTvPrivacyAgreement, v -> FSWebActivity.startWithUrl(ctx, "https://world.wishfoxs.com/gaemOfPrivacy.html"));
    }

    private void setupTextWatchers() {
        binding.fsEtPhone.addTextChangedListener(new CustomTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                phoneValue = s.toString();
                checkCanLogin();
            }
        });

        binding.fsEtVerifyCode.addTextChangedListener(new CustomTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                verifyCodeValue = s.toString();
                checkCanLogin();
            }
        });

        binding.fsEtPassword.addTextChangedListener(new CustomTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordValue = s.toString();
                checkCanLogin();
            }
        });
    }

    private void sendSmsCode(String userName) {
        Map<String, String> params = new HashMap<>();
        params.put("channel_id", WishFoxSdk.getConfig().getChannelId());
        params.put("app_id", WishFoxSdk.getConfig().getAppId());
        params.put("user_name", userName);

        Single.fromCallable(() -> FoxSdkRetrofitManager.getApiService().sendSmsCode(params))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    loading.dismiss();
                    if (response.blockingGet().getCode() == 200) {
                        Toaster.show("验证码发送成功");
                        startTimeout();
                    } else {
                        Toaster.show(response.blockingGet().getMsg() != null ? response.blockingGet().getMsg() : "验证码发送失败");
                    }
                }, throwable -> {
                    loading.dismiss();
                    Toaster.show(throwable.getMessage() != null ? throwable.getMessage() : "验证码发送失败");
                });
    }

    private void startTimeout() {
        if (timeouter != null) return;

        time = 60;
        timeouter = new Timer();
        timeouter.schedule(new TimerTask() {
            @Override
            public void run() {
                if (time > 0) {
                    mainHandler.post(() -> binding.fsTvSendVerifyCode.setText(time + "S"));
                    time--;
                } else {
                    mainHandler.post(() -> binding.fsTvSendVerifyCode.setText(R.string.fs_send_verify_code));
                    timeouter.cancel();
                    timeouter = null;
                }
            }
        }, 0, 1000);
    }

    private void checkCanLogin() {
        boolean phonePass = phoneValue.length() == 11;
        boolean verifyCodePass = verifyCodeValue.length() > 3;
        boolean passwordPass = passwordValue.length() > 5;
        boolean isAgreement = binding.fsCtvAgree.isChecked();

        boolean canLogin;
        if (binding.fsLlVerifyCode.getVisibility() == View.VISIBLE) {
            canLogin = phonePass && verifyCodePass && isAgreement;
        } else {
            canLogin = phonePass && passwordPass && isAgreement;
        }

        binding.fsTvLogin.setEnabled(canLogin);
        binding.fsTvLogin.setAlpha(canLogin ? 1.0f : 0.5f);
    }

    private void changeLoginType() {
        boolean isVerifyCodeLogin = binding.fsTvVerifyCodeLogin.getAlpha() == 1.0f;
        binding.fsLlVerifyCode.setVisibility(isVerifyCodeLogin ? View.VISIBLE : View.GONE);
        binding.fsLlPassword.setVisibility(!isVerifyCodeLogin ? View.VISIBLE : View.GONE);

        if (isVerifyCodeLogin) {
            binding.fsTvLogin.setText(ctx.getString(R.string.fs_register_and_login));
        } else {
            binding.fsTvLogin.setText(ctx.getString(R.string.fs_login));
        }
    }

    private boolean isValidPhone(String phone) {
        Pattern pattern = Pattern.compile("^1[3-9]\\d{9}$");
        return pattern.matcher(phone).matches();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (loading != null) {
            loading.dismiss();
        }
        _instance = null;
        if (timeouter != null) {
            timeouter.cancel();
            timeouter = null;
        }
    }

    /**
     * 登录按钮点击事件
     */
    public FSLoginDialog setOnLoginClickListener(OnLoginClickListener listener) {
        this.mListener = listener;
        return this;
    }

    public interface OnLoginClickListener {
        void onLoginClick(String phone, String codeOrPassword, int loginType);
    }

    public static void dismissInstance() {
        if (_instance != null) {
            _instance.dismiss();
        }
    }

    public static void dismissLoading() {
        if (_instance != null && _instance.loading != null) {
            _instance.loading.dismiss();
        }
    }
}
