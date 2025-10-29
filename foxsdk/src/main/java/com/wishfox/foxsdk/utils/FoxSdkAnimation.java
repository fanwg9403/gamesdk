package com.wishfox.foxsdk.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;

import io.reactivex.rxjava3.core.Completable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:07
 */
public class FoxSdkAnimation {

    public static Completable translation(String orientation, View view, int start, int end, long duration) {
        return Completable.create(emitter -> {
            String propertyName = "HORIZONTAL".equals(orientation) ? "translationX" : "translationY";
            ObjectAnimator animator = ObjectAnimator.ofFloat(
                    view,
                    propertyName,
                    FoxSdkCommonExt.dp2px(view.getContext(), start),
                    FoxSdkCommonExt.dp2px(view.getContext(), end)
            );
            animator.setDuration(duration);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    emitter.onComplete();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    emitter.onComplete();
                }
            });
            animator.start();
        });
    }
}
