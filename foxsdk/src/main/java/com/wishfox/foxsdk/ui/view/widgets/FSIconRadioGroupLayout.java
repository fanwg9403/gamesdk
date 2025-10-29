package com.wishfox.foxsdk.ui.view.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:03
 */
public class FSIconRadioGroupLayout extends LinearLayout {

    public FSIconRadioGroupLayout(Context context) {
        this(context, null);
    }

    public FSIconRadioGroupLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FSIconRadioGroupLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setOrientation(VERTICAL);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (!(child instanceof FSIconRadioTextView)) {
                removeView(child);
                Log.w(
                        "FSIconRadioGroupLayout",
                        "child must be FSIconRadioTextView, not " + child.getClass().getName()
                );
            } else {
                FSIconRadioTextView radioTextView = (FSIconRadioTextView) child;
                radioTextView.setOnCheckedChangeListener((v, isChecked) -> {
                    for (int j = 0; j < getChildCount(); j++) {
                        View childView = getChildAt(j);
                        if (childView != v && childView instanceof FSIconRadioTextView) {
                            ((FSIconRadioTextView) childView).setChecked(false);
                        }
                    }
                });
            }
        }
    }

    public FSIconRadioTextView getCheckedItem() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof FSIconRadioTextView && ((FSIconRadioTextView) child).isChecked()) {
                return (FSIconRadioTextView) child;
            }
        }
        return null;
    }

    public int getCheckedItemPosition() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof FSIconRadioTextView && ((FSIconRadioTextView) child).isChecked()) {
                return i;
            }
        }
        return -1;
    }

    public void setCheckedItem(int position) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof FSIconRadioTextView) {
                ((FSIconRadioTextView) child).setChecked(i == position);
            }
        }
    }
}
