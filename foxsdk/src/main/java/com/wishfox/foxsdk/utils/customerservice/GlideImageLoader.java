package com.wishfox.foxsdk.utils.customerservice;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.qiyukf.unicorn.api.ImageLoaderListener;
import com.qiyukf.unicorn.api.UnicornImageLoader;
import com.wishfox.foxsdk.R;

import org.jetbrains.annotations.NotNull;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:16
 */
public class GlideImageLoader implements UnicornImageLoader {

    private Context context;

    public GlideImageLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Bitmap loadImageSync(String s, int i, int i1) {
        return null;
    }

    @Override
    public void loadImage(String uri, int width, int height, ImageLoaderListener imageLoaderListener) {
        int targetWidth = width <= 0 ? Integer.MIN_VALUE : width;
        int targetHeight = height <= 0 ? Integer.MIN_VALUE : height;
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(new RequestOptions()
                        .placeholder(R.mipmap.fs_icon_win_coin)
                        .centerCrop()
                        .error(R.mipmap.fs_icon_win_coin))
                .into(new CustomTarget<Bitmap>(targetWidth, targetHeight) {
                    @Override
                    public void onResourceReady(@NotNull Bitmap resource, Transition<? super Bitmap> transition) {
                        imageLoaderListener.onLoadComplete(resource);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        imageLoaderListener.onLoadFailed(new Throwable("加载异常"));
                    }
                });
    }
}