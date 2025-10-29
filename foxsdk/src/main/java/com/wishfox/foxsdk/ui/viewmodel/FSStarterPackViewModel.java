package com.wishfox.foxsdk.ui.viewmodel;

import com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest;
import com.wishfox.foxsdk.data.model.paging.PageConstants;
import com.wishfox.foxsdk.data.repository.FSStarterPackRepository;
import com.wishfox.foxsdk.domain.intent.FSStarterPackIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSStarterPackViewState;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;

import java.util.Collections;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 20:10
 */
public class FSStarterPackViewModel extends FoxSdkBaseMviViewModel<FSStarterPackViewState, FSStarterPackIntent, FoxSdkUiEffect> {

    private FSStarterPackRepository starterPackRepository;
    private int currentPage = 1;
    private boolean hasMoreData = true;
    private int clickPosition = -1;

    public FSStarterPackViewModel(FSStarterPackRepository starterPackRepository) {
        this.starterPackRepository = starterPackRepository;
    }

    @Override
    protected FSStarterPackViewState initialState() {
        return new FSStarterPackViewState();
    }

    @Override
    protected void handleIntent(FSStarterPackIntent intent) {
        if (intent instanceof FSStarterPackIntent.LoadInitial) {
            loadStarterPacks();
        } else if (intent instanceof FSStarterPackIntent.Refresh) {
            loadStarterPacks();
        } else if (intent instanceof FSStarterPackIntent.LoadMore) {
            loadMoreStarterPack();
        } else if (intent instanceof FSStarterPackIntent.ReceiveStarterPack) {
            receiveStarterPack(((FSStarterPackIntent.ReceiveStarterPack) intent).getMailId());
        }
    }

    // 初次请求or刷新
    private void loadStarterPacks() {
        loadStarterPacks(false);
    }

    private void loadStarterPacks(boolean isInitial) {
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
                (page, size) -> starterPackRepository.getStarterPackList(page, size),
                (data, page, hasMore, totalCount) -> {
                    FSStarterPackViewState newState = getCurrentState();
                    newState.setStarterPackList(data.getData() != null ? data.getData() : Collections.emptyList());
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
                    FSStarterPackViewState newState = getCurrentState();
                    newState.setLoading(false);
                    newState.setRefreshing(false);
                    newState.setInit(false);
                    newState.setError(error);
                    setState(newState);

                    sendEffect(new FoxSdkUiEffect.ShowToast(error));
                },
                (isInitialLoading, page) -> {
                    if (isInitialLoading) {
                        FSStarterPackViewState newState = getCurrentState();
                        newState.setLoading(true);
                        newState.setInit(false);
                        newState.setError(null);
                        setState(newState);
                    } else {
                        FSStarterPackViewState newState = getCurrentState();
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
    private void loadMoreStarterPack() {
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
                (page, size) -> starterPackRepository.getStarterPackList(page, size),
                (data, page, hasMore, totalCount) -> {
                    FSStarterPackViewState newState = getCurrentState();
                    newState.setMoreStarterPackList(data.getData() != null ? data.getData() : Collections.emptyList());
                    newState.setStarterPackList(null);
                    newState.setLoadingMore(false);
                    newState.setError(null);
                    newState.setInit(false);
                    newState.setHasMore(hasMore);
                    setState(newState);

                    currentPage = page;
                    hasMoreData = hasMore;
                },
                (error, page, code) -> {
                    FSStarterPackViewState newState = getCurrentState();
                    newState.setLoadingMore(false);
                    newState.setStarterPackList(null);
                    newState.setInit(false);
                    newState.setError(error);
                    setState(newState);

                    sendEffect(new FoxSdkUiEffect.ShowToast(error));
                },
                (isInitial, page) -> {
                    FSStarterPackViewState newState = getCurrentState();
                    newState.setLoadingMore(true);
                    newState.setStarterPackList(null);
                    newState.setInit(false);
                    setState(newState);
                }
        );
    }

    // 领取新手礼包
    private void receiveStarterPack(String mailId) {
        // 使用新的重载方法，直接传递 Single<FoxSdkNetworkResult<Object>>
        executeNetworkRequest(
                starterPackRepository.receiveStarterPack(mailId),
                result -> {
                    FSStarterPackViewState newState = getCurrentState();
                    newState.setStateViewEnable(true);
                    setState(newState);
                },
                (error, code) -> {
                    sendEffect(new FoxSdkUiEffect.ShowToast(error));
                }
        );
    }

    // 修改领取状态
    public void modifyReceiveState() {
        FSStarterPackViewState newState = getCurrentState();
        newState.setStateViewEnable(false);
        setState(newState);
    }

    public int getClickPosition() {
        return clickPosition;
    }

    public void setClickPosition(int clickPosition) {
        this.clickPosition = clickPosition;
    }
}
