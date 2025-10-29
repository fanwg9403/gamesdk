package com.wishfox.foxsdk.ui.view.activity;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSGameSchemeData;
import com.wishfox.foxsdk.data.model.entity.FSWinFoxCoin;
import com.wishfox.foxsdk.databinding.FsActivityWinFoxCoinBinding;
import com.wishfox.foxsdk.di.FoxSdkViewModelFactory;
import com.wishfox.foxsdk.domain.intent.FSWinFoxCoinIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.ui.view.adapter.FSWinFoxCoinAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSWinFoxCoinViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSWinFoxCoinViewState;
import com.wishfox.foxsdk.utils.FoxSdkAnimation;
import com.wishfox.foxsdk.utils.FoxSdkAppJumpUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 20:38
 */
public class FSWinFoxCoinActivity extends FoxSdkBaseMviActivity<FSWinFoxCoinViewState, FSWinFoxCoinIntent, FsActivityWinFoxCoinBinding> {

    private FSWinFoxCoinViewModel viewModel;
    private FSWinFoxCoinAdapter winFoxCoinAdapter;

    public static void start(Context context) {
        context.startActivity(new Intent(context, FSWinFoxCoinActivity.class));
    }

    @Override
    protected FSWinFoxCoinViewModel getViewModel() {
        if (viewModel == null) {
            FoxSdkViewModelFactory factory = new FoxSdkViewModelFactory();
            viewModel = new ViewModelProvider(this, factory).get(FSWinFoxCoinViewModel.class);
        }
        return viewModel;
    }

    @Override
    protected FsActivityWinFoxCoinBinding createBinding() {
        return FsActivityWinFoxCoinBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        ImmersionBar.with(this)
                .statusBarDarkFont(false)
                .statusBarView(binding.fsTopView)
                .hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR)
                .transparentBar()
                .init();

        binding.getRoot().post(() -> {
            FoxSdkAnimation.translation("HORIZONTAL", binding.getRoot(), -600, 0, 300);
            FoxSdkAnimation.translation("HORIZONTAL", binding.fsLlAll, -600, 0, 300);
        });

        FoxSdkViewExt.setOnClickListener(binding.fsLlBack, v -> finish());
        binding.fsLlAll.setOnClickListener(v -> {
        });
        FoxSdkViewExt.setOnClickListener(binding.getRoot(), v -> finish());

        binding.fsRefresh.setRefreshHeader(new ClassicsHeader(this));
        binding.fsRefresh.setRefreshFooter(new ClassicsFooter(this));
        binding.fsRefresh.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onRefresh(RefreshLayout refreshLayout) {
                FSWinFoxCoinViewState currentState = getViewModel().getCurrentState();
                if (!currentState.isRefreshing() &&
                        !currentState.isLoadingMore() &&
                        !currentState.isLoading()) {
                    dispatch(new FSWinFoxCoinIntent.Refresh());
                }
            }

            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                FSWinFoxCoinViewState currentState = getViewModel().getCurrentState();
                if (!currentState.isRefreshing() &&
                        !currentState.isLoadingMore() &&
                        !currentState.isLoading()) {
                    dispatch(new FSWinFoxCoinIntent.LoadMore());
                }
            }
        });

        winFoxCoinAdapter = new FSWinFoxCoinAdapter(new ArrayList<>());
        binding.fsRecyclerView.setAdapter(winFoxCoinAdapter);
        winFoxCoinAdapter.addChildClickViewIds(R.id.fs_shp_tv_on);
        winFoxCoinAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            FoxSdkViewExt.setOnClickListener(view, v -> {
                FSWinFoxCoin item = winFoxCoinAdapter.getItem(position);
                if (item != null) {
                    Gson gson = new Gson();
                    FSGameSchemeData gameSchemeData = new FSGameSchemeData(
                            "wish_game",
                            item.getTask_number(),
                            item.getType_name(),
                            true,
                            ""
                    );
                    String json = gson.toJson(gameSchemeData);
                    FoxSdkAppJumpUtils.launchByDeepLink(
                            FSWinFoxCoinActivity.this,
                            "sohugloba://app/game_sdk?gameData=" + json,
                            item.getH5_url()
                    );
                }
            });
        });
    }

    @Override
    protected void renderState(FSWinFoxCoinViewState state) {
        if (state.isInit()) {
            dispatch(new FSWinFoxCoinIntent.LoadInitial());
        } else {
            if (!state.isLoading()) {
                if (!state.isRefreshing())
                    binding.fsRefresh.finishRefresh();

                if (!state.isLoadingMore())
                    binding.fsRefresh.finishLoadMore();

                if ((state.getWinFoxCoinList() == null || state.getWinFoxCoinList().isEmpty()) && (state.getMoreWinFoxCoinList() == null || state.getMoreWinFoxCoinList().isEmpty())) {
                    // 空布局
                    winFoxCoinAdapter.setUseEmpty(true);
                    winFoxCoinAdapter.setEmptyView(R.layout.fs_layout_empty);
                } else if (state.getWinFoxCoinList() != null && !state.getWinFoxCoinList().isEmpty()) {
                    winFoxCoinAdapter.setNewInstance(state.getWinFoxCoinList());
                } else {
                    winFoxCoinAdapter.addData(state.getMoreWinFoxCoinList());
                }

                if (!state.isHasMore())
                    binding.fsRefresh.finishLoadMoreWithNoMoreData();
            }
        }
    }

    @Override
    public void finish() {
        FoxSdkAnimation.translation("HORIZONTAL", binding.fsLlAll, 0, -600, 300);
        FoxSdkAnimation.translation("HORIZONTAL", binding.getRoot(), 0, -600, 300)
                .subscribe(() -> binding.getRoot().postDelayed(super::finish, 50L));
    }
}
