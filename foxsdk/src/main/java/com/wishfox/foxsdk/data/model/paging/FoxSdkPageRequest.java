package com.wishfox.foxsdk.data.model.paging;

import java.util.HashMap;
import java.util.Map;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:24
 */
public class FoxSdkPageRequest {

    private final int page;
    private final int pageSize;
    private final boolean isRefresh;
    private final boolean isInitial;
    private final Map<String, Object> parameters;

    public FoxSdkPageRequest() {
        this(1, 20, false, false, new HashMap<>());
    }

    public FoxSdkPageRequest(int page, int pageSize, boolean isRefresh, boolean isInitial,
                             Map<String, Object> parameters) {
        this.page = page;
        this.pageSize = pageSize;
        this.isRefresh = isRefresh;
        this.isInitial = isInitial;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    /**
     * 创建下一页请求
     */
    public FoxSdkPageRequest nextPage() {
        return new FoxSdkPageRequest(page + 1, pageSize, false, isInitial, parameters);
    }

    /**
     * 创建刷新请求（回到第一页）
     */
    public FoxSdkPageRequest refresh() {
        return new FoxSdkPageRequest(1, pageSize, true, isInitial, parameters);
    }

    /**
     * 添加额外参数
     */
    public FoxSdkPageRequest withParameter(String key, Object value) {
        Map<String, Object> newParameters = new HashMap<>(parameters);
        newParameters.put(key, value);
        return new FoxSdkPageRequest(page, pageSize, isRefresh, isInitial, newParameters);
    }

    /**
     * 获取参数值
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * 获取字符串参数
     */
    public String getStringParameter(String key) {
        Object value = parameters.get(key);
        return value instanceof String ? (String) value : null;
    }

    /**
     * 获取整型参数
     */
    public Integer getIntParameter(String key) {
        Object value = parameters.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // Getter方法
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public boolean isRefresh() { return isRefresh; }
    public boolean isInitial() { return isInitial; }
    public Map<String, Object> getParameters() { return new HashMap<>(parameters); }

    @Override
    public String toString() {
        return "FoxSdkPageRequest{" +
                "page=" + page +
                ", pageSize=" + pageSize +
                ", isRefresh=" + isRefresh +
                ", isInitial=" + isInitial +
                ", parameters=" + parameters +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FoxSdkPageRequest that = (FoxSdkPageRequest) o;

        if (page != that.page) return false;
        if (pageSize != that.pageSize) return false;
        if (isRefresh != that.isRefresh) return false;
        if (isInitial != that.isInitial) return false;
        return parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        int result = page;
        result = 31 * result + pageSize;
        result = 31 * result + (isRefresh ? 1 : 0);
        result = 31 * result + (isInitial ? 1 : 0);
        result = 31 * result + parameters.hashCode();
        return result;
    }
}
