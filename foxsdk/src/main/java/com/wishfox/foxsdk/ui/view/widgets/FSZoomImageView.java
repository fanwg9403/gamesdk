package com.wishfox.foxsdk.ui.view.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/** 有界解码后的图片预览：1～4 倍缩放、边界平移、单击/原始比例拖动关闭。 */
final class FSZoomImageView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector scaleDetector;
    private Bitmap bitmap;
    private float zoom = 1f, x, y, lastX, lastY, downY;
    private boolean multiTouch;
    private final Runnable close;

    FSZoomImageView(Context context, Runnable close) {
        super(context);
        this.close = close;
        setContentDescription("图片预览，双指缩放，上下拖拽关闭");
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float previous = zoom;
                zoom = Math.max(1f, Math.min(4f, zoom * detector.getScaleFactor()));
                float ratio = zoom / previous;
                x = (x - detector.getFocusX() + getWidth() / 2f) * ratio
                        + detector.getFocusX() - getWidth() / 2f;
                y = (y - detector.getFocusY() + getHeight() / 2f) * ratio
                        + detector.getFocusY() - getHeight() / 2f;
                clamp();
                invalidate();
                return true;
            }
        });
    }

    void setBitmap(Bitmap value) { bitmap = value; zoom = 1; x = y = 0; invalidate(); }

    private float baseScale() {
        return bitmap == null ? 1f : Math.min((float) getWidth() / bitmap.getWidth(),
                (float) getHeight() / bitmap.getHeight());
    }

    private void clamp() {
        if (bitmap == null) return;
        float scale = baseScale() * zoom;
        float maxX = Math.max(0, (bitmap.getWidth() * scale - getWidth()) / 2f);
        float maxY = Math.max(0, (bitmap.getHeight() * scale - getHeight()) / 2f);
        x = Math.max(-maxX, Math.min(maxX, x));
        y = Math.max(-maxY, Math.min(maxY, y));
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        clamp();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || bitmap.isRecycled()) return;
        float scale = baseScale() * zoom;
        canvas.save();
        canvas.translate(getWidth() / 2f + x, getHeight() / 2f + y);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, -bitmap.getWidth() / 2f, -bitmap.getHeight() / 2f, paint);
        canvas.restore();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            multiTouch = false;
            lastX = event.getX(); lastY = downY = event.getY();
        }
        if (event.getPointerCount() > 1) multiTouch = true;
        scaleDetector.onTouchEvent(event);
        if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1 && !multiTouch) {
            if (zoom > 1.01f) { x += event.getX() - lastX; y += event.getY() - lastY; clamp(); }
            else { y = event.getY() - downY; }
            lastX = event.getX(); lastY = event.getY(); invalidate();
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (action == MotionEvent.ACTION_UP && !multiTouch && zoom <= 1.01f
                    && Math.abs(y) >= dismissDistance()) close.run();
            if (zoom <= 1.01f) { x = y = 0; invalidate(); }
        }
        return true;
    }

    private float dismissDistance() { return Math.max(96 * getResources().getDisplayMetrics().density, getHeight() * .2f); }

}
