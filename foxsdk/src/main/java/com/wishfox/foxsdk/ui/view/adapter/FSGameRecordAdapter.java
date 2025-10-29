package com.wishfox.foxsdk.ui.view.adapter;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.makeramen.roundedimageview.RoundedImageView;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSGameRecord;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 21:31
 */
public class FSGameRecordAdapter extends BaseQuickAdapter<FSGameRecord, BaseViewHolder> {

    public FSGameRecordAdapter(@Nullable List<FSGameRecord> data) {
        super(R.layout.fs_item_game_record, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, FSGameRecord fsGameRecord) {
        Glide.with(getContext())
                .load(fsGameRecord.getGame_index_img_file_name())
                .error(R.mipmap.fs_default_avatar_round)
                .into((RoundedImageView) baseViewHolder.getView(R.id.fs_rounded_image));

        baseViewHolder.setText(R.id.fs_tv_title, fsGameRecord.getApp_name())
                .setText(R.id.fs_tv_content, fsGameRecord.getServer_name());
    }
}
