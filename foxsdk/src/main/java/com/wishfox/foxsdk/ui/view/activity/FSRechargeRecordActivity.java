package com.wishfox.foxsdk.ui.view.activity;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.databinding.FsActivityRechargeRecordBinding;
import com.wishfox.foxsdk.di.FoxSdkViewModelFactory;
import com.wishfox.foxsdk.domain.intent.FSRechargeRecordIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.ui.view.adapter.FSRechargeRecordAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSRechargeRecordViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSRechargeRecordViewState;
import com.wishfox.foxsdk.utils.FoxSdkAnimation;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 20:33
 */
public class FSRechargeRecordActivity extends FoxSdkBaseMviActivity<FSRechargeRecordViewState, FSRechargeRecordIntent, FsActivityRechargeRecordBinding> {

    private FSRechargeRecordViewModel viewModel;
    private FSRechargeRecordAdapter rechargeRecordAdapter;

    public static void start(Context context) {
        context.startActivity(new Intent(context, FSRechargeRecordActivity.class));
    }

    @Override
    protected FSRechargeRecordViewModel getViewModel() {
        if (viewModel == null) {
            FoxSdkViewModelFactory factory = new FoxSdkViewModelFactory();
            viewModel = new ViewModelProvider(this, factory).get(FSRechargeRecordViewModel.class);
        }
        return viewModel;
    }

    @Override
    protected FsActivityRechargeRecordBinding createBinding() {
        return FsActivityRechargeRecordBinding.inflate(getLayoutInflater());
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
                FSRechargeRecordViewState currentState = getViewModel().getCurrentState();
                if (!currentState.isRefreshing() &&
                        !currentState.isLoadingMore() &&
                        !currentState.isLoading()) {
                    dispatch(new FSRechargeRecordIntent.Refresh());
                }
            }

            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                FSRechargeRecordViewState currentState = getViewModel().getCurrentState();
                if (!currentState.isRefreshing() &&
                        !currentState.isLoadingMore() &&
                        !currentState.isLoading()) {
                    dispatch(new FSRechargeRecordIntent.LoadMore());
                }
            }
        });

        rechargeRecordAdapter = new FSRechargeRecordAdapter(new ArrayList<>());
        binding.fsRecyclerView.setAdapter(rechargeRecordAdapter);
    }

    @Override
    protected void renderState(FSRechargeRecordViewState state) {
        if (state.isInit()) {
            dispatch(new FSRechargeRecordIntent.LoadInitial());
        } else {
            if (!state.isLoading()) {
                if (!state.isRefreshing())
                    binding.fsRefresh.finishRefresh();

                if (!state.isLoadingMore())
                    binding.fsRefresh.finishLoadMore();

                if ((state.getRechargeRecordList() == null || state.getRechargeRecordList().isEmpty()) && (state.getMoreRechargeRecordList() == null || state.getMoreRechargeRecordList().isEmpty())) {
                    // 空布局
                    rechargeRecordAdapter.setUseEmpty(true);
                    rechargeRecordAdapter.setEmptyView(R.layout.fs_layout_empty);
                } else if (state.getRechargeRecordList() != null && !state.getRechargeRecordList().isEmpty()) {
                    rechargeRecordAdapter.setNewInstance(state.getRechargeRecordList());
                } else {
                    rechargeRecordAdapter.addData(state.getMoreRechargeRecordList());
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
