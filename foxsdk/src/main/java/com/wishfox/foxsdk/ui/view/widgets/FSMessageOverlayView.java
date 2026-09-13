package com.wishfox.foxsdk.ui.view.widgets;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import com.hjq.toast.Toaster;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSMessage;
import com.wishfox.foxsdk.databinding.FsActivityMessageBinding;
import com.wishfox.foxsdk.di.FoxSdkRepositoryContainer;
import com.wishfox.foxsdk.domain.intent.FSMessageIntent;
import com.wishfox.foxsdk.ui.view.adapter.FSMessageListAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSMessageViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSMessageViewState;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息页的宿主内 Overlay 实现。
 *
 * <p>消息布局同时提供了 portrait 和 landscape 资源：
 * 竖屏详情覆盖消息列表；横屏详情位于列表右侧。详情返回只收起详情，
 * 列表返回才回到 SDK 首页。</p>
 */
public final class FSMessageOverlayView extends FSOverlayPageView {

    private final FsActivityMessageBinding binding;
    private final FSMessageViewModel viewModel;
    private final FSMessageListAdapter adapter;
    private boolean detailShown;
    private FSMessage detailMessage;

    public FSMessageOverlayView(Activity activity, Callback callback) {
        this(activity, callback, null, false);
    }

    public FSMessageOverlayView(
            Activity activity,
            Callback callback,
            FSMessage detailMessage,
            boolean detailShown
    ) {
        super(activity, callback);
        binding = FsActivityMessageBinding.inflate(LayoutInflater.from(activity), this, true);
        viewModel = new FSMessageViewModel(
                FoxSdkRepositoryContainer.getMessageRepository()
        );
        adapter = new FSMessageListAdapter(new ArrayList<FSMessage>());

        initView();
        bindViewModel(viewModel);
        disposables.add(viewModel.getViewState().subscribe(
                this::renderState,
                throwable -> com.wishfox.foxsdk.core.FoxSdkDiagnostics.reportFailure(
                        activity, "message", "state_observer_failed", throwable
                )
        ));
        if (detailShown && detailMessage != null) {
            showDetail(detailMessage);
        }
    }

    private void initView() {
        applyWindowInsets(
                binding.fsVTopSafeArea,
                findViewById(R.id.fs_v_start_safe_area),
                binding.fsMessageRoot
        );
        binding.fsMessageDetail.setOnClickListener(v -> {
            // Consume clicks inside the detail page so they cannot close the SDK.
        });
        FoxSdkViewExt.setOnClickListener(binding.fsTvDetailBack, v -> hideDetail());
        FoxSdkViewExt.setOnClickListener(binding.fsIvBack, v -> requestClose());
        FoxSdkViewExt.setOnClickListener(binding.fsVOutside, v -> {
            if (detailShown) {
                hideDetail();
            } else {
                requestClose();
            }
        });
        FoxSdkViewExt.setOnClickListener(binding.fsIvClear, v ->
                viewModel.dispatch(new FSMessageIntent.Read())
        );
        binding.fsRefresh.setRefreshHeader(new ClassicsHeader(activity));
        binding.fsRefresh.setOnRefreshListener(refreshLayout ->
                viewModel.dispatch(new FSMessageIntent.Refresh())
        );

        adapter.setOnItemClickListener((baseAdapter, view, position) -> {
            FSMessage message = (FSMessage) baseAdapter.getItem(position);
            if (message == null) {
                return;
            }
            showDetail(message);
            if (!message.isRead()) {
                viewModel.dispatch(new FSMessageIntent.Read(message.getMailId()));
            }
        });
        binding.fsRv.setAdapter(adapter);
    }

    private void showDetail(FSMessage message) {
        if (message == null || isDestroyedForOverlay()) {
            return;
        }
        binding.fsTvDetailTitle.setText(message.getTitle());
        binding.fsTvDetailContent.setText(message.getContent());
        binding.fsTvDetailTime.setText(message.getTime());
        detailMessage = message;
        detailShown = true;
        binding.fsMessageDetail.setVisibility(View.VISIBLE);
    }

    private void hideDetail() {
        if (!detailShown) {
            return;
        }
        detailShown = false;
        detailMessage = null;
        binding.fsMessageDetail.setVisibility(View.INVISIBLE);
    }

    public FSMessage getDetailMessage() {
        return detailMessage;
    }

    public boolean isDetailShown() {
        return detailShown;
    }

    @Override
    protected void handleBackPressed() {
        if (detailShown) {
            hideDetail();
        } else {
            requestClose();
        }
    }

    private void renderState(FSMessageViewState state) {
        if (isDestroyedForOverlay() || state == null) {
            return;
        }
        if (state instanceof FSMessageViewState.Init) {
            adapter.setUseEmpty(true);
            adapter.setEmptyView(R.layout.fs_layout_empty);
            viewModel.dispatch(new FSMessageIntent.Refresh());
        } else if (state instanceof FSMessageViewState.LoadList) {
            FSMessageViewState.LoadList loadList = (FSMessageViewState.LoadList) state;
            binding.fsRefresh.finishRefresh();
            if (loadList.isRefresh()) {
                adapter.setNewInstance(loadList.getList());
            } else {
                adapter.addData(loadList.getList());
            }
        } else if (state instanceof FSMessageViewState.Read) {
            FSMessageViewState.Read readState = (FSMessageViewState.Read) state;
            if (readState.getId() == null) {
                List<FSMessage> list = adapter.getData();
                for (FSMessage message : list) {
                    if (message != null) {
                        message.setIs_read(1);
                    }
                }
                adapter.setNewInstance(list);
                Toaster.show("清除未读消息成功");
            } else {
                FSMessage data = null;
                for (FSMessage message : adapter.getData()) {
                    if (message != null &&
                            (readState.getId().equals(message.getId()) ||
                                    readState.getId().equals(message.getMailId()))) {
                        data = message;
                        break;
                    }
                }
                if (data != null) {
                    int index = adapter.getData().indexOf(data);
                    data.setIs_read(1);
                    adapter.setData(index, data);
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        viewModel.disposeForOverlay();
    }
}
