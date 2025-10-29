package com.wishfox.foxsdk.data.model.entity;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:55
 */
public class FSPageContainer<T> {
    @SerializedName("current_page")
    private Integer currentPage;

    private ArrayList<T> data;

    @SerializedName("last_page")
    private Integer lastPage;

    @SerializedName("per_page")
    private Integer perPage;

    /**
     * 总数据量
     */
    private Integer total = 0;

    public FSPageContainer() {}

    public FSPageContainer(Integer currentPage, ArrayList<T> data, Integer lastPage,
                           Integer perPage, Integer total) {
        this.currentPage = currentPage;
        this.data = data;
        this.lastPage = lastPage;
        this.perPage = perPage;
        this.total = total;
    }

    // Getter和Setter方法
    public Integer getCurrentPage() { return currentPage; }
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }

    public ArrayList<T> getData() { return data; }
    public void setData(ArrayList<T> data) { this.data = data; }

    public Integer getLastPage() { return lastPage; }
    public void setLastPage(Integer lastPage) { this.lastPage = lastPage; }

    public Integer getPerPage() { return perPage; }
    public void setPerPage(Integer perPage) { this.perPage = perPage; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}
