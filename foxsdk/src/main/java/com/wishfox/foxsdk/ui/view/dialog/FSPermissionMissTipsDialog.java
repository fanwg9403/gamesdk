package com.wishfox.foxsdk.ui.view.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.lxj.xpopup.core.CenterPopupView;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:57
 */
@SuppressLint("ViewConstructor")
public class FSPermissionMissTipsDialog extends CenterPopupView {

    private String message;
    private View.OnClickListener listener;

    public FSPermissionMissTipsDialog(Context context, String message) {
        super(context);
        this.message = message;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.fs_dialog_permission_miss_tips;
    }

    // 设置监听
    public void setMListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        TextView ok = findViewById(R.id.fs_tv_ok);
        TextView cancel = findViewById(R.id.fs_tv_cancel);
        TextView text = findViewById(R.id.fs_text);

        text.setText(message);

        FoxSdkViewExt.setOnClickListener(ok, v -> {
            dismiss();
            if (listener != null) {
                listener.onClick(v);
            }
        });

        FoxSdkViewExt.setOnClickListener(cancel, v -> dismiss());
    }
}
