package com.wishfox.foxsdk.utils.custom;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:28
 */
public class CustomTextWatcher implements TextWatcher {
    private final CustomLiveData<String> liveData;
    private final OnTextChangedListener textChangedListener;

    public CustomTextWatcher() {
        this(null, null);
    }

    public CustomTextWatcher(CustomLiveData<String> liveData) {
        this(liveData, null);
    }

    public CustomTextWatcher(OnTextChangedListener listener) {
        this(null, listener);
    }

    public CustomTextWatcher(CustomLiveData<String> liveData, OnTextChangedListener listener) {
        this.liveData = liveData;
        this.textChangedListener = listener;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        String text = editable != null ? editable.toString() : "";

        if (liveData != null) {
            liveData.postValue(text);
        }

        if (textChangedListener != null) {
            textChangedListener.afterTextChanged(text);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        if (textChangedListener != null) {
            textChangedListener.beforeTextChanged(
                    charSequence != null ? charSequence.toString() : "", start, count, after);
        }
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        if (textChangedListener != null) {
            textChangedListener.onTextChanged(
                    charSequence != null ? charSequence.toString() : "", start, before, count);
        }
    }

    /**
     * 文本变化监听接口
     */
    public interface OnTextChangedListener {
        void afterTextChanged(String text);
        void beforeTextChanged(String text, int start, int count, int after);
        void onTextChanged(String text, int start, int before, int count);
    }

    /**
     * 简单的文本变化监听适配器
     */
    public static abstract class SimpleTextChangedListener implements OnTextChangedListener {
        @Override
        public void beforeTextChanged(String text, int start, int count, int after) {
            // 默认空实现
        }

        @Override
        public void onTextChanged(String text, int start, int before, int count) {
            // 默认空实现
        }
    }
}
