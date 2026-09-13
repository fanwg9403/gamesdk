package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.view.LayoutInflater;

import com.google.gson.Gson;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSGameSchemeData;
import com.wishfox.foxsdk.data.model.entity.FSWinFoxCoin;
import com.wishfox.foxsdk.databinding.FsActivityWinFoxCoinBinding;
import com.wishfox.foxsdk.di.FoxSdkRepositoryContainer;
import com.wishfox.foxsdk.domain.intent.FSWinFoxCoinIntent;
import com.wishfox.foxsdk.ui.view.adapter.FSWinFoxCoinAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSWinFoxCoinViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSWinFoxCoinViewState;
import com.wishfox.foxsdk.utils.FoxSdkAppJumpUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;

/**
 * 赢狐币页的宿主内 Overlay 实现。
 */
public final class FSWinFoxCoinOverlayView extends FSOverlayPageView {

    private final FsActivityWinFoxCoinBinding binding;
    private final FSWinFoxCoinViewModel viewModel;
    private final FSWinFoxCoinAdapter adapter;

    public FSWinFoxCoinOverlayView(Activity activity, Callback callback) {
        super(activity, callback);
        binding = FsActivityWinFoxCoinBinding.inflate(LayoutInflater.from(activity), this, true);
        viewModel = new FSWinFoxCoinViewModel(
                FoxSdkRepositoryContainer.getWinFoxCoinRepository()
        );
        adapter = new FSWinFoxCoinAdapter(new ArrayList<FSWinFoxCoin>());

        initView();
        bindViewModel(viewModel);
        disposables.add(viewModel.getViewState().subscribe(
                this::renderState,
                throwable -> com.wishfox.foxsdk.core.FoxSdkDiagnostics.reportFailure(
                        activity, "win_fox_coin", "state_observer_failed", throwable
                )
        ));
        viewModel.dispatch(new FSWinFoxCoinIntent.LoadInitial());
    }

    private void initView() {
        applyWindowInsets(
                binding.fsTopView,
                findViewById(R.id.fs_v_start_safe_area),
                binding.fsLlAll
        );
        FoxSdkViewExt.setOnClickListener(binding.fsLlBack, v -> requestClose());
        FoxSdkViewExt.setOnClickListener(binding.getRoot(), v -> {
            if (v == binding.getRoot()) {
                requestClose();
            }
        });
        binding.fsRightView.setOnClickListener(v -> requestClose());
        binding.fsRefresh.setRefreshHeader(new ClassicsHeader(activity));
        binding.fsRefresh.setRefreshFooter(new ClassicsFooter(activity));
        binding.fsRefresh.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onRefresh(RefreshLayout refreshLayout) {
                FSWinFoxCoinViewState state = viewModel.getCurrentState();
                if (state != null && !state.isRefreshing() &&
                        !state.isLoadingMore() && !state.isLoading()) {
                    viewModel.dispatch(new FSWinFoxCoinIntent.Refresh());
                }
            }

            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                FSWinFoxCoinViewState state = viewModel.getCurrentState();
                if (state != null && !state.isRefreshing() &&
                        !state.isLoadingMore() && !state.isLoading()) {
                    viewModel.dispatch(new FSWinFoxCoinIntent.LoadMore());
                }
            }
        });
        binding.fsRecyclerView.setAdapter(adapter);
        adapter.addChildClickViewIds(R.id.fs_shp_tv_on);
        adapter.setOnItemChildClickListener((baseAdapter, view, position) -> {
            FSWinFoxCoin item = adapter.getItem(position);
            if (item == null) {
                return;
            }
            FSGameSchemeData gameSchemeData = new FSGameSchemeData(
                    "wish_game",
                    item.getTask_number(),
                    item.getType_name(),
                    true,
                    ""
            );
            String json = new Gson().toJson(gameSchemeData);
            FoxSdkAppJumpUtils.launchByDeepLink(
                    activity,
                    "sohugloba://app/game_sdk?gameData=" + json,
                    item.getH5_url()
            );
        });
    }

    private void renderState(FSWinFoxCoinViewState state) {
        if (isDestroyedForOverlay() || state == null) {
            return;
        }
        if (state.isInit()) {
            viewModel.dispatch(new FSWinFoxCoinIntent.LoadInitial());
            return;
        }
        if (state.isLoading()) {
            return;
        }

        if (!state.isRefreshing()) {
            binding.fsRefresh.finishRefresh();
        }
        if (!state.isLoadingMore()) {
            binding.fsRefresh.finishLoadMore();
        }

        if ((state.getWinFoxCoinList() == null || state.getWinFoxCoinList().isEmpty()) &&
                (state.getMoreWinFoxCoinList() == null || state.getMoreWinFoxCoinList().isEmpty())) {
            adapter.setUseEmpty(true);
            adapter.setEmptyView(R.layout.fs_layout_empty);
        } else if (state.getWinFoxCoinList() != null && !state.getWinFoxCoinList().isEmpty()) {
            adapter.setNewInstance(state.getWinFoxCoinList());
        } else if (state.getMoreWinFoxCoinList() != null) {
            adapter.addData(state.getMoreWinFoxCoinList());
        }

        if (!state.isHasMore()) {
            binding.fsRefresh.finishLoadMoreWithNoMoreData();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        viewModel.disposeForOverlay();
    }
}
