package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.view.LayoutInflater;

import com.hjq.toast.Toaster;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSStarterPack;
import com.wishfox.foxsdk.databinding.FsActivityStarterPackBinding;
import com.wishfox.foxsdk.di.FoxSdkRepositoryContainer;
import com.wishfox.foxsdk.domain.intent.FSStarterPackIntent;
import com.wishfox.foxsdk.ui.view.adapter.FSStarterPackAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSStarterPackViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSStarterPackViewState;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;

/**
 * 新手礼包页的宿主内 Overlay 实现。
 */
public final class FSStarterPackOverlayView extends FSOverlayPageView {

    private final FsActivityStarterPackBinding binding;
    private final FSStarterPackViewModel viewModel;
    private final FSStarterPackAdapter adapter;

    public FSStarterPackOverlayView(Activity activity, Callback callback) {
        super(activity, callback);
        binding = FsActivityStarterPackBinding.inflate(LayoutInflater.from(activity), this, true);
        viewModel = new FSStarterPackViewModel(
                FoxSdkRepositoryContainer.getStarterPackRepository()
        );
        adapter = new FSStarterPackAdapter(new ArrayList<FSStarterPack>());

        initView();
        bindViewModel(viewModel);
        disposables.add(viewModel.getViewState().subscribe(
                this::renderState,
                throwable -> com.wishfox.foxsdk.core.FoxSdkDiagnostics.reportFailure(
                        activity, "starter_pack", "state_observer_failed", throwable
                )
        ));
        viewModel.dispatch(new FSStarterPackIntent.LoadInitial());
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
                FSStarterPackViewState state = viewModel.getCurrentState();
                if (state != null && !state.isRefreshing() &&
                        !state.isLoadingMore() && !state.isLoading()) {
                    viewModel.dispatch(new FSStarterPackIntent.Refresh());
                }
            }

            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                FSStarterPackViewState state = viewModel.getCurrentState();
                if (state != null && !state.isRefreshing() &&
                        !state.isLoadingMore() && !state.isLoading()) {
                    viewModel.dispatch(new FSStarterPackIntent.LoadMore());
                }
            }
        });
        binding.fsRecyclerView.setAdapter(adapter);
        adapter.addChildClickViewIds(R.id.fs_shp_tv_on);
        adapter.setOnItemChildClickListener((baseAdapter, view, position) -> {
            viewModel.setClickPosition(position);
            FSStarterPack item = adapter.getItem(position);
            if (item == null) {
                return;
            }
            if (item.getStatus() == null || item.getStatus() != 1) {
                FoxSdkUtils.copyText(activity, item.getCode());
                Toaster.show(activity.getString(R.string.fs_str_redemption_successful));
            } else {
                viewModel.dispatch(new FSStarterPackIntent.ReceiveStarterPack(
                        item.getId() == null ? "" : item.getId().toString()
                ));
            }
        });
    }

    private void renderState(FSStarterPackViewState state) {
        if (isDestroyedForOverlay() || state == null) {
            return;
        }
        if (state.isInit()) {
            viewModel.dispatch(new FSStarterPackIntent.LoadInitial());
        } else if (!state.isLoading()) {
            if (!state.isRefreshing()) {
                binding.fsRefresh.finishRefresh();
            }
            if (!state.isLoadingMore()) {
                binding.fsRefresh.finishLoadMore();
            }

            if ((state.getStarterPackList() == null || state.getStarterPackList().isEmpty()) &&
                    (state.getMoreStarterPackList() == null || state.getMoreStarterPackList().isEmpty())) {
                adapter.setUseEmpty(true);
                adapter.setEmptyView(R.layout.fs_layout_empty);
            } else if (state.getStarterPackList() != null && !state.getStarterPackList().isEmpty()) {
                adapter.setNewInstance(state.getStarterPackList());
            } else if (state.getMoreStarterPackList() != null) {
                adapter.addData(state.getMoreStarterPackList());
            }

            if (!state.isHasMore()) {
                binding.fsRefresh.finishLoadMoreWithNoMoreData();
            }
        }

        if (state.isStateViewEnable()) {
            int clickPosition = viewModel.getClickPosition();
            if (clickPosition >= 0 && clickPosition < adapter.getItemCount()) {
                FSStarterPack item = adapter.getItem(clickPosition);
                if (item != null) {
                    item.setStatus(2);
                    adapter.notifyItemChanged(clickPosition);
                    FoxSdkUtils.copyText(activity, item.getCode());
                    Toaster.show(activity.getString(R.string.fs_str_redemption_successful));
                }
            }
            viewModel.modifyReceiveState();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        viewModel.disposeForOverlay();
    }
}
