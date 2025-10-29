package com.wishfox.foxsdk.ui.viewmodel;

import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest;
import com.wishfox.foxsdk.data.repository.FSGameRecordRepository;
import com.wishfox.foxsdk.domain.intent.FSGameRecordIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSGameRecordViewState;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;

import java.util.Collections;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:49
 */
public class FSGameRecordViewModel extends FoxSdkBaseMviViewModel<FSGameRecordViewState, FSGameRecordIntent, FoxSdkUiEffect> {

    private FSGameRecordRepository gameRecordRepository;

    private int currentPage = 1;
    private boolean hasMoreData = true;

    public FSGameRecordViewModel(FSGameRecordRepository gameRecordRepository) {
        this.gameRecordRepository = gameRecordRepository;
    }

    @Override
    protected FSGameRecordViewState initialState() {
        return new FSGameRecordViewState();
    }

    @Override
    protected void handleIntent(FSGameRecordIntent intent) {
        if (intent instanceof FSGameRecordIntent.LoadInitial) {
            loadGameRecords(false);
        } else if (intent instanceof FSGameRecordIntent.Refresh) {
            loadGameRecords();
        }
    }

    // 初次请求or刷新
    private void loadGameRecords() {
        loadGameRecords(false);
    }

    private void loadGameRecords(boolean isInitial) {
        currentPage = 1;
        hasMoreData = true;

        FoxSdkPageRequest pageRequest = new FoxSdkPageRequest(
                currentPage,
                50,
                !isInitial,
                isInitial,
                Collections.emptyMap()
        );

        executePageRequestData(
                pageRequest,
                (page, size) -> gameRecordRepository.getGameRecordList(page, size, WishFoxSdk.getConfig().getChannelId(), WishFoxSdk.getConfig().getAppId()),
                (data, page, hasMore, totalCount) -> {
                    FSGameRecordViewState newState = new FSGameRecordViewState();
                    newState.setGameRecordList(data.getData() != null ? data.getData() : java.util.Collections.emptyList());
                    newState.setLoading(false);
                    newState.setRefreshing(false);
                    newState.setError(null);
                    newState.setInit(false);
                    newState.setHasMore(false);
                    newState.setTotalCount(totalCount);
                    setState(newState);

                    currentPage = page;
                    hasMoreData = false;
                },
                (error, page, code) -> {
                    FSGameRecordViewState newState = new FSGameRecordViewState();
                    newState.setLoading(false);
                    newState.setRefreshing(false);
                    newState.setInit(false);
                    newState.setError(error);
                    setState(newState);

                    sendEffect(new FoxSdkUiEffect.ShowToast(error));
                },
                (isInitialLoading, page) -> {
                    if (isInitialLoading) {
                        FSGameRecordViewState newState = getCurrentState();
                        newState.setLoading(true);
                        newState.setInit(false);
                        newState.setError(null);
                        setState(newState);
                    } else {
                        FSGameRecordViewState newState = getCurrentState();
                        newState.setLoading(true);
                        newState.setInit(false);
                        newState.setRefreshing(true);
                        newState.setError(null);
                        setState(newState);
                    }
                }
        );
    }
}
