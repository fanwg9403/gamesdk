package com.wishfox.foxsdk.ui.view.activity;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.databinding.FsActivityGameRecordBinding;
import com.wishfox.foxsdk.di.FoxSdkViewModelFactory;
import com.wishfox.foxsdk.domain.intent.FSGameRecordIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.ui.view.adapter.FSGameRecordAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSGameRecordViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSGameRecordViewState;
import com.wishfox.foxsdk.utils.FoxSdkAnimation;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 20:28
 */
public class FSGameRecordActivity extends FoxSdkBaseMviActivity<FSGameRecordViewState, FSGameRecordIntent, FsActivityGameRecordBinding> {

    private FSGameRecordViewModel viewModel;
    private FSGameRecordAdapter gameRecordAdapter;

    public static void start(Context context) {
        context.startActivity(new Intent(context, FSGameRecordActivity.class));
    }

    @Override
    protected FSGameRecordViewModel getViewModel() {
        if (viewModel == null) {
            FoxSdkViewModelFactory factory = new FoxSdkViewModelFactory();
            viewModel = new ViewModelProvider(this, factory).get(FSGameRecordViewModel.class);
        }
        return viewModel;
    }

    @Override
    protected FsActivityGameRecordBinding createBinding() {
        return FsActivityGameRecordBinding.inflate(getLayoutInflater());
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
        binding.fsRefresh.setEnableLoadMore(false);
        binding.fsRefresh.setOnRefreshListener(refreshLayout -> {
            FSGameRecordViewState currentState = getViewModel().getCurrentState();
            if (!currentState.isRefreshing() && !currentState.isLoading())
                dispatch(new FSGameRecordIntent.Refresh());
        });

        gameRecordAdapter = new FSGameRecordAdapter(new ArrayList<>());
        binding.fsRecyclerView.setAdapter(gameRecordAdapter);
    }

    @Override
    protected void renderState(FSGameRecordViewState state) {
        if (state.isInit()) {
            dispatch(new FSGameRecordIntent.LoadInitial());
        } else {
            if (!state.isLoading()) {
                if (!state.isRefreshing())
                    binding.fsRefresh.finishRefresh();

                if (!state.isLoadingMore())
                    binding.fsRefresh.finishLoadMore();

                if ((state.getGameRecordList() == null || state.getGameRecordList().isEmpty()) && (state.getMoreGameRecordList() == null || state.getMoreGameRecordList().isEmpty())) {
                    // 空布局
                    gameRecordAdapter.setUseEmpty(true);
                    gameRecordAdapter.setEmptyView(R.layout.fs_layout_empty);
                } else if (state.getGameRecordList() != null && !state.getGameRecordList().isEmpty()) {
                    gameRecordAdapter.setNewInstance(state.getGameRecordList());
                } else {
                    gameRecordAdapter.addData(state.getMoreGameRecordList());
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
