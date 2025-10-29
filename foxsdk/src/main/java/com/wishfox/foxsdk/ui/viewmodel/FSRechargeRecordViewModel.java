package com.wishfox.foxsdk.ui.viewmodel;

import com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest;
import com.wishfox.foxsdk.data.model.paging.PageConstants;
import com.wishfox.foxsdk.data.repository.FSRechargeRecordRepository;
import com.wishfox.foxsdk.domain.intent.FSRechargeRecordIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSRechargeRecordViewState;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;

import java.util.Collections;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 20:12
 */
public class FSRechargeRecordViewModel extends FoxSdkBaseMviViewModel<FSRechargeRecordViewState, FSRechargeRecordIntent, FoxSdkUiEffect> {

    private FSRechargeRecordRepository repository;
    private int currentPage = 1;
    private boolean hasMoreData = true;

    public FSRechargeRecordViewModel(FSRechargeRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    protected FSRechargeRecordViewState initialState() {
        return new FSRechargeRecordViewState();
    }

    @Override
    protected void handleIntent(FSRechargeRecordIntent intent) {
        if (intent instanceof FSRechargeRecordIntent.LoadInitial) {
            loadRechargeRecords(false);
        } else if (intent instanceof FSRechargeRecordIntent.Refresh) {
            loadRechargeRecords();
        } else if (intent instanceof FSRechargeRecordIntent.LoadMore) {
            loadMoreRechargeRecord();
        }
    }

    // 初次请求or刷新
    private void loadRechargeRecords() {
        loadRechargeRecords(false);
    }

    private void loadRechargeRecords(boolean isInitial) {
        currentPage = 1;
        hasMoreData = true;

        FoxSdkPageRequest pageRequest = new FoxSdkPageRequest(
                currentPage,
                PageConstants.DEFAULT_PAGE_SIZE,
                !isInitial,
                isInitial,
                Collections.emptyMap()
        );

        executePageRequestData(
                pageRequest,
                (page, size) -> repository.getRechargeRecords(page, size),
                (data, page, hasMore, totalCount) -> {
                    FSRechargeRecordViewState newState = getCurrentState();
                    newState.setRechargeRecordList(data.getData() != null ? data.getData() : Collections.emptyList());
                    newState.setLoading(false);
                    newState.setRefreshing(false);
                    newState.setError(null);
                    newState.setHasMore(hasMore);
                    newState.setInit(false);
                    newState.setTotalCount(totalCount);
                    setState(newState);

                    currentPage = page;
                    hasMoreData = hasMore;
                },
                (error, page, code) -> {
                    FSRechargeRecordViewState newState = getCurrentState();
                    newState.setLoading(false);
                    newState.setRefreshing(false);
                    newState.setInit(false);
                    newState.setError(error);
                    setState(newState);

                    sendEffect(new FoxSdkUiEffect.ShowToast(error));
                },
                (isInitialLoading, page) -> {
                    if (isInitialLoading) {
                        FSRechargeRecordViewState newState = getCurrentState();
                        newState.setLoading(true);
                        newState.setError(null);
                        newState.setInit(false);
                        setState(newState);
                    } else {
                        FSRechargeRecordViewState newState = getCurrentState();
                        newState.setLoading(true);
                        newState.setInit(false);
                        newState.setRefreshing(true);
                        newState.setError(null);
                        setState(newState);
                    }
                }
        );
    }

    // 下一页
    private void loadMoreRechargeRecord() {
        if (!hasMoreData || getCurrentState().isLoadingMore()) {
            return;
        }

        int nextPage = currentPage + 1;

        FoxSdkPageRequest pageRequest = new FoxSdkPageRequest(
                nextPage,
                PageConstants.DEFAULT_PAGE_SIZE,
                false,
                false,
                Collections.emptyMap()
        );

        executePageRequestData(
                pageRequest,
                (page, size) -> repository.getRechargeRecords(page, size),
                (data, page, hasMore, totalCount) -> {
                    FSRechargeRecordViewState newState = getCurrentState();
                    newState.setMoreRechargeRecordList(data.getData() != null ? data.getData() : Collections.emptyList());
                    newState.setRechargeRecordList(null);
                    newState.setLoadingMore(false);
                    newState.setError(null);
                    newState.setInit(false);
                    newState.setHasMore(hasMore);
                    setState(newState);

                    currentPage = page;
                    hasMoreData = hasMore;
                },
                (error, page, code) -> {
                    FSRechargeRecordViewState newState = getCurrentState();
                    newState.setLoadingMore(false);
                    newState.setRechargeRecordList(null);
                    newState.setInit(false);
                    newState.setError(error);
                    setState(newState);

                    sendEffect(new FoxSdkUiEffect.ShowToast(error));
                },
                (isInitial, page) -> {
                    FSRechargeRecordViewState newState = getCurrentState();
                    newState.setLoadingMore(true);
                    newState.setRechargeRecordList(null);
                    newState.setInit(false);
                    setState(newState);
                }
        );
    }
}
