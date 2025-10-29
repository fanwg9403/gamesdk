package com.wishfox.foxsdk.ui.view.adapter;

import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSHomeBanner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 21:42
 */
public class FSHomeBannerAdapter extends BaseQuickAdapter<List<FSHomeBanner>, BaseViewHolder> {

    public FSHomeBannerAdapter(@Nullable List<List<FSHomeBanner>> data) {
        super(R.layout.fs_layout_home_banner, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, List<FSHomeBanner> fsHomeBanners) {
        baseViewHolder.setGone(R.id.fs_home_banner, fsHomeBanners == null || fsHomeBanners.isEmpty());

        FSBannerAdapter fsBannerAdapter = new FSBannerAdapter(fsHomeBanners);
        ((RecyclerView) baseViewHolder.getView(R.id.fs_home_banner)).setAdapter(fsBannerAdapter);
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
