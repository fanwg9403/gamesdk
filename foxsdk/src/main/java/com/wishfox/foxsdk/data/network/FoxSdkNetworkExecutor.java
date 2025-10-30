package com.wishfox.foxsdk.data.network;

import com.wishfox.foxsdk.data.model.entity.FSPageContainer;
import com.wishfox.foxsdk.data.model.FoxSdkBaseResponse;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import retrofit2.adapter.rxjava3.HttpException;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:47
 */
public class FoxSdkNetworkExecutor {

    public static <T> Single<FoxSdkNetworkResult<T>> execute(
            FoxSdkApiCall<FoxSdkBaseResponse<T>> call) {
        return Single.fromCallable(() -> {
            try {
                FoxSdkBaseResponse<T> response = call.execute();

                if (response.isSuccess() && response.getData() != null) {
                    return FoxSdkNetworkResult.success(response.getData(), response.getMsg());
                } else if (response.isSuccess() && response.getData() == null) {
                    return FoxSdkNetworkResult.empty(response.getMsg() != null ? response.getMsg() : "数据为空", response.getCode());
                } else {
                    return FoxSdkNetworkResult.error(
                            response.getMsg() != null ? response.getMsg() : "请求失败",
                            response.getCode()
                    );
                }
            } catch (Exception e) {
                // 这里需要显式转换类型
                @SuppressWarnings("unchecked")
                FoxSdkNetworkResult<T> result = (FoxSdkNetworkResult<T>) handleException(e);
                return result;
            }
        });
    }

    public static <T> Observable<FoxSdkNetworkResult<T>> executeAsObservable(
            FoxSdkApiCall<FoxSdkBaseResponse<T>> call) {
        return Observable.create(emitter -> {
            try {
                FoxSdkBaseResponse<T> response = call.execute();

                if (response.isSuccess() && response.getData() != null) {
                    emitter.onNext(FoxSdkNetworkResult.success(response.getData(), response.getMsg()));
                } else if (response.isSuccess() && response.getData() == null) {
                    emitter.onNext(FoxSdkNetworkResult.empty(response.getMsg() != null ? response.getMsg() : "数据为空"));
                } else {
                    emitter.onNext(FoxSdkNetworkResult.error(
                            response.getMsg() != null ? response.getMsg() : "请求失败",
                            response.getCode()
                    ));
                }
                emitter.onComplete();
            } catch (Exception e) {
                // 使用专门的错误处理方法
                FoxSdkNetworkResult<T> errorResult = handleExceptionForType(e);
                emitter.onNext(errorResult);
                emitter.onComplete();
            }
        });
    }

    public static <T> Observable<FoxSdkNetworkResult<T>> executePage(
            FoxSdkPageRequest pageRequest,
            FoxSdkPageApiCall<T> apiCall) {
        return Observable.create(emitter -> {
            try {
                emitter.onNext(FoxSdkNetworkResult.pageLoading(pageRequest.isInitial(), pageRequest.getPage()));

                FoxSdkBaseResponse<T> response = apiCall.execute(pageRequest.getPage(), pageRequest.getPageSize());

                if (response.isSuccess() && response.getData() != null) {
                    T data = response.getData();
                    boolean hasMore = determineHasMore(data, pageRequest.getPageSize());

                    emitter.onNext(FoxSdkNetworkResult.pageSuccess(
                            data,
                            pageRequest.getPage(),
                            hasMore,
                            response.getTotal()
                    ));
                } else if (response.isSuccess() && response.getData() == null) {
                    emitter.onNext(FoxSdkNetworkResult.empty("暂无数据"));
                } else {
                    emitter.onNext(FoxSdkNetworkResult.pageError(
                            response.getMsg() != null ? response.getMsg() : "请求失败",
                            pageRequest.getPage(),
                            response.getCode()
                    ));
                }
                emitter.onComplete();
            } catch (Exception e) {
                // 显式转换类型
                @SuppressWarnings("unchecked")
                FoxSdkNetworkResult<T> errorResult = (FoxSdkNetworkResult<T>) handlePageException(e, pageRequest.getPage());
                emitter.onNext(errorResult);
                emitter.onComplete();
            }
        });
    }

    public static <T> Observable<FoxSdkNetworkResult<T>> executePageData(
            FoxSdkPageRequest pageRequest,
            FoxSdkPageApiCall<T> apiCall) {
        return Observable.create(emitter -> {
            try {
                emitter.onNext(FoxSdkNetworkResult.pageLoading(pageRequest.isInitial(), pageRequest.getPage()));

                FoxSdkBaseResponse<T> response = apiCall.execute(pageRequest.getPage(), pageRequest.getPageSize());

                if (response.isSuccess() && response.getData() != null) {
                    T data = response.getData();
                    boolean hasMore = determineHasMoreForData(data, pageRequest.getPageSize());

                    emitter.onNext(FoxSdkNetworkResult.pageSuccess(
                            data,
                            pageRequest.getPage(),
                            hasMore,
                            response.getTotal()
                    ));
                } else if (response.isSuccess() && response.getData() == null) {
                    emitter.onNext(FoxSdkNetworkResult.empty("暂无数据"));
                } else {
                    emitter.onNext(FoxSdkNetworkResult.pageError(
                            response.getMsg() != null ? response.getMsg() : "请求失败",
                            pageRequest.getPage(),
                            response.getCode()
                    ));
                }
                emitter.onComplete();
            } catch (Exception e) {
                // 显式转换类型
                @SuppressWarnings("unchecked")
                FoxSdkNetworkResult<T> errorResult = (FoxSdkNetworkResult<T>) handlePageException(e, pageRequest.getPage());
                emitter.onNext(errorResult);
                emitter.onComplete();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean determineHasMore(T data, int pageSize) {
        if (data instanceof java.util.List) {
            return ((java.util.List<?>) data).size() >= pageSize;
        }
        // 对于非列表数据，默认认为还有更多数据
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean determineHasMoreForData(T data, int pageSize) {
        // 特殊处理分页容器类型
        if (data instanceof FSPageContainer) {
            FSPageContainer<?> pageContainer =
                    (FSPageContainer<?>) data;
            return (pageContainer.getLastPage() != null ? pageContainer.getLastPage() : 0) >
                    (pageContainer.getCurrentPage() != null ? pageContainer.getCurrentPage() : 0);
        } else if (data instanceof java.util.List) {
            return ((java.util.List<?>) data).size() >= pageSize;
        }
        // 对于其他类型数据，默认认为还有更多数据
        return true;
    }

    /**
     * 处理异常 - 返回原始类型，由调用方进行类型转换
     */
    private static FoxSdkNetworkResult<?> handleException(Throwable e) {
        if (e instanceof IOException || e instanceof SocketTimeoutException ||
                e instanceof UnknownHostException || e instanceof ConnectException) {
            return FoxSdkNetworkResult.error("网络连接失败，请检查网络设置", -1, e);
        } else if (e instanceof HttpException) {
            return FoxSdkNetworkResult.error("网络连接失败，请检查网络设置", -1, e);
        } else if (e instanceof com.google.gson.JsonParseException ||
                e instanceof org.json.JSONException) {
            return FoxSdkNetworkResult.error("解析错误，请稍后再试", -1, e);
        } else if (e instanceof TimeoutException) {
            return FoxSdkNetworkResult.error("网络连接超时，请稍后重试", -1, e);
        } else {
            return FoxSdkNetworkResult.error("请求失败: " + e.getMessage(), -2, e);
        }
    }

    /**
     * 处理分页异常 - 返回原始类型，由调用方进行类型转换
     */
    private static FoxSdkNetworkResult<?> handlePageException(Throwable e, int page) {
        String errorMessage;
        if (e instanceof IOException || e instanceof SocketTimeoutException ||
                e instanceof UnknownHostException || e instanceof ConnectException) {
            errorMessage = "网络连接失败";
        } else if (e instanceof HttpException) {
            errorMessage = e.getMessage() != null ? e.getMessage() : "网络连接失败，请检查网络设置";
        } else if (e instanceof com.google.gson.JsonParseException ||
                e instanceof org.json.JSONException) {
            errorMessage = e.getMessage() != null ? e.getMessage() : "解析错误，请稍后再试";
        } else if (e instanceof TimeoutException) {
            errorMessage = e.getMessage() != null ? e.getMessage() : "网络连接超时，请稍后重试";
        } else {
            errorMessage = "加载失败: " + e.getMessage();
        }

        return FoxSdkNetworkResult.pageError(errorMessage, page, -1);
    }

    /**
     * 专门为具体类型处理异常的方法
     */
    private static <T> FoxSdkNetworkResult<T> handleExceptionForType(Throwable e) {
        String errorMessage;
        int errorCode = -1;

        if (e instanceof IOException || e instanceof SocketTimeoutException ||
                e instanceof UnknownHostException || e instanceof ConnectException) {
            errorMessage = "网络连接失败，请检查网络设置";
        } else if (e instanceof HttpException) {
            errorMessage = "网络连接失败，请检查网络设置";
        } else if (e instanceof com.google.gson.JsonParseException ||
                e instanceof org.json.JSONException) {
            errorMessage = "解析错误，请稍后再试";
        } else if (e instanceof TimeoutException) {
            errorMessage = "网络连接超时，请稍后重试";
        } else {
            errorMessage = "请求失败: " + e.getMessage();
            errorCode = -2;
        }

        return FoxSdkNetworkResult.error(errorMessage, errorCode, e);
    }

    public interface FoxSdkApiCall<T> {
        T execute() throws Exception;
    }

    public interface FoxSdkPageApiCall<T> {
        FoxSdkBaseResponse<T> execute(int page, int pageSize) throws Exception;
    }
}
