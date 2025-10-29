package com.wishfox.foxsdk.ui.view.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.databinding.FsAlertDialogBinding;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:57
 */
public class FSAlertDialog extends Dialog {

    @SuppressLint("StaticFieldLeak")
    public static FsAlertDialogBinding binding;

    public FSAlertDialog(Context context) {
        super(context, R.style.FSLoadingDialog);
        binding = FsAlertDialogBinding.inflate(LayoutInflater.from(context));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(binding.getRoot());
    }

    /**
     * Builder 对象
     */
    public static class Builder {
        private Context ctx;
        private String title;
        private int titleColor = Color.WHITE;
        private String message;
        private int messageColor = Color.WHITE;
        private String positiveButtonText;
        private int positiveButtonTextColor = Color.WHITE;
        private OnButtonClickListener positiveButtonClickListener;
        private String negativeButtonText;
        private int negativeButtonTextColor = Color.WHITE;
        private OnButtonClickListener negativeButtonClickListener;
        private String negativeButtonTheme;
        private boolean isCancelable = true;
        private boolean openClose = false;
        private OnDismissListener onDismissListener;
        private View contentView;

        public Builder(Context context) {
            this.ctx = context;
        }

        public Builder setTitle(String title, int color) {
            this.title = title;
            this.titleColor = color;
            return this;
        }

        public Builder setTitle(int titleRes, int color) {
            this.title = ctx.getString(titleRes);
            this.titleColor = color;
            return this;
        }

        public Builder setMessage(String message, int color) {
            this.message = message;
            this.messageColor = color;
            return this;
        }

        public Builder setMessage(int messageRes, int textColor) {
            this.message = ctx.getString(messageRes);
            this.messageColor = textColor;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setTitle(int titleRes) {
            this.title = ctx.getString(titleRes);
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setMessage(int messageRes) {
            this.message = ctx.getString(messageRes);
            return this;
        }

        public Builder setPositive(String text, int textColor, OnButtonClickListener listener) {
            this.positiveButtonText = text;
            this.positiveButtonClickListener = listener;
            this.positiveButtonTextColor = textColor;
            return this;
        }

        public Builder setPositive(int textRes, int textColor, OnButtonClickListener listener) {
            this.positiveButtonText = ctx.getString(textRes);
            this.positiveButtonClickListener = listener;
            this.positiveButtonTextColor = textColor;
            return this;
        }

        public Builder setPositive(String text, OnButtonClickListener listener) {
            this.positiveButtonText = text;
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setPositive(int textRes, OnButtonClickListener listener) {
            this.positiveButtonText = ctx.getString(textRes);
            this.positiveButtonClickListener = listener;
            return this;
        }

        /**
         * @param theme "fill":常规 "outline": 文字同色外边框
         */
        public Builder setNegative(String text, int textColor, OnButtonClickListener listener, String theme) {
            this.negativeButtonText = text;
            this.negativeButtonClickListener = listener;
            this.negativeButtonTextColor = textColor;
            this.negativeButtonTheme = theme;
            return this;
        }

        /**
         * @param theme "fill":常规 "outline": 文字同色外边框
         */
        public Builder setNegative(int textRes, int textColor, OnButtonClickListener listener, String theme) {
            this.negativeButtonText = ctx.getString(textRes);
            this.negativeButtonClickListener = listener;
            this.negativeButtonTextColor = textColor;
            this.negativeButtonTheme = theme;
            return this;
        }

        public Builder setNegative(String text, OnButtonClickListener listener) {
            this.negativeButtonText = text;
            this.negativeButtonClickListener = listener;
            return this;
        }

        public Builder setNegative(int textRes, OnButtonClickListener listener) {
            this.negativeButtonText = ctx.getString(textRes);
            this.negativeButtonClickListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.isCancelable = cancelable;
            return this;
        }

        public Builder setOnDismissListener(OnDismissListener listener) {
            this.onDismissListener = listener;
            return this;
        }

        public Builder setContentView(View view) {
            this.contentView = view;
            return this;
        }

        public Builder setContentView(int layoutId) {
            this.contentView = LayoutInflater.from(ctx).inflate(layoutId, null);
            return this;
        }

        public Builder withClose() {
            this.openClose = true;
            return this;
        }

        public FSAlertDialog build() {
            FSAlertDialog dialog = new FSAlertDialog(ctx);

            // 标题
            binding.fsTvAlertTitle.setVisibility(title != null ? View.VISIBLE : View.GONE);
            binding.fsTvAlertTitle.setText(title);
            binding.fsTvAlertTitle.setTextColor(titleColor);

            // 内容
            binding.fsTvAlertMessage.setVisibility(message != null ? View.VISIBLE : View.GONE);
            binding.fsTvAlertMessage.setText(message);
            binding.fsTvAlertMessage.setTextColor(messageColor);

            if (contentView != null) {
                binding.fsTvAlertTitle.setVisibility(View.INVISIBLE);
                binding.fsTvAlertMessage.setVisibility(View.INVISIBLE);
                binding.fsLlAlertContainer.setVisibility(View.VISIBLE);
                binding.fsLlAlertContainer.removeAllViews();
                binding.fsLlAlertContainer.addView(contentView);
            }

            FoxSdkViewExt.setOnClickListener(binding.fsIvAlertClose, v -> dialog.dismiss());
            binding.fsIvAlertClose.setVisibility(openClose ? View.VISIBLE : View.GONE);

            // 确定按钮
            binding.fsTvConfirm.setVisibility(positiveButtonText != null ? View.VISIBLE : View.GONE);
            binding.fsTvConfirm.setText(positiveButtonText);
            binding.fsTvConfirm.setTextColor(positiveButtonTextColor);
            FoxSdkViewExt.setOnClickListener(binding.fsTvConfirm, v -> {
                if (positiveButtonClickListener != null) {
                    positiveButtonClickListener.onClick();
                }
                dialog.dismiss();
            });

            // 取消按钮
            binding.fsTvCancel.setVisibility(negativeButtonText != null ? View.VISIBLE : View.GONE);
            binding.fsTvCancel.setText(negativeButtonText);
            binding.fsTvCancel.setTextColor(negativeButtonTextColor);

            if ("outline".equals(negativeButtonTheme)) {
                binding.fsTvCancel.setBackgroundColor(Color.TRANSPARENT);
                // 注意：这里需要根据实际的shape drawable builder来设置
                // binding.fsTvCancel.shapeDrawableBuilder...
            }

            FoxSdkViewExt.setOnClickListener(binding.fsTvCancel, v -> {
                if (negativeButtonClickListener != null) {
                    negativeButtonClickListener.onClick();
                }
                dialog.dismiss();
            });

            dialog.setCancelable(isCancelable);

            dialog.setOnDismissListener(dialogInterface -> {
                if (onDismissListener != null) {
                    onDismissListener.onDismiss();
                }
            });

            return dialog;
        }
    }

    public interface OnButtonClickListener {
        void onClick();
    }

    public interface OnDismissListener {
        void onDismiss();
    }
}
