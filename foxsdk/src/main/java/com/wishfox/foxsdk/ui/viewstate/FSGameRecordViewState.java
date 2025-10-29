package com.wishfox.foxsdk.ui.viewstate;

import com.wishfox.foxsdk.data.model.entity.FSGameRecord;

import java.util.List;
import java.util.Objects;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:58
 */
public class FSGameRecordViewState implements FoxSdkViewState {
    private List<FSGameRecord> gameRecordList;
    private List<FSGameRecord> moreGameRecordList;
    private boolean isLoading;
    private boolean isLoadingMore;
    private boolean isRefreshing;
    private String error;
    private boolean hasMore;
    private Integer totalCount;
    private boolean isInit;

    public FSGameRecordViewState() {
        this.gameRecordList = java.util.Collections.emptyList();
        this.moreGameRecordList = java.util.Collections.emptyList();
        this.isLoading = false;
        this.isLoadingMore = false;
        this.isRefreshing = false;
        this.error = null;
        this.hasMore = false;
        this.totalCount = null;
        this.isInit = true;
    }

    public FSGameRecordViewState(List<FSGameRecord> gameRecordList, List<FSGameRecord> moreGameRecordList,
                                 boolean isLoading, boolean isLoadingMore, boolean isRefreshing,
                                 String error, boolean hasMore, Integer totalCount, boolean isInit) {
        this.gameRecordList = gameRecordList;
        this.moreGameRecordList = moreGameRecordList;
        this.isLoading = isLoading;
        this.isLoadingMore = isLoadingMore;
        this.isRefreshing = isRefreshing;
        this.error = error;
        this.hasMore = hasMore;
        this.totalCount = totalCount;
        this.isInit = isInit;
    }

    // Getters and Setters
    public List<FSGameRecord> getGameRecordList() { return gameRecordList; }
    public void setGameRecordList(List<FSGameRecord> gameRecordList) { this.gameRecordList = gameRecordList; }

    public List<FSGameRecord> getMoreGameRecordList() { return moreGameRecordList; }
    public void setMoreGameRecordList(List<FSGameRecord> moreGameRecordList) { this.moreGameRecordList = moreGameRecordList; }

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public boolean isLoadingMore() { return isLoadingMore; }
    public void setLoadingMore(boolean loadingMore) { isLoadingMore = loadingMore; }

    public boolean isRefreshing() { return isRefreshing; }
    public void setRefreshing(boolean refreshing) { isRefreshing = refreshing; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public boolean isInit() { return isInit; }
    public void setInit(boolean init) { isInit = init; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FSGameRecordViewState that = (FSGameRecordViewState) o;
        return isLoading == that.isLoading &&
                isLoadingMore == that.isLoadingMore &&
                isRefreshing == that.isRefreshing &&
                hasMore == that.hasMore &&
                isInit == that.isInit &&
                Objects.equals(gameRecordList, that.gameRecordList) &&
                Objects.equals(moreGameRecordList, that.moreGameRecordList) &&
                Objects.equals(error, that.error) &&
                Objects.equals(totalCount, that.totalCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameRecordList, moreGameRecordList, isLoading, isLoadingMore,
                isRefreshing, error, hasMore, totalCount, isInit);
    }

    @Override
    public String toString() {
        return "FSGameRecordViewState{" +
                "gameRecordList=" + gameRecordList +
                ", moreGameRecordList=" + moreGameRecordList +
                ", isLoading=" + isLoading +
                ", isLoadingMore=" + isLoadingMore +
                ", isRefreshing=" + isRefreshing +
                ", error='" + error + '\'' +
                ", hasMore=" + hasMore +
                ", totalCount=" + totalCount +
                ", isInit=" + isInit +
                '}';
    }
}
