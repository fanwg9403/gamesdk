package com.wishfox.foxsdk.ui.view.adapter;

import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.makeramen.roundedimageview.RoundedImageView;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSWinFoxCoin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月29日 9:04
 */
public class FSWinFoxCoinAdapter extends BaseQuickAdapter<FSWinFoxCoin, BaseViewHolder> {

    public FSWinFoxCoinAdapter(@Nullable List<FSWinFoxCoin> data) {
        super(R.layout.fs_item_win_fox_coin, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, FSWinFoxCoin fsWinFoxCoin) {
        Glide.with(getContext())
                .load(handleCoverImage(fsWinFoxCoin.getImage_url(), fsWinFoxCoin.getIndustry_image_url()))
                .error(R.mipmap.fs_default_avatar_round)
                .into((RoundedImageView) baseViewHolder.getView(R.id.fs_rounded_image));
        baseViewHolder.setText(R.id.fs_tv_title, fsWinFoxCoin.getTitle())
                .setText(R.id.fs_tv_content, fsWinFoxCoin.getContent())
                .setText(R.id.fs_tv_price, fsWinFoxCoin.getFull_amount())
                .setText(R.id.fs_tv_coin_num, "奖励" + (fsWinFoxCoin.getGive_coin() == null ? "0" : fsWinFoxCoin.getGive_coin()) + "狐币")
                .setGone(R.id.fs_cl_coin, fsWinFoxCoin.getGive_coin() == null || fsWinFoxCoin.getGive_coin() == 0);
    }

    /**
     * 愿望封面图加载规则处理
     * 封面图->分类图->默认图
     */
    private String handleCoverImage(String coverImage, String industryImageUrl) {
        return TextUtils.isEmpty(coverImage) ? TextUtils.isEmpty(industryImageUrl) ? "" : industryImageUrl : coverImage.split(",")[0];
    }
}
