package com.wishfox.foxsdk.ui.view.activity;

import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.toast.Toaster;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.wishfox.foxsdk.R;
import com.wishfox.foxsdk.data.model.entity.FSMessage;
import com.wishfox.foxsdk.databinding.FsActivityMessageBinding;
import com.wishfox.foxsdk.di.FoxSdkViewModelFactory;
import com.wishfox.foxsdk.domain.intent.FSMessageIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviActivity;
import com.wishfox.foxsdk.ui.view.adapter.FSMessageListAdapter;
import com.wishfox.foxsdk.ui.viewmodel.FSMessageViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSMessageViewState;
import com.wishfox.foxsdk.utils.FoxSdkAnimation;
import com.wishfox.foxsdk.utils.FoxSdkUtils;
import com.wishfox.foxsdk.utils.FoxSdkViewExt;

import java.util.ArrayList;
import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 20:33
 */
public class FSMessageActivity extends FoxSdkBaseMviActivity<FSMessageViewState, FSMessageIntent, FsActivityMessageBinding> {

    private FSMessageViewModel viewModel;
    private FSMessageListAdapter mAdapter;
    private boolean _detailShow = false;

    @Override
    protected FSMessageViewModel getViewModel() {
        if (viewModel == null) {
            FoxSdkViewModelFactory factory = new FoxSdkViewModelFactory();
            viewModel = new ViewModelProvider(this, factory).get(FSMessageViewModel.class);
        }
        return viewModel;
    }

    @Override
    protected FsActivityMessageBinding createBinding() {
        return FsActivityMessageBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        ImmersionBar.with(this)
                .statusBarDarkFont(false)
                .transparentStatusBar()
                .statusBarView(binding.fsVTopSafeArea)
                .hideBar(BarHide.FLAG_HIDE_NAVIGATION_BAR)
                .init();

        binding.fsMessageRoot.post(() -> {
            binding.getRoot().postDelayed(() -> binding.fsVStartSafeArea.setAlpha(1f), 100L);
        });

        binding.fsMessageDetail.setOnClickListener(v -> { /* 防止点击事件穿透到空白区域触发关闭 */ });
        FoxSdkViewExt.setOnClickListener(binding.fsTvDetailBack, v -> hideDetail());
        FoxSdkViewExt.setOnClickListener(binding.fsVOutside, v -> finish());
        FoxSdkViewExt.setOnClickListener(binding.fsIvBack, v -> finish());
        FoxSdkViewExt.setOnClickListener(binding.fsIvClear, v -> dispatch(new FSMessageIntent.Read()));
        binding.fsRefresh.setRefreshHeader(new ClassicsHeader(this));
        binding.fsRefresh.setOnRefreshListener(refreshLayout ->
                dispatch(new FSMessageIntent.Refresh()));

        mAdapter = new FSMessageListAdapter(new ArrayList<>());
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            FSMessage item = (FSMessage) adapter.getItem(position);
            showDetail(item);
            if (item != null && !item.isRead()) {
                dispatch(new FSMessageIntent.Read(item.getMailId()));
            }
        });
        binding.fsRv.setAdapter(mAdapter);
    }

    private void showDetail(FSMessage item) {
        if (!_detailShow) {
            binding.fsTvDetailTitle.setText(item.getTitle());
            binding.fsTvDetailContent.setText(item.getContent());
            binding.fsTvDetailTime.setText(item.getTime());
            _detailShow = true;
            binding.fsMessageDetail.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void renderState(FSMessageViewState state) {
        if (state instanceof FSMessageViewState.LoadList) {
            FSMessageViewState.LoadList loadList = (FSMessageViewState.LoadList) state;
            binding.fsRefresh.finishRefresh();
            if (loadList.isRefresh()) {
                mAdapter.setNewInstance(loadList.getList());
            } else {
                mAdapter.addData(loadList.getList());
            }
        } else if (state instanceof FSMessageViewState.Read) {
            FSMessageViewState.Read readState = (FSMessageViewState.Read) state;
            if (readState.getId() == null) {
                List<FSMessage> list = mAdapter.getData();
                for (FSMessage message : list) {
                    message.setIs_read(1);
                }
                mAdapter.setNewInstance(list);
                Toaster.show("清除未读消息成功");
            } else {
                FSMessage data = null;
                for (FSMessage message : mAdapter.getData()) {
                    if (message.getId().equals(readState.getId())) {
                        data = message;
                        break;
                    }
                }
                if (data != null) {
                    int index = mAdapter.getData().indexOf(data);
                    data.setIs_read(1);
                    mAdapter.setData(index, data);
                }
            }
        } else if (state instanceof FSMessageViewState.Init) {
            mAdapter.setUseEmpty(true);
            mAdapter.setEmptyView(R.layout.fs_layout_empty);
            dispatch(new FSMessageIntent.Refresh());
        }
    }

    private void hideDetail() {
        _detailShow = false;
        binding.fsMessageDetail.setVisibility(View.INVISIBLE);
    }

    @Override
    public void finish() {
        if (_detailShow) {
            hideDetail();
        } else {
            binding.getRoot().postDelayed(super::finish, 50L);
        }
    }
}
