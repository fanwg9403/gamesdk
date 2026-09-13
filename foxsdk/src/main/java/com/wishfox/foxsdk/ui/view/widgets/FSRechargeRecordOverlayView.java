package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.view.LayoutInflater;

import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSRechargeRecord;
import com.wishfox.foxsdk.databinding.FsActivityRechargeRecordBinding;
import com.wishfox.foxsdk.di.FoxSdkRepositoryContainer;
import com.wishfox.foxsdk.domain.intent.FSRechargeRecordIntent;
import com.wishfox.foxsdk.ui.view.adapter.FSRechargeRecordAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSRechargeRecordViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSRechargeRecordViewState;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;

/**
 * 充值记录页的宿主内 Overlay 实现。
 */
public final class FSRechargeRecordOverlayView extends FSOverlayPageView {

    private final FsActivityRechargeRecordBinding binding;
    private final FSRechargeRecordViewModel viewModel;
    private final FSRechargeRecordAdapter adapter;

    public FSRechargeRecordOverlayView(Activity activity, Callback callback) {
        super(activity, callback);
        binding = FsActivityRechargeRecordBinding.inflate(LayoutInflater.from(activity), this, true);
        viewModel = new FSRechargeRecordViewModel(
                FoxSdkRepositoryContainer.getRechargeRecordRepository()
        );
        adapter = new FSRechargeRecordAdapter(new ArrayList<FSRechargeRecord>());

        initView();
        bindViewModel(viewModel);
        disposables.add(viewModel.getViewState().subscribe(
                this::renderState,
                throwable -> com.wishfox.foxsdk.core.FoxSdkDiagnostics.reportFailure(
                        activity, "recharge_record", "state_observer_failed", throwable
                )
        ));
        viewModel.dispatch(new FSRechargeRecordIntent.LoadInitial());
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
                FSRechargeRecordViewState state = viewModel.getCurrentState();
                if (state != null && !state.isRefreshing() &&
                        !state.isLoadingMore() && !state.isLoading()) {
                    viewModel.dispatch(new FSRechargeRecordIntent.Refresh());
                }
            }

            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                FSRechargeRecordViewState state = viewModel.getCurrentState();
                if (state != null && !state.isRefreshing() &&
                        !state.isLoadingMore() && !state.isLoading()) {
                    viewModel.dispatch(new FSRechargeRecordIntent.LoadMore());
                }
            }
        });
        binding.fsRecyclerView.setAdapter(adapter);
    }

    private void renderState(FSRechargeRecordViewState state) {
        if (isDestroyedForOverlay() || state == null) {
            return;
        }
        if (state.isInit()) {
            viewModel.dispatch(new FSRechargeRecordIntent.LoadInitial());
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

        if ((state.getRechargeRecordList() == null || state.getRechargeRecordList().isEmpty()) &&
                (state.getMoreRechargeRecordList() == null || state.getMoreRechargeRecordList().isEmpty())) {
            adapter.setUseEmpty(true);
            adapter.setEmptyView(R.layout.fs_layout_empty);
        } else if (state.getRechargeRecordList() != null && !state.getRechargeRecordList().isEmpty()) {
            adapter.setNewInstance(state.getRechargeRecordList());
        } else if (state.getMoreRechargeRecordList() != null) {
            adapter.addData(state.getMoreRechargeRecordList());
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
