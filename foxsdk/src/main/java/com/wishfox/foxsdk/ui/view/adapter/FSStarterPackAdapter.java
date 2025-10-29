package com.wishfox.foxsdk.ui.view.adapter;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.hjq.shape.view.ShapeTextView;
import com.makeramen.roundedimageview.RoundedImageView;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSStarterPack;
import com.wishfox.foxsdk.utils.FoxSdkCommonExt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 22:02
 */
public class FSStarterPackAdapter extends BaseQuickAdapter<FSStarterPack, BaseViewHolder> {

    public FSStarterPackAdapter(@Nullable List<FSStarterPack> data) {
        super(R.layout.fs_item_starter_pack, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, FSStarterPack fsStarterPack) {
        Glide.with(getContext())
                .load(fsStarterPack.getImg())
                .error(R.mipmap.fs_default_avatar_round)
                .into((RoundedImageView) baseViewHolder.getView(R.id.fs_rounded_image));
        baseViewHolder.setText(R.id.fs_tv_title, fsStarterPack.getMail_title())
                .setText(R.id.fs_tv_content, fsStarterPack.getMail_content());
        int[] colorsborder = new int[]{
                0x8092FFD2, // 半透明浅蓝色
                0x0027B178, // 透明绿色
                0x0027B178, // 透明绿色
                0x8092FFD2  // 半透明浅蓝色
        };
        ShapeTextView shapeTextView = baseViewHolder.getView(R.id.fs_shp_tv_type);
        shapeTextView.getShapeDrawableBuilder()
                .setStrokeGradientColors(colorsborder)
                .setStrokeSize(FoxSdkCommonExt.dp2px(1))
                .intoBackground();

        ShapeTextView shapeTvOn = baseViewHolder.getView(R.id.fs_shp_tv_on);
        if (fsStarterPack.getStatus() != null) {
            switch (fsStarterPack.getStatus()) {
                case 1://未领取
                    baseViewHolder.setText(R.id.fs_shp_tv_on, getContext().getString(R.string.fs_str_claim_and_copy))
                            .setGone(R.id.fs_shp_tv_type, true);
                    shapeTvOn.getShapeDrawableBuilder()
                            .setSolidColor(0xff009E5C)
                            .intoBackground();
                    break;

                default:
                    baseViewHolder.setText(R.id.fs_shp_tv_on, getContext().getString(R.string.fs_str_copy_code))
                            .setGone(R.id.fs_shp_tv_type, false);
                    shapeTvOn.getShapeDrawableBuilder()
                            .setSolidColor(0xff02B8AC)
                            .intoBackground();
                    break;
            }
        } else {
            baseViewHolder.setText(R.id.fs_shp_tv_on, getContext().getString(R.string.fs_str_copy_code))
                    .setGone(R.id.fs_shp_tv_type, false);
            shapeTvOn.getShapeDrawableBuilder()
                    .setSolidColor(0xff02B8AC)
                    .intoBackground();
        }
    }
}
