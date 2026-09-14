package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.view.LayoutInflater;

import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSGameRecord;
import com.wishfox.foxsdk.databinding.FsActivityGameRecordBinding;
import com.wishfox.foxsdk.di.FoxSdkRepositoryContainer;
import com.wishfox.foxsdk.domain.intent.FSGameRecordIntent;
import com.wishfox.foxsdk.ui.view.adapter.FSGameRecordAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSGameRecordViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSGameRecordViewState;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;

/**
 * 游戏记录页的宿主内 Overlay 实现，负责展示记录列表并处理刷新与分页状态。
 */
public final class FSGameRecordOverlayView extends FSOverlayPageView {

    private final FsActivityGameRecordBinding binding;
    private final FSGameRecordViewModel viewModel;
    private final FSGameRecordAdapter adapter;

    /**
     * 创建游戏记录页面并启动首次加载。
     *
     * @param activity 宿主 Activity
     * @param callback Overlay 关闭回调
     */
    public FSGameRecordOverlayView(Activity activity, Callback callback) {
        super(activity, callback);
        binding = FsActivityGameRecordBinding.inflate(LayoutInflater.from(activity), this, true);
        viewModel = new FSGameRecordViewModel(
                FoxSdkRepositoryContainer.getGameRecordRepository()
        );
        adapter = new FSGameRecordAdapter(new ArrayList<FSGameRecord>());

        initView();
        bindViewModel(viewModel);
        disposables.add(viewModel.getViewState().subscribe(
                this::renderState,
                throwable -> com.wishfox.foxsdk.core.FoxSdkDiagnostics.reportFailure(
                        activity, "game_record", "state_observer_failed", throwable
                )
        ));
        viewModel.dispatch(new FSGameRecordIntent.LoadInitial());
    }

    /**
     * 初始化安全区、返回按钮、刷新控件和列表适配器。
     */
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
        binding.fsRefresh.setEnableLoadMore(false);
        binding.fsRefresh.setOnRefreshListener(refreshLayout -> {
            FSGameRecordViewState state = viewModel.getCurrentState();
            if (state != null && !state.isRefreshing() && !state.isLoading()) {
                viewModel.dispatch(new FSGameRecordIntent.Refresh());
            }
        });
        binding.fsRecyclerView.setAdapter(adapter);
    }

    /**
     * 根据 ViewModel 状态更新列表数据及刷新控件状态。
     *
     * @param state 当前游戏记录页面状态
     */
    private void renderState(FSGameRecordViewState state) {
        if (isDestroyedForOverlay() || state == null) {
            return;
        }
        if (state.isInit()) {
            viewModel.dispatch(new FSGameRecordIntent.LoadInitial());
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

        if ((state.getGameRecordList() == null || state.getGameRecordList().isEmpty()) &&
                (state.getMoreGameRecordList() == null || state.getMoreGameRecordList().isEmpty())) {
            adapter.setUseEmpty(true);
            adapter.setEmptyView(R.layout.fs_layout_empty);
        } else if (state.getGameRecordList() != null && !state.getGameRecordList().isEmpty()) {
            adapter.setNewInstance(state.getGameRecordList());
        } else if (state.getMoreGameRecordList() != null) {
            adapter.addData(state.getMoreGameRecordList());
        }

        if (!state.isHasMore()) {
            binding.fsRefresh.finishLoadMoreWithNoMoreData();
        }
    }

    /**
     * 销毁页面并释放 ViewModel 订阅资源。
     */
    @Override
    public void destroy() {
        super.destroy();
        viewModel.disposeForOverlay();
    }
}
