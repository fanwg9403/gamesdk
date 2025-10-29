package com.wishfox.foxsdk.ui.viewmodel;

import com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest;
import com.wishfox.foxsdk.data.model.paging.PageConstants;
import com.wishfox.foxsdk.data.repository.FSWinFoxCoinRepository;
import com.wishfox.foxsdk.domain.intent.FSWinFoxCoinIntent;
import com.wishfox.foxsdk.ui.base.FoxSdkBaseMviViewModel;
import com.wishfox.foxsdk.ui.viewstate.FSWinFoxCoinViewState;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;

import java.util.Collections;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 19:59
 */
public class FSWinFoxCoinViewModel extends FoxSdkBaseMviViewModel<FSWinFoxCoinViewState, FSWinFoxCoinIntent, FoxSdkUiEffect> {

    private FSWinFoxCoinRepository winFoxCoinRepository;
    private int currentPage = 1;
    private boolean hasMoreData = true;

    public FSWinFoxCoinViewModel(FSWinFoxCoinRepository winFoxCoinRepository) {
        this.winFoxCoinRepository = winFoxCoinRepository;
    }

    @Override
    protected FSWinFoxCoinViewState initialState() {
        return new FSWinFoxCoinViewState();
    }

    @Override
    protected void handleIntent(FSWinFoxCoinIntent intent) {
        if (intent instanceof FSWinFoxCoinIntent.LoadInitial) {
            loadWinFoxCoins();
        } else if (intent instanceof FSWinFoxCoinIntent.Refresh) {
            loadWinFoxCoins();
        } else if (intent instanceof FSWinFoxCoinIntent.LoadMore) {
            loadMoreWinFoxCoin();
        }
    }

    // 初次请求or刷新
    private void loadWinFoxCoins() {
        loadWinFoxCoins(false);
    }

    private void loadWinFoxCoins(boolean isInitial) {
        currentPage = 1;
        hasMoreData = true;

        FoxSdkPageRequest pageRequest = new FoxSdkPageRequest(
                currentPage,
                PageConstants.DEFAULT_PAGE_SIZE,
                !isInitial,
                isInitial,
                Collections.emptyMap()
        );

        // 使用新的 executePageRequest 方法
        executePageRequest(
                pageRequest,
                (page, size) -> winFoxCoinRepository.getWinFoxCoinList(page, size),
                (data, page, hasMore, totalCount) -> {
                    FSWinFoxCoinViewState newState = getCurrentState();
                    newState.setWinFoxCoinList(data != null ? data : Collections.emptyList());
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
                    FSWinFoxCoinViewState newState = getCurrentState();
                    newState.setLoading(false);
                    newState.setRefreshing(false);
                    newState.setInit(false);
                    newState.setError(error);
                    setState(newState);

                    sendEffect(new FoxSdkUiEffect.ShowToast(error));
                },
                (isInitialLoading, page) -> {
                    if (isInitialLoading) {
                        FSWinFoxCoinViewState newState = getCurrentState();
                        newState.setLoading(true);
                        newState.setError(null);
                        newState.setInit(false);
                        setState(newState);
                    } else {
                        FSWinFoxCoinViewState newState = getCurrentState();
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
    private void loadMoreWinFoxCoin() {
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

        // 使用新的 executePageRequest 方法
        executePageRequest(
                pageRequest,
                (page, size) -> winFoxCoinRepository.getWinFoxCoinList(page, size),
                (data, page, hasMore, totalCount) -> {
                    FSWinFoxCoinViewState newState = getCurrentState();
                    newState.setMoreWinFoxCoinList(data != null ? data : Collections.emptyList());
                    newState.setWinFoxCoinList(null);
                    newState.setLoadingMore(false);
                    newState.setError(null);
                    newState.setInit(false);
                    newState.setHasMore(hasMore);
                    setState(newState);

                    currentPage = page;
                    hasMoreData = hasMore;
                },
                (error, page, code) -> {
                    FSWinFoxCoinViewState newState = getCurrentState();
                    newState.setLoadingMore(false);
                    newState.setWinFoxCoinList(null);
                    newState.setInit(false);
                    newState.setError(error);
                    setState(newState);

                    sendEffect(new FoxSdkUiEffect.ShowToast(error));
                },
                (isInitial, page) -> {
                    FSWinFoxCoinViewState newState = getCurrentState();
                    newState.setLoadingMore(true);
                    newState.setWinFoxCoinList(null);
                    newState.setInit(false);
                    setState(newState);
                }
        );
    }
}
