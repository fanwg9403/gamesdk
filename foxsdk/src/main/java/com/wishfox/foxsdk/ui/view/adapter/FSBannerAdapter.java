package com.wishfox.foxsdk.ui.view.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wishfox.foxsdk.data.model.entity.FSHomeBanner;
import com.wishfox.foxsdk.databinding.FsLayoutHomeBannerItemBinding;
import com.wishfox.foxsdk.ui.view.activity.FSWebActivity;
import com.wishfox.foxsdk.utils.FSGlideRoundTransform;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;
import com.youth.banner.adapter.BannerAdapter;

import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 21:22
 */
public class FSBannerAdapter extends BannerAdapter<FSHomeBanner, FSBannerAdapter.VH> {

    public FSBannerAdapter(List<FSHomeBanner> datas) {
        super(datas);
    }

    @Override
    public VH onCreateHolder(ViewGroup parent, int viewType) {
        return new VH(parent);
    }

    @Override
    public void onBindView(VH holder, FSHomeBanner data, int position, int size) {
        FoxSdkViewExt.setOnClickListener(holder.binding.fsBannerImage, v -> FSWebActivity.startWithUrl(v.getContext(), data.getLink() != null ? data.getLink() : ""));

        Glide.with(holder.binding.fsBannerImage.getContext())
                .load(data.getImage())
                .transform(new FSGlideRoundTransform(holder.binding.fsBannerImage.getContext(), 9))
                .into(holder.binding.fsBannerImage);
    }

    public static class VH extends RecyclerView.ViewHolder {
        public final FsLayoutHomeBannerItemBinding binding;

        public VH(@NonNull ViewGroup parent) {
            this(FsLayoutHomeBannerItemBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            ));
        }

        public VH(@NonNull FsLayoutHomeBannerItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
