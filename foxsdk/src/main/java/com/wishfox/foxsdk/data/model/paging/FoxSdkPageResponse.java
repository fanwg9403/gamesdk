package com.wishfox.foxsdk.data.model.paging;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:27
 */
public class FoxSdkPageResponse<T> {
    private final List<T> items;
    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final boolean hasNextPage;

    public FoxSdkPageResponse(List<T> items, int currentPage, int pageSize,
                              int totalCount, int totalPages, boolean hasNextPage) {
        this.items = items;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.hasNextPage = hasNextPage;
    }

    public FoxSdkPageResponse(List<T> items, int totalCount) {
        this(items, PageConstants.FIRST_PAGE, PageConstants.DEFAULT_PAGE_SIZE,
                totalCount, calculateTotalPages(totalCount, PageConstants.DEFAULT_PAGE_SIZE),
                hasNextPage(PageConstants.FIRST_PAGE, totalCount, PageConstants.DEFAULT_PAGE_SIZE));
    }

    private static int calculateTotalPages(int totalCount, int pageSize) {
        if (pageSize <= 0) return 0;
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    private static boolean hasNextPage(int currentPage, int totalCount, int pageSize) {
        if (pageSize <= 0) return false;
        return currentPage * pageSize < totalCount;
    }

    // Getter方法
    public List<T> getItems() { return items; }
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }
    public int getTotalCount() { return totalCount; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasNextPage() { return hasNextPage; }

    @Override
    public String toString() {
        return "FoxSdkPageResponse{" +
                "items=" + items +
                ", currentPage=" + currentPage +
                ", pageSize=" + pageSize +
                ", totalCount=" + totalCount +
                ", totalPages=" + totalPages +
                ", hasNextPage=" + hasNextPage +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FoxSdkPageResponse<?> that = (FoxSdkPageResponse<?>) o;

        if (currentPage != that.currentPage) return false;
        if (pageSize != that.pageSize) return false;
        if (totalCount != that.totalCount) return false;
        if (totalPages != that.totalPages) return false;
        if (hasNextPage != that.hasNextPage) return false;
        return items != null ? items.equals(that.items) : that.items == null;
    }

    @Override
    public int hashCode() {
        int result = items != null ? items.hashCode() : 0;
        result = 31 * result + currentPage;
        result = 31 * result + pageSize;
        result = 31 * result + totalCount;
        result = 31 * result + totalPages;
        result = 31 * result + (hasNextPage ? 1 : 0);
        return result;
    }
}
