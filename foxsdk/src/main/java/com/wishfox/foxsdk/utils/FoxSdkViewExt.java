package com.wishfox.foxsdk.utils;

import android.view.View;

import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:19
 */
public class FoxSdkViewExt {

    // 原有的点击方法
    public static Disposable setOnClickListener(View view, View.OnClickListener listener) {
        AtomicLong lastClickTime = new AtomicLong(0);

        return Observable.create(emitter -> {
            View.OnClickListener wrapperListener = v -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime.get() >= 500) {
                    lastClickTime.set(currentTime);
                    if (!emitter.isDisposed()) {
                        emitter.onNext(v);
                    }
                }
            };
            view.setOnClickListener(wrapperListener);
            emitter.setCancellable(() -> view.setOnClickListener(null));
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> {
                    if (listener != null) {
                        listener.onClick((View) v);
                    }
                });
    }
}
