package com.wishfox.foxsdk.ui.view.adapter;

import android.util.Pair;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.wishfox.foxsdk.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 21:38
 */
public class FSHomeActionAdapter extends BaseQuickAdapter<Pair<String, Integer>, BaseViewHolder> {

    public FSHomeActionAdapter(@Nullable List<Pair<String, Integer>> data) {
        super(R.layout.fs_layout_home_action, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, Pair<String, Integer> stringPair) {
        baseViewHolder.setText(R.id.fs_tv_action, stringPair.first);
        ((TextView) baseViewHolder.getView(R.id.fs_tv_action)).setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                stringPair.second == -1 ? 0 : R.drawable.fs_icon_right_arrow,
                0
        );
    }
}
