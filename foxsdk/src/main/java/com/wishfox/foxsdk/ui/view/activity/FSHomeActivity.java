package com.wishfox.foxsdk.ui.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;
import com.wishfox.foxsdk.databinding.FsActivityHomeBinding;
import com.wishfox.foxsdk.di.FoxSdkViewModelFactory;
import com.wishfox.foxsdk.domain.intent.FSHomeIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.ui.view.adapter.FSBannerAdapter;
import com.wishfox.foxsdk.ui.view.adapter.FSHomeActionAdapter;
import com.wishfox.foxsdk.ui.view.dialog.FSAlertDialog;
import com.wishfox.foxsdk.ui.view.dialog.FSLoginDialog;
import com.wishfox.foxsdk.ui.viewmodel.FSHomeViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSHomeViewState;
import com.wishfox.foxsdk.utils.FoxSdkAnimation;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;
import com.wishfox.foxsdk.utils.customerservice.QiyukfHelper;
import com.youth.banner.Banner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.annotations.Nullable;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:56
 */
public class FSHomeActivity extends FoxSdkBaseMviActivity<FSHomeViewState, FSHomeIntent, FsActivityHomeBinding> {

    private FSHomeViewModel viewModel;
    private FSHomeActionAdapter actionAdapter;
    private View userHead;
    private View bannerHead;
    private View regionHead;
    private FSBannerAdapter fsBannerAdapter;

    private final List<Pair<String, Integer>> actionItems = Arrays.asList(
            new Pair<>("游戏记录", 0),
            new Pair<>("充值记录", 1),
            new Pair<>("我的消息", 2),
            new Pair<>("", -1)
    );

    @Override
    protected FSHomeViewModel getViewModel() {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this, new FoxSdkViewModelFactory()).get(FSHomeViewModel.class);
        }
        return viewModel;
    }

    @Override
    protected FsActivityHomeBinding createBinding() {
        return FsActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        initAdapters();
    }

    private void initAdapters() {
        ((TextView) userHead.findViewById(R.id.fs_tv_username)).setText(getString(R.string.fs_login_now));
        ((TextView) userHead.findViewById(R.id.fs_stv_coin)).setVisibility(View.GONE);

        FoxSdkViewExt.setOnClickListener(((ConstraintLayout) userHead.findViewById(R.id.fs_cl_top_bar)), v -> {
            if (((TextView) userHead.findViewById(R.id.fs_stv_coin)).getVisibility() == View.GONE) {
                new FSLoginDialog(this)
                        .setOnLoginClickListener((phone, codeOrPassword, loginType) ->
                                viewModel.dispatch(
                                        new FSHomeIntent.Login(
                                                phone,
                                                codeOrPassword,
                                                loginType
                                        )
                                ))
                        .show();
            }
        });

        FoxSdkViewExt.setOnClickListener(((ImageView) regionHead.findViewById(R.id.fs_iv_coin)), v -> {
            if (FSUserInfo.getInstance() == null) {
                new FSLoginDialog(this)
                        .setOnLoginClickListener((phone, codeOrPassword, loginType) -> {
                            viewModel.dispatch(
                                    new FSHomeIntent.Login(
                                            phone,
                                            codeOrPassword,
                                            loginType
                                    )
                            );
                        })
                        .show();
            } else {
                FSWinFoxCoinActivity.start(this);
            }
        });

        FoxSdkViewExt.setOnClickListener(((ImageView) regionHead.findViewById(R.id.fs_iv_gift)), v -> {
            if (FSUserInfo.getInstance() == null) {
                new FSLoginDialog(this)
                        .setOnLoginClickListener((phone, codeOrPassword, loginType) -> {
                            viewModel.dispatch(
                                    new FSHomeIntent.Login(
                                            phone,
                                            codeOrPassword,
                                            loginType
                                    )
                            );
                        })
                        .show();
            } else {
                FSStarterPackActivity.start(this);
            }
        });

        FoxSdkViewExt.setOnClickListener(((ImageView) regionHead.findViewById(R.id.fs_iv_service)), v -> {
            if (FSUserInfo.getInstance() == null) {
                new FSLoginDialog(this)
                        .setOnLoginClickListener((phone, codeOrPassword, loginType) -> {
                            viewModel.dispatch(
                                    new FSHomeIntent.Login(
                                            phone,
                                            codeOrPassword,
                                            loginType
                                    )
                            );
                        })
                        .show();
            } else {
                QiyukfHelper.getInstance().openCustomerService(
                        this,
                        "在线客服",
                        "Mine",
                        "",
                        ""
                );
            }
        });

        actionAdapter = new FSHomeActionAdapter(FSUserInfo.getInstance() == null ? new ArrayList<>() : actionItems);
        actionAdapter.setOnItemClickListener((adapter, view, position) -> {
            switch (position) {
                case 0:
                    FSGameRecordActivity.start(this);
                    break;
                case 1:
                    FSRechargeRecordActivity.start(this);
                    break;
                case 2:
                    startActivity(new Intent(this, FSMessageActivity.class));
                    break;
//                case 3:
//                    showLogoutDialog();
//                    break;
            }
        });

        actionAdapter.addHeaderView(userHead);
        if (FSUserInfo.getInstance() != null)
            actionAdapter.addHeaderView(regionHead);
        binding.fsRv.setAdapter(actionAdapter);

        FoxSdkViewExt.setOnClickListener(binding.fsVOutside, v -> finish());
        binding.fsHomeRoot.setEnabled(true);
        binding.fsHomeRoot.setRefreshHeader(new ClassicsHeader(this));
        dispatch(new FSHomeIntent.Init());

        binding.fsHomeRoot.setOnRefreshListener(refreshLayout ->
                dispatch(new FSHomeIntent.Init()));
    }

    private void showLogoutDialog() {
        new FSAlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定要退出登录吗？")
                .setPositive("确定", () ->
                        getViewModel().dispatch(new FSHomeIntent.Logout()))
                .setNegative("取消", null)
                .build()
                .show();
    }

    @Override
    protected void initView() {
        ImmersionBar.with(this)
                .statusBarDarkFont(false)
                .transparentStatusBar()
                .statusBarView(binding.fsVTopSafeArea)
                .hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR)
                .init();

        binding.fsHomeRoot.post(() -> {
            FoxSdkAnimation.translation("HORIZONTAL", binding.fsHomeRoot, -600, 0, 300);
            FoxSdkAnimation.translation("HORIZONTAL", binding.fsVTopSafeArea, -600, 0, 300);
        });
        binding.fsVStartSafeArea.postDelayed(() -> binding.fsVStartSafeArea.setAlpha(1f), 100L);

        userHead = LayoutInflater.from(this).inflate(R.layout.fs_layout_home_user_info, null);
        bannerHead = LayoutInflater.from(this).inflate(R.layout.fs_layout_home_banner, null);
        regionHead = LayoutInflater.from(this).inflate(R.layout.fs_layout_home_region, null);

        fsBannerAdapter = new FSBannerAdapter(new ArrayList<>());
        ((Banner) bannerHead.findViewById(R.id.fs_home_banner)).setAdapter(fsBannerAdapter);
    }

    @Override
    protected void renderState(FSHomeViewState state) {
        binding.fsHomeRoot.finishRefresh();

        if (state == null)
            return;

        if (state.getUserInfo() != null && state.isLoginSuccess()) {
            if (state.getCoinInfo() != null) {
                state.getUserInfo().setFoxCoin(
                        (int) Float.parseFloat(state.getCoinInfo().getBalanceCoin())
                );
            }
            // 已登录
            ((TextView) userHead.findViewById(R.id.fs_tv_username)).setText(state.getUserInfo().getUserName());
            ((TextView) userHead.findViewById(R.id.fs_stv_coin)).setText(getString(R.string.fs_fox_coin) +
                    (state.getUserInfo() == null || state.getUserInfo().getFoxCoin() == null ? "0" : (state.getUserInfo().getFoxCoin() + "")));
            ((TextView) userHead.findViewById(R.id.fs_stv_coin)).setVisibility(View.VISIBLE);

            actionAdapter.setNewInstance(actionItems);
        } else {
            // 未登录
            ((TextView) userHead.findViewById(R.id.fs_tv_username)).setText(getString(R.string.fs_login_now));
            ((TextView) userHead.findViewById(R.id.fs_stv_coin)).setVisibility(View.GONE);

            actionAdapter.setNewInstance(new ArrayList<>());
        }

        boolean hasBanner = false;
        if (actionAdapter.getHeaderLayout().getChildCount() == 2) {
            hasBanner = actionAdapter.getHeaderLayout().getChildAt(1) instanceof ConstraintLayout &&
                    ((ConstraintLayout) actionAdapter.getHeaderLayout().getChildAt(1)).getChildAt(0) instanceof Banner;
        } else {
            hasBanner = false;
        }
        if (state.getBannerList() != null && !state.getBannerList().isEmpty()) {
            fsBannerAdapter.setDatas(state.getBannerList());
            if (!hasBanner)
                if(bannerHead.getParent() != null){
                    ((ViewGroup) bannerHead.getParent()).removeView(bannerHead);
                }
                actionAdapter.addHeaderView(bannerHead, 1);
        } else if (hasBanner)
            actionAdapter.removeHeaderView(bannerHead);

        boolean hasRegion = false;
        if (actionAdapter.getHeaderLayout().getChildCount() == 2) {
            hasRegion = actionAdapter.getHeaderLayout().getChildAt(1) instanceof ConstraintLayout &&
                    ((ConstraintLayout) actionAdapter.getHeaderLayout().getChildAt(1)).getChildAt(0) instanceof ImageView;
        } else hasRegion = actionAdapter.getHeaderLayout().getChildCount() == 3;
        if (state.getUserInfo() != null && state.isLoginSuccess()) {
            if (!hasRegion)
                actionAdapter.addHeaderView(regionHead, hasBanner ? 2 : 1);
        } else {
            if (hasRegion)
                actionAdapter.removeHeaderView(regionHead);
        }
    }

    @Override
    public void finish() {
        binding.getRoot().postDelayed(() -> binding.fsVStartSafeArea.setAlpha(0f), 100L);
        FoxSdkAnimation.translation("HORIZONTAL", binding.fsVTopSafeArea, 0, -600, 300);
        FoxSdkAnimation.translation("HORIZONTAL", binding.fsHomeRoot, 0, -600, 300)
                .subscribe(() -> binding.getRoot().postDelayed(() -> super.finish(), 50L));
    }
}
