package com.wishfox.foxsdk.ui.view.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSMessage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 21:57
 */
public class FSMessageListAdapter extends BaseQuickAdapter<FSMessage, BaseViewHolder> {

    public FSMessageListAdapter(@Nullable List<FSMessage> data) {
        super(R.layout.fs_item_message_list, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, FSMessage fsMessage) {
        baseViewHolder.setText(R.id.fs_tv_title, fsMessage.getTitle())
                .setText(R.id.fs_tv_content, fsMessage.getContent())
                .setText(R.id.fs_tv_time, fsMessage.getTime())
                .setGone(R.id.fs_iv_unread, fsMessage.isRead());
    }
}
