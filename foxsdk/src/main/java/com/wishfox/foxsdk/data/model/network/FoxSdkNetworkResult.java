package com.wishfox.foxsdk.data.model.network;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:48
 */
public class FoxSdkNetworkResult<T> {
    private final ResultType type;
    private final T data;
    private final String message;
    private final String error;
    private final int code;
    private final Throwable throwable;
    private final int page;
    private final boolean hasMore;
    private final boolean isInitial;
    private final Integer totalCount;

    private FoxSdkNetworkResult(ResultType type, T data, String message, String error,
                                int code, Throwable throwable, int page, boolean hasMore,
                                boolean isInitial, Integer totalCount) {
        this.type = type;
        this.data = data;
        this.message = message;
        this.error = error;
        this.code = code;
        this.throwable = throwable;
        this.page = page;
        this.hasMore = hasMore;
        this.isInitial = isInitial;
        this.totalCount = totalCount;
    }

    // Static factory methods
    public static <T> FoxSdkNetworkResult<T> success(T data, String message) {
        return new FoxSdkNetworkResult<>(ResultType.SUCCESS, data, message, null, 0, null, 0, false, false, null);
    }

    public static <T> FoxSdkNetworkResult<T> error(String error, int code, Throwable throwable) {
        return new FoxSdkNetworkResult<>(ResultType.ERROR, null, null, error, code, throwable, 0, false, false, null);
    }

    public static <T> FoxSdkNetworkResult<T> error(String error, int code) {
        return error(error, code, null);
    }

    public static <T> FoxSdkNetworkResult<T> empty(String message) {
        return new FoxSdkNetworkResult<>(ResultType.EMPTY, null, message, null, 0, null, 0, false, false, null);
    }

    public static <T> FoxSdkNetworkResult<T> empty(String message, int code) {
        return new FoxSdkNetworkResult<>(ResultType.EMPTY, null, message, null, code, null, 0, false, false, null);
    }

    public static <T> FoxSdkNetworkResult<T> pageLoading(boolean isInitial, int page) {
        return new FoxSdkNetworkResult<>(ResultType.PAGE_LOADING, null, null, null, 0, null, page, false, isInitial, null);
    }

    public static <T> FoxSdkNetworkResult<T> pageSuccess(T data, int page, boolean hasMore, Integer totalCount) {
        return new FoxSdkNetworkResult<>(ResultType.PAGE_SUCCESS, data, null, null, 0, null, page, hasMore, false, totalCount);
    }

    public static <T> FoxSdkNetworkResult<T> pageError(String error, int page, int code) {
        return new FoxSdkNetworkResult<>(ResultType.PAGE_ERROR, null, null, error, code, null, page, false, false, null);
    }

    // Getters
    public ResultType getType() { return type; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public String getError() { return error; }
    public int getCode() { return code; }
    public Throwable getThrowable() { return throwable; }
    public int getPage() { return page; }
    public boolean isHasMore() { return hasMore; }
    public boolean isInitial() { return isInitial; }
    public Integer getTotalCount() { return totalCount; }

    // Type check methods
    public boolean isSuccess() { return type == ResultType.SUCCESS; }
    public boolean isError() { return type == ResultType.ERROR; }
    public boolean isEmpty() { return type == ResultType.EMPTY; }
    public boolean isPageLoading() { return type == ResultType.PAGE_LOADING; }
    public boolean isPageSuccess() { return type == ResultType.PAGE_SUCCESS; }
    public boolean isPageError() { return type == ResultType.PAGE_ERROR; }

    public enum ResultType {
        SUCCESS, ERROR, EMPTY, PAGE_LOADING, PAGE_SUCCESS, PAGE_ERROR
    }
}
