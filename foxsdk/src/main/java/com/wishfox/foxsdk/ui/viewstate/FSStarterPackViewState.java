package com.wishfox.foxsdk.ui.viewstate;

import com.wishfox.foxsdk.data.model.entity.FSStarterPack;

import java.util.List;
import java.util.Objects;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 19:00
 */
public class FSStarterPackViewState implements FoxSdkViewState {
    private List<FSStarterPack> starterPackList;
    private List<FSStarterPack> moreStarterPackList;
    private boolean isLoading;
    private boolean isLoadingMore;
    private boolean isRefreshing;
    private String error;
    private boolean hasMore;
    private Integer totalCount;
    private boolean isStateViewEnable;
    private boolean isInit;

    public FSStarterPackViewState() {
        this.starterPackList = java.util.Collections.emptyList();
        this.moreStarterPackList = java.util.Collections.emptyList();
        this.isLoading = false;
        this.isLoadingMore = false;
        this.isRefreshing = false;
        this.error = null;
        this.hasMore = false;
        this.totalCount = null;
        this.isStateViewEnable = false;
        this.isInit = true;
    }

    public FSStarterPackViewState(List<FSStarterPack> starterPackList, List<FSStarterPack> moreStarterPackList,
                                  boolean isLoading, boolean isLoadingMore, boolean isRefreshing,
                                  String error, boolean hasMore, Integer totalCount,
                                  boolean isStateViewEnable, boolean isInit) {
        this.starterPackList = starterPackList;
        this.moreStarterPackList = moreStarterPackList;
        this.isLoading = isLoading;
        this.isLoadingMore = isLoadingMore;
        this.isRefreshing = isRefreshing;
        this.error = error;
        this.hasMore = hasMore;
        this.totalCount = totalCount;
        this.isStateViewEnable = isStateViewEnable;
        this.isInit = isInit;
    }

    // Getters and Setters
    public List<FSStarterPack> getStarterPackList() { return starterPackList; }
    public void setStarterPackList(List<FSStarterPack> starterPackList) { this.starterPackList = starterPackList; }

    public List<FSStarterPack> getMoreStarterPackList() { return moreStarterPackList; }
    public void setMoreStarterPackList(List<FSStarterPack> moreStarterPackList) { this.moreStarterPackList = moreStarterPackList; }

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

    public boolean isStateViewEnable() { return isStateViewEnable; }
    public void setStateViewEnable(boolean stateViewEnable) { isStateViewEnable = stateViewEnable; }

    public boolean isInit() { return isInit; }
    public void setInit(boolean init) { isInit = init; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FSStarterPackViewState that = (FSStarterPackViewState) o;
        return isLoading == that.isLoading &&
                isLoadingMore == that.isLoadingMore &&
                isRefreshing == that.isRefreshing &&
                hasMore == that.hasMore &&
                isStateViewEnable == that.isStateViewEnable &&
                isInit == that.isInit &&
                Objects.equals(starterPackList, that.starterPackList) &&
                Objects.equals(moreStarterPackList, that.moreStarterPackList) &&
                Objects.equals(error, that.error) &&
                Objects.equals(totalCount, that.totalCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(starterPackList, moreStarterPackList, isLoading, isLoadingMore,
                isRefreshing, error, hasMore, totalCount, isStateViewEnable, isInit);
    }

    @Override
    public String toString() {
        return "FSStarterPackViewState{" +
                "starterPackList=" + starterPackList +
                ", moreStarterPackList=" + moreStarterPackList +
                ", isLoading=" + isLoading +
                ", isLoadingMore=" + isLoadingMore +
                ", isRefreshing=" + isRefreshing +
                ", error='" + error + '\'' +
                ", hasMore=" + hasMore +
                ", totalCount=" + totalCount +
                ", isStateViewEnable=" + isStateViewEnable +
                ", isInit=" + isInit +
                '}';
    }
}
