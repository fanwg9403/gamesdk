package com.wishfox.foxsdk.ui.view.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;
import com.wishfox.foxsdk.domain.intent.FSHomeIntent;
import com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog;
import com.wishfox.foxsdk.ui.viewmodel.FSHomeViewModel;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 21:51
 */
public class FSHomeUserInfoAdapter extends BaseQuickAdapter<FSUserInfo, BaseViewHolder> {

    private FSHomeViewModel fsHomeViewModel;

    public FSHomeUserInfoAdapter(@Nullable List<FSUserInfo> data, FSHomeViewModel fsHomeViewModel) {
        super(R.layout.fs_layout_home_user_info, data);
        this.fsHomeViewModel = fsHomeViewModel;
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, FSUserInfo fsUserInfo) {
        if (fsUserInfo != null) {
            baseViewHolder.setText(R.id.fs_tv_username, fsUserInfo.getUserName())
                    .setText(R.id.fs_stv_coin,
                            getContext().getString(R.string.fs_fox_coin) +
                                    (FSCoinInfo.getInstance() == null ? "0" : (FSCoinInfo.getInstance().getFoxCoin() + "")))
                    .setGone(R.id.fs_stv_coin, false);
        } else {
            baseViewHolder.setText(R.id.fs_tv_username, getContext().getString(R.string.fs_login_now))
                    .setGone(R.id.fs_stv_coin, true);
        }

        FoxSdkViewExt.setOnClickListener(baseViewHolder.getView(R.id.fs_cl_top_bar), v -> {
            if (fsUserInfo == null) {
                new FSLoginDialog(getContext())
                        .setOnLoginClickListener((phone, codeOrPassword, loginType) ->
                                fsHomeViewModel.dispatch(
                                        new FSHomeIntent.Login(
                                                phone,
                                                codeOrPassword,
                                                loginType
                                        )
                                ))
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
