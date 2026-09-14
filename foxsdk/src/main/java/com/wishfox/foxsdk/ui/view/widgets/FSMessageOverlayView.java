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
 * 消息页的宿主内 Overlay 实现，支持消息列表、已读处理和详情覆盖层。
 *
 * <p>消息布局同时提供了竖屏和横屏资源：
 * 竖屏详情覆盖消息列表；横屏详情位于列表右侧。详情返回只收起详情，
 * 列表返回才回到 SDK 首页。</p>
 */
public final class FSMessageOverlayView extends FSOverlayPageView {

    private final FsActivityMessageBinding binding;
    private final FSMessageViewModel viewModel;
    private final FSMessageListAdapter adapter;
    private boolean detailShown;
    private FSMessage detailMessage;

    /**
     * 创建消息页面并显示消息列表。
     *
     * @param activity 宿主 Activity
     * @param callback Overlay 关闭回调
     */
    public FSMessageOverlayView(Activity activity, Callback callback) {
        this(activity, callback, null, false);
    }

    /**
     * 创建消息页面，并按需恢复已打开的消息详情。
     *
     * @param activity 宿主 Activity
     * @param callback Overlay 关闭回调
     * @param detailMessage 需要恢复的消息详情
     * @param detailShown 是否显示消息详情
     */
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

    /**
     * 初始化安全区、导航按钮、刷新控件、列表和点击事件。
     */
    private void initView() {
        applyWindowInsets(
                binding.fsVTopSafeArea,
                findViewById(R.id.fs_v_start_safe_area),
                binding.fsMessageRoot
        );
        binding.fsMessageDetail.setOnClickListener(v -> {
            // 消费详情页内部点击，避免事件冒泡导致 SDK 被关闭。
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

    /**
     * 展示指定消息的详情内容。
     *
     * @param message 要展示的消息
     */
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

    /**
     * 收起当前消息详情并恢复列表视图。
     */
    private void hideDetail() {
        if (!detailShown) {
            return;
        }
        detailShown = false;
        detailMessage = null;
        binding.fsMessageDetail.setVisibility(View.INVISIBLE);
    }

    /**
     * 获取当前正在展示的消息详情。
     *
     * @return 当前详情消息，没有详情时返回 {@code null}
     */
    public FSMessage getDetailMessage() {
        return detailMessage;
    }

    /**
     * 判断消息详情是否处于显示状态。
     *
     * @return 显示详情时返回 {@code true}
     */
    public boolean isDetailShown() {
        return detailShown;
    }

    /**
     * 处理返回键：优先收起详情，否则关闭页面。
     */
    @Override
    protected void handleBackPressed() {
        if (detailShown) {
            hideDetail();
        } else {
            requestClose();
        }
    }

    /**
     * 根据 ViewModel 状态刷新消息列表并同步已读标记。
     *
     * @param state 当前消息页面状态
     */
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

    /**
     * 销毁页面并释放 ViewModel 订阅资源。
     */
    @Override
    public void destroy() {
        super.destroy();
        viewModel.disposeForOverlay();
    }
}
