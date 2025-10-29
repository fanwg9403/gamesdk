package com.wishfox.foxsdk.data.model.paging;

import java.util.HashMap;
import java.util.Map;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:27
 */
public class PageRequestBuilder {

    private int page = PageConstants.FIRST_PAGE;
    private int pageSize = PageConstants.DEFAULT_PAGE_SIZE;
    private boolean isRefresh = false;
    private boolean isInitial = false;
    private Map<String, Object> parameters = new HashMap<>();

    public PageRequestBuilder page(int page) {
        this.page = page;
        return this;
    }

    public PageRequestBuilder pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PageRequestBuilder isRefresh(boolean isRefresh) {
        this.isRefresh = isRefresh;
        return this;
    }

    public PageRequestBuilder isInitial(boolean isInitial) {
        this.isInitial = isInitial;
        return this;
    }

    public PageRequestBuilder parameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    public FoxSdkPageRequest build() {
        return new FoxSdkPageRequest(page, pageSize, isRefresh, isInitial, parameters);
    }
}
