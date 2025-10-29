package com.wishfox.foxsdk.ui.view.activity;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.toast.Toaster;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSStarterPack;
import com.wishfox.foxsdk.databinding.FsActivityStarterPackBinding;
import com.wishfox.foxsdk.di.FoxSdkViewModelFactory;
import com.wishfox.foxsdk.domain.intent.FSStarterPackIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.ui.view.adapter.FSStarterPackAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSStarterPackViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSStarterPackViewState;
import com.wishfox.foxsdk.utils.FoxSdkAnimation;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 20:38
 */
public class FSStarterPackActivity extends FoxSdkBaseMviActivity<FSStarterPackViewState, FSStarterPackIntent, FsActivityStarterPackBinding> {

    private FSStarterPackViewModel viewModel;
    private FSStarterPackAdapter starterPackAdapter;
    private long lastClickTime = 0L;
    private static final long CLICK_INTERVAL = 3000;

    public static void start(Context context) {
        context.startActivity(new Intent(context, FSStarterPackActivity.class));
    }

    @Override
    protected FSStarterPackViewModel getViewModel() {
        if (viewModel == null) {
            FoxSdkViewModelFactory factory = new FoxSdkViewModelFactory();
            viewModel = new ViewModelProvider(this, factory).get(FSStarterPackViewModel.class);
        }
        return viewModel;
    }

    @Override
    protected FsActivityStarterPackBinding createBinding() {
        return FsActivityStarterPackBinding.inflate(getLayoutInflater());
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

        starterPackAdapter = new FSStarterPackAdapter(new ArrayList<>());

        FoxSdkViewExt.setOnClickListener(binding.fsLlBack, v -> finish());
        binding.fsLlAll.setOnClickListener(v -> {
        });
        FoxSdkViewExt.setOnClickListener(binding.getRoot(), v -> finish());

        binding.fsRefresh.setRefreshHeader(new ClassicsHeader(this));
        binding.fsRefresh.setRefreshFooter(new ClassicsFooter(this));
        binding.fsRefresh.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onRefresh(RefreshLayout refreshLayout) {
                FSStarterPackViewState currentState = getViewModel().getCurrentState();
                if (!currentState.isRefreshing() &&
                        !currentState.isLoadingMore() &&
                        !currentState.isLoading()) {
                    dispatch(new FSStarterPackIntent.Refresh());
                }
            }

            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                FSStarterPackViewState currentState = getViewModel().getCurrentState();
                if (!currentState.isRefreshing() &&
                        !currentState.isLoadingMore() &&
                        !currentState.isLoading()) {
                    dispatch(new FSStarterPackIntent.LoadMore());
                }
            }
        });

        binding.fsRecyclerView.setAdapter(starterPackAdapter);
        starterPackAdapter.addChildClickViewIds(R.id.fs_shp_tv_on);
        starterPackAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            viewModel.setClickPosition(position);
            FSStarterPack item = starterPackAdapter.getItem(position);
            if (item != null) {
                if (item.getStatus() == null || item.getStatus() != 1) {
                    FoxSdkUtils.copyText(FSStarterPackActivity.this, item.getCode());
                    Toaster.show(getString(R.string.fs_str_redemption_successful));
                } else {
                    dispatch(new FSStarterPackIntent.ReceiveStarterPack(item.getId().toString()));
                }
            }
        });
    }

    /**
     * 根据状态渲染页面
     */
    @Override
    protected void renderState(FSStarterPackViewState state) {
        if (state.isInit()) {
            dispatch(new FSStarterPackIntent.LoadInitial());
        } else {
            if (!state.isLoading()) {
                if (!state.isRefreshing())
                    binding.fsRefresh.finishRefresh();

                if (!state.isLoadingMore())
                    binding.fsRefresh.finishLoadMore();

                if ((state.getStarterPackList() == null || state.getStarterPackList().isEmpty()) && (state.getMoreStarterPackList() == null || state.getMoreStarterPackList().isEmpty())) {
                    // 空布局
                    starterPackAdapter.setUseEmpty(true);
                    starterPackAdapter.setEmptyView(R.layout.fs_layout_empty);
                } else if (state.getStarterPackList() != null && !state.getStarterPackList().isEmpty()) {
                    starterPackAdapter.setNewInstance(state.getStarterPackList());
                } else {
                    starterPackAdapter.addData(state.getMoreStarterPackList());
                }

                if (!state.isHasMore())
                    binding.fsRefresh.finishLoadMoreWithNoMoreData();
            }
        }

        if (state.isStateViewEnable()) {
            int clickPosition = viewModel.getClickPosition();
            if (clickPosition != -1 && clickPosition < starterPackAdapter.getItemCount()) {
                FSStarterPack item = starterPackAdapter.getItem(clickPosition);
                if (item != null) {
                    item.setStatus(2);
                    starterPackAdapter.notifyItemChanged(clickPosition);
                    FoxSdkUtils.copyText(this, item.getCode());
                    Toaster.show(getString(R.string.fs_str_redemption_successful));
                }
            }
            viewModel.modifyReceiveState();
        }
    }

    @Override
    public void finish() {
        FoxSdkAnimation.translation("HORIZONTAL", binding.fsLlAll, 0, -600, 300);
        FoxSdkAnimation.translation("HORIZONTAL", binding.getRoot(), 0, -600, 300)
                .subscribe(() -> binding.getRoot().postDelayed(super::finish, 50L));
    }
}
