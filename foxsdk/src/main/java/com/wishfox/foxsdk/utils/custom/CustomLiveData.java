package com.wishfox.foxsdk.utils.custom;

import android.os.Looper;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:28
 */
public class CustomLiveData<T> {
    private T currentValue;
    private OnValueChangedListener<T> observer;

    public CustomLiveData() {
        this(null);
    }

    public CustomLiveData(T value) {
        this.currentValue = value;
    }

    public T getValue() {
        return currentValue;
    }

    /**
     * 设置观察者
     */
    public void observe(OnValueChangedListener<T> listener) {
        this.observer = listener;
    }

    /**
     * 如果值相等不触发监听
     */
    public void setValue(T value) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            if (!isEqual(currentValue, value)) {
                this.currentValue = value;
                if (observer != null) {
                    observer.onChanged(value);
                }
            }
        }
    }

    /**
     * 不论值是否相等都触发监听
     */
    public void postValue(T value) {
        this.currentValue = value;
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            if (observer != null) {
                observer.onChanged(value);
            }
        }
    }

    /**
     * 安全的值比较
     */
    private boolean isEqual(T a, T b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * 移除观察者
     */
    public void removeObserver() {
        this.observer = null;
    }

    /**
     * 值变化监听接口
     */
    public interface OnValueChangedListener<T> {
        void onChanged(T value);
    }
}
