package com.wishfox.foxsdk.ui.viewstate;

import com.wishfox.foxsdk.data.model.entity.FSStarterPack;

import java.util.List;
import java.util.Objects;

/**
 * 主要功能:
 */
public class FSAllinpaysdkAlipayViewState implements FoxSdkViewState {

    private boolean isLoading;
    private boolean isLoadingMore;
    private boolean isRefreshing;
    private String error;
    private boolean hasMore;
    private Integer totalCount;
    private boolean isStateViewEnable;
    private boolean isInit;
    public FSAllinpaysdkAlipayViewState() {
        this.isLoading = false;
        this.isLoadingMore = false;
        this.isRefreshing = false;
        this.error = null;
        this.hasMore = false;
        this.totalCount = null;
        this.isStateViewEnable = false;
        this.isInit = true;
    }
}
