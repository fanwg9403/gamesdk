package com.wishfox.foxsdk.ui.view.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.StringRes;

import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.databinding.FsIconRadioTextViewBinding;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import io.reactivex.rxjava3.functions.Consumer;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:03
 */
public class FSIconRadioTextView extends LinearLayout {

    private boolean _isChecked = false;
    private String text = "";
    private int textColor = -1;
    private int icon = -1;

    private int checkedRadioColor = -1;
    private int radioColor = -1;

    private boolean _disabled = false;

    private FsIconRadioTextViewBinding binding;

    private OnCheckedChangeListener listener;
    private OnCheckedChangeListenerWithView viewListener;

    public FSIconRadioTextView(Context context) {
        this(context, null);
    }

    public FSIconRadioTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FSIconRadioTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // 解析自定义属性
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FSIconRadioTextView);
            try {
                text = a.getString(R.styleable.FSIconRadioTextView_android_text);
                if (text == null) {
                    text = "";
                }
                textColor = a.getColor(R.styleable.FSIconRadioTextView_android_textColor, -1);
                icon = a.getResourceId(R.styleable.FSIconRadioTextView_android_icon, -1);
                radioColor = a.getColor(R.styleable.FSIconRadioTextView_radioColor, -1);
                checkedRadioColor = a.getColor(R.styleable.FSIconRadioTextView_checkedRadioColor, -1);
                _isChecked = a.getBoolean(R.styleable.FSIconRadioTextView_android_checked, false);
                _disabled = !a.getBoolean(R.styleable.FSIconRadioTextView_android_enabled, true);
            } finally {
                a.recycle();
            }
        }

        // 初始化绑定
        binding = FsIconRadioTextViewBinding.inflate(LayoutInflater.from(getContext()), this, true);

        setOrientation(HORIZONTAL);

        if (icon != -1) {
            binding.fsCheckRadioIcon.setImageResource(icon);
        } else {
            binding.fsCheckRadioIcon.setVisibility(GONE);
        }

        if (text != null && !text.isEmpty()) {
            binding.fsCheckRadioText.setText(text);
        } else {
            binding.fsCheckRadioText.setVisibility(GONE);
        }

        if (textColor != -1) {
            binding.fsCheckRadioText.setTextColor(textColor);
        }

        if (radioColor != -1 && !_isChecked) {
            binding.fsCheckRadio.setBackgroundTintList(ColorStateList.valueOf(radioColor));
        }

        if (checkedRadioColor != -1 && _isChecked) {
            binding.fsCheckRadio.setBackgroundTintList(ColorStateList.valueOf(checkedRadioColor));
        }

        binding.fsCheckRadio.setChecked(_isChecked);

        FoxSdkViewExt.setOnClickListener(binding.getRoot(), v -> {
            if (_disabled) return;
            setChecked(!_isChecked);
            if (listener != null) {
                listener.onCheckedChanged(_isChecked);
            }
            if (viewListener != null) {
                viewListener.onCheckedChanged(this, _isChecked);
            }
        });

        if (_disabled) {
            binding.fsCheckRadio.setBackgroundResource(R.drawable.fs_radio_disabled);
            binding.fsCheckRadioText.setTextColor(Color.parseColor("#66FFFFFF"));
            binding.fsCheckRadio.setChecked(false);
        }
    }

    public void setText(String text) {
        binding.fsCheckRadioText.setText(text);
    }

    public void setTextColor(int color) {
        binding.fsCheckRadioText.setTextColor(color);
    }

    public void setIcon(int icon) {
        binding.fsCheckRadioIcon.setImageResource(icon);
    }

    public void setIcon(Drawable icon) {
        binding.fsCheckRadioIcon.setImageDrawable(icon);
    }

    public void setRadioColor(int color) {
        radioColor = color;
        if (!_isChecked) {
            binding.fsCheckRadio.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    public void setCheckedRadioColor(int color) {
        checkedRadioColor = color;
        if (_isChecked) {
            binding.fsCheckRadio.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    public void setText(@StringRes int text) {
        binding.fsCheckRadioText.setText(text);
    }

    public boolean isChecked() {
        return _isChecked;
    }

    public void setChecked(boolean value) {
        _isChecked = value;
        binding.fsCheckRadio.setChecked(value);
        if (value) {
            binding.fsCheckRadio.setBackgroundTintList(
                    checkedRadioColor != -1 ? ColorStateList.valueOf(checkedRadioColor) : null);
        } else {
            binding.fsCheckRadio.setBackgroundTintList(
                    radioColor != -1 ? ColorStateList.valueOf(radioColor) : null);
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListenerWithView listener) {
        this.viewListener = listener;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        _disabled = !enabled;
        if (_disabled) {
            setChecked(false);
            binding.fsCheckRadio.setBackgroundResource(R.drawable.fs_radio_disabled);
            binding.fsCheckRadioText.setTextColor(Color.parseColor("#66FFFFFF"));
        } else {
            binding.fsCheckRadio.setBackgroundResource(R.drawable.fs_radio_selector);
            binding.fsCheckRadioText.setTextColor(Color.parseColor("#FFFFFFFF"));
        }
    }

    // 监听器接口
    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean isChecked);
    }

    public interface OnCheckedChangeListenerWithView {
        void onCheckedChanged(View view, boolean isChecked);
    }
}