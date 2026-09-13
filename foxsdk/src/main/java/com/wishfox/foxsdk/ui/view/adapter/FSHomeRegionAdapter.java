package com.wishfox.foxsdk.ui.view.adapter;

import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;
import com.wishfox.foxsdk.domain.intent.FSHomeIntent;
import com.wishfox.foxsdk.ui.view.activity.FSStarterPackActivity;
import com.wishfox.foxsdk.ui.view.activity.FSWinFoxCoinActivity;
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
 * @date: 2025年10月28日 21:46
 */
public class FSHomeRegionAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    private FSHomeViewModel fsHomeViewModel;

    public FSHomeRegionAdapter(@Nullable List<String> data, FSHomeViewModel fsHomeViewModel) {
        super(R.layout.fs_layout_home_region, data);
        this.fsHomeViewModel = fsHomeViewModel;
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, String s) {
        hideCustomerServiceEntry(baseViewHolder);

        FoxSdkViewExt.setOnClickListener(baseViewHolder.getView(R.id.fs_iv_coin), v -> {
            if (FSUserInfo.getInstance() == null) {
                new FSLoginDialog(getContext())
                        .setOnLoginClickListener((phone, codeOrPassword, loginType,loadingDialog) -> {
                            fsHomeViewModel.dispatch(
                                    new FSHomeIntent.Login(
                                            phone,
                                            codeOrPassword,
                                            loginType
                                    )
                            );
                        })
                        .show();
            } else {
                FSWinFoxCoinActivity.start(getContext());
            }
        });

        FoxSdkViewExt.setOnClickListener(baseViewHolder.getView(R.id.fs_iv_gift), v -> {
            if (FSUserInfo.getInstance() == null) {
                new FSLoginDialog(getContext())
                        .setOnLoginClickListener((phone, codeOrPassword, loginType,loadingDialog) -> {
                            fsHomeViewModel.dispatch(
                                    new FSHomeIntent.Login(
                                            phone,
                                            codeOrPassword,
                                            loginType
                                    )
                            );
                        })
                        .show();
            } else {
                FSStarterPackActivity.start(getContext());
            }
        });

    }

    private void hideCustomerServiceEntry(BaseViewHolder baseViewHolder) {
        View serviceView = baseViewHolder.getView(R.id.fs_iv_service);
        View coinView = baseViewHolder.getView(R.id.fs_iv_coin);
        serviceView.setVisibility(View.GONE);
        serviceView.setOnClickListener(null);
        if (coinView.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) coinView.getLayoutParams();
            params.endToStart = -1;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            coinView.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
