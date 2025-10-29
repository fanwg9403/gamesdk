package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wishfox.foxsdk.R;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:08
 */
public class FSLoadingDialog extends Dialog {

    private static final String TAG = "FoxSdk[LoadingDialog]";

    private TextView tvMessage;

    public FSLoadingDialog(@NonNull Context context) {
        super(context, R.style.FSLoadingDialog);
        initView();
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        android.view.View view = inflater.inflate(R.layout.fs_dialog_loading, null);
        setContentView(view);

        tvMessage = view.findViewById(R.id.fs_tv_message);

        // 设置窗口背景透明和尺寸
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /**
     * 设置提示消息
     *
     * @param message 消息内容，为null或空字符串时隐藏消息文本
     */
    public void setMessage(@Nullable String message) {
        if (TextUtils.isEmpty(message)) {
            tvMessage.setVisibility(android.view.View.GONE);
        } else {
            tvMessage.setVisibility(android.view.View.VISIBLE);
            tvMessage.setText(message);
        }
    }

    /**
     * 重写show方法，增加生命周期安全检查
     */
    @Override
    public void show() {
        Context context = getContext();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                try {
                    super.show();
                } catch (Exception e) {
                    Log.e(TAG, "LoadingDialog 展示失败", e);
                }
            }
        } else {
            try {
                super.show();
            } catch (Exception e) {
                Log.e(TAG, "LoadingDialog 展示失败", e);
            }
        }
    }

    /**
     * 重写dismiss方法，增加异常处理
     */
    @Override
    public void dismiss() {
        try {
            super.dismiss();
        } catch (Exception e) {
            Log.e(TAG, "LoadingDialog 隐藏失败", e);
        }
    }
}
