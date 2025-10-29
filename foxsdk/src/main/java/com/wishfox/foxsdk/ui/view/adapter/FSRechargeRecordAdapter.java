package com.wishfox.foxsdk.ui.view.adapter;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.makeramen.roundedimageview.RoundedImageView;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSRechargeRecord;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 21:59
 */
public class FSRechargeRecordAdapter extends BaseQuickAdapter<FSRechargeRecord, BaseViewHolder> {

    public FSRechargeRecordAdapter(@Nullable List<FSRechargeRecord> data) {
        super(R.layout.fs_item_recharge_record, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, FSRechargeRecord fsRechargeRecord) {
        Glide.with(getContext())
                .load("")
                .error(R.mipmap.fs_default_avatar_round)
                .into((RoundedImageView) baseViewHolder.getView(R.id.fs_rounded_image));
        baseViewHolder.setText(R.id.fs_tv_title, fsRechargeRecord.getMallName())
                .setText(R.id.fs_tv_content, fsRechargeRecord.getMallSku())
                .setText(R.id.fs_tv_fox_coin, "¥" + fsRechargeRecord.getPrice())
                .setText(R.id.fs_shp_tv_time, fsRechargeRecord.getCreateTime());
    }
}
