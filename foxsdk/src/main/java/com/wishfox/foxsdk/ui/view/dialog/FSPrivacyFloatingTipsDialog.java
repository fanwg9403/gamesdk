package com.wishfox.foxsdk.ui.view.dialog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lxj.xpopup.core.CenterPopupView;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.databinding.FsItemPrivacyfloatingtipsBinding;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:55
 */
@SuppressLint("ViewConstructor")
public class FSPrivacyFloatingTipsDialog extends CenterPopupView {

    private List<String> permissionList;
    private View.OnClickListener listener;
    private PermissionTipsAdapter mPermissionTipsAdapter;

    public FSPrivacyFloatingTipsDialog(Context context, List<String> permissionList) {
        super(context);
        this.permissionList = permissionList;
        this.mPermissionTipsAdapter = new PermissionTipsAdapter();
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.fs_dialog_privacyfloatingtips;
    }

    // 设置监听
    public void setMListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        RecyclerView fsRv = findViewById(R.id.fs_rv);
        TextView fsOk = findViewById(R.id.fs_tv_ok);
        TextView fsCancel = findViewById(R.id.fs_tv_cancel);

        List<PermissionTips> itemData = new ArrayList<>();

        for (String permission : permissionList) {
            PermissionTips tips = new PermissionTips();

            switch (permission) {
                case "Camera_And_Stored_Media_Files":
                    tips.setTitle(getContext().getResources().getString(R.string.fs_camera_and_gallery_permission_title));
                    tips.setText(getContext().getResources().getString(R.string.fs_camera_and_gallery_permission_text));
                    break;

                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                case Manifest.permission.READ_EXTERNAL_STORAGE:
                    tips.setTitle(getContext().getResources().getString(R.string.fs_gallery_permission_title));
                    tips.setText(getContext().getResources().getString(R.string.fs_gallery_permission_text));
                    break;

                case Manifest.permission.RECORD_AUDIO:
                    tips.setTitle(getContext().getResources().getString(R.string.fs_microphone_permission_title));
                    tips.setText(getContext().getResources().getString(R.string.fs_microphone_permission_text));
                    break;

                case Manifest.permission.CAMERA:
                    tips.setTitle(getContext().getResources().getString(R.string.fs_camera_permission_title));
                    tips.setText(getContext().getResources().getString(R.string.fs_camera_permission_text));
                    break;

                default:
                    // 不处理
                    break;
            }
            itemData.add(tips);
        }

        fsRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mPermissionTipsAdapter.submitList(itemData);
        fsRv.setAdapter(mPermissionTipsAdapter);

        FoxSdkViewExt.setOnClickListener(fsOk, v -> {
            dismiss();
            if (listener != null) {
                listener.onClick(v);
            }
        });

        FoxSdkViewExt.setOnClickListener(fsCancel, v -> dismiss());
    }

    /**
     * 权限提示适配器
     */
    public static class PermissionTipsAdapter extends RecyclerView.Adapter<PermissionTipsAdapter.VH> {

        private List<PermissionTips> data = new ArrayList<>();

        public void submitList(List<PermissionTips> newData) {
            this.data = newData != null ? new ArrayList<>(newData) : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PermissionTips item = data.get(position);
            if (item != null) {
                holder.binding.fsTitle.setText(item.getTitle());
                holder.binding.fsText.setText(item.getText());
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        /**
         * ViewHolder
         */
        public static class VH extends RecyclerView.ViewHolder {
            public FsItemPrivacyfloatingtipsBinding binding;

            public VH(ViewGroup parent) {
                super(FsItemPrivacyfloatingtipsBinding.inflate(
                        android.view.LayoutInflater.from(parent.getContext()), parent, false
                ).getRoot());
                binding = FsItemPrivacyfloatingtipsBinding.bind(itemView);
            }
        }
    }

    /**
     * 权限提示数据类
     */
    public static class PermissionTips {
        private String title = "";
        private String text = "";

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
