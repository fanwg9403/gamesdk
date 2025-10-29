package com.wishfox.foxsdk.ui.base;

import androidx.lifecycle.ViewModel;

import com.wishfox.foxsdk.data.model.FoxSdkBaseResponse;
import com.wishfox.foxsdk.data.model.entity.FSPageContainer;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest;
import com.wishfox.foxsdk.data.network.FoxSdkNetworkExecutor;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkUiEffect;
import com.wishfox.foxsdk.domain.intent.FoxSdkViewIntent;
import com.wishfox.foxsdk.ui.viewstate.FoxSdkViewState;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:02
 */
public abstract class FoxSdkBaseMviViewModel<State extends FoxSdkViewState,Intent extends FoxSdkViewIntent,Effect extends FoxSdkUiEffect> extends ViewModel {

    private final BehaviorSubject<State> viewState = BehaviorSubject.create();
    private final PublishSubject<Effect> uiEffect = PublishSubject.create();
    private final BehaviorSubject<LoadingState> loadingState = BehaviorSubject.createDefault(LoadingState.IDLE);

    private final CompositeDisposable disposables = new CompositeDisposable();

    protected abstract State initialState();

    protected abstract void handleIntent(Intent intent);

    public FoxSdkBaseMviViewModel() {
        viewState.onNext(initialState());
    }

    public void dispatch(Intent intent) {
        handleIntent(intent);
    }

    protected void setState(State newState) {
        viewState.onNext(newState);
    }

    public State getCurrentState() {
        return viewState.getValue();
    }

    protected void sendEffect(Effect effect) {
        uiEffect.onNext(effect);
    }

    protected void showLoading(String message) {
        loadingState.onNext(new LoadingState.Show(message));
    }

    protected void dismissLoading() {
        loadingState.onNext(LoadingState.DISMISS);
    }

    public Observable<State> getViewState() {
        return viewState.hide().observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Effect> getUiEffect() {
        return uiEffect.hide().observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<LoadingState> getLoadingState() {
        return loadingState.hide().observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 执行普通网络请求的通用方法（使用 FoxSdkApiCall）
     */
    protected <T> void executeNetworkRequest(
            FoxSdkNetworkExecutor.FoxSdkApiCall<FoxSdkBaseResponse<T>> request,
            boolean showLoading,
            String loadingMessage,
            Consumer<T> onSuccess,
            FoxSdkNetworkErrorConsumer onError
    ) {
        if (showLoading) {
            showLoading(loadingMessage);
        }

        Disposable disposable = FoxSdkNetworkExecutor.execute(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        onSuccess.accept(result.getData());
                        if (showLoading) {
                            dismissLoading();
                        }
                    } else if (result.isError()) {
                        onError.accept(result.getError(), result.getCode());
                        if (showLoading) {
                            dismissLoading();
                        }
                    } else if (result.isEmpty()) {
                        // 处理空数据
                        if (showLoading) {
                            dismissLoading();
                        }
                    }
                }, throwable -> {
                    if (showLoading) {
                        dismissLoading();
                    }
                    onError.accept(throwable.getMessage(), -1);
                });

        disposables.add(disposable);
    }

    /**
     * 执行普通网络请求的通用方法（使用 Single<FoxSdkNetworkResult<T>>）
     */
    protected <T> void executeNetworkRequest(
            Single<FoxSdkNetworkResult<T>> request,
            boolean showLoading,
            String loadingMessage,
            Consumer<T> onSuccess,
            FoxSdkNetworkErrorConsumer onError
    ) {
        if (showLoading) {
            showLoading(loadingMessage);
        }

        Disposable disposable = request
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        onSuccess.accept(result.getData());
                        if (showLoading) {
                            dismissLoading();
                        }
                    } else if (result.isError()) {
                        onError.accept(result.getError(), result.getCode());
                        if (showLoading) {
                            dismissLoading();
                        }
                    } else if (result.isEmpty()) {
                        // 处理空数据
                        if (showLoading) {
                            dismissLoading();
                        }
                    }
                }, throwable -> {
                    if (showLoading) {
                        dismissLoading();
                    }
                    onError.accept(throwable.getMessage(), -1);
                });

        disposables.add(disposable);
    }

    /**
     * 执行普通网络请求的简化方法（使用 FoxSdkApiCall）
     */
    protected <T> void executeNetworkRequest(
            FoxSdkNetworkExecutor.FoxSdkApiCall<FoxSdkBaseResponse<T>> request,
            Consumer<T> onSuccess,
            FoxSdkNetworkErrorConsumer onError
    ) {
        executeNetworkRequest(request, true, "加载中...", onSuccess, onError);
    }

    /**
     * 执行普通网络请求的简化方法（使用 Single<FoxSdkNetworkResult<T>>）
     */
    protected <T> void executeNetworkRequest(
            Single<FoxSdkNetworkResult<T>> request,
            Consumer<T> onSuccess,
            FoxSdkNetworkErrorConsumer onError
    ) {
        executeNetworkRequest(request, true, "加载中...", onSuccess, onError);
    }

    /**
     * 执行分页网络请求（返回 List<T>）
     * 注意：这里 T 应该是 List<Item> 类型
     */
    protected <Item> void executePageRequest(
            FoxSdkPageRequest pageRequest,
            PageRequestFunction<List<Item>> requestFunction,
            FoxSdkPageSuccessConsumer<Item> onSuccess,
            FoxSdkPageErrorConsumer onError,
            FoxSdkPageLoadingConsumer onLoading
    ) {
        // 发送加载状态
        onLoading.accept(pageRequest.isInitial(), pageRequest.getPage());
        if (pageRequest.isInitial()) {
            showLoading("加载中...");
        }

        Disposable disposable = requestFunction.apply(pageRequest.getPage(), pageRequest.getPageSize())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        List<Item> data = result.getData();
                        boolean hasMore = data != null && data.size() >= pageRequest.getPageSize();

                        onSuccess.accept(data, pageRequest.getPage(), hasMore, result.getTotalCount());
                        if (pageRequest.getPage() == 1) {
                            dismissLoading();
                        }
                    } else if (result.isError()) {
                        onError.accept(result.getError(), pageRequest.getPage(), result.getCode());
                        dismissLoading();
                    } else if (result.isEmpty()) {
                        // 处理空数据
                        onSuccess.accept(null, pageRequest.getPage(), false, result.getTotalCount());
                        if (pageRequest.getPage() == 1) {
                            dismissLoading();
                        }
                    }
                }, throwable -> {
                    dismissLoading();
                    onError.accept(throwable.getMessage(), pageRequest.getPage(), -1);
                });

        disposables.add(disposable);
    }

    /**
     * 执行分页网络请求（返回 T，可能是 PageContainer）
     * 注意：这里 T 应该是 FSPageContainer<Item> 或其他分页容器类型
     */
    protected <T> void executePageRequestData(
            FoxSdkPageRequest pageRequest,
            PageRequestFunction<T> requestFunction,
            FoxSdkPageDataSuccessConsumer<T> onSuccess,
            FoxSdkPageErrorConsumer onError,
            FoxSdkPageLoadingConsumer onLoading
    ) {
        // 发送加载状态
        onLoading.accept(pageRequest.isInitial(), pageRequest.getPage());
        if (pageRequest.isInitial()) {
            showLoading("加载中...");
        }

        Disposable disposable = requestFunction.apply(pageRequest.getPage(), pageRequest.getPageSize())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        T data = result.getData();
                        boolean hasMore;

                        if (data instanceof FSPageContainer) {
                            FSPageContainer<?> pageContainer = (FSPageContainer<?>) data;
                            int lastPage = pageContainer.getLastPage() != null ? pageContainer.getLastPage() : 0;
                            int currentPage = pageContainer.getCurrentPage() != null ? pageContainer.getCurrentPage() : 0;
                            hasMore = lastPage > currentPage;
                        } else {
                            hasMore = false; // 对于非分页容器，默认没有更多数据
                        }

                        onSuccess.accept(data, pageRequest.getPage(), hasMore, result.getTotalCount());
                        if (pageRequest.getPage() == 1) {
                            dismissLoading();
                        }
                    } else if (result.isError()) {
                        onError.accept(result.getError(), pageRequest.getPage(), result.getCode());
                        dismissLoading();
                    } else if (result.isEmpty()) {
                        // 处理空数据
                        onSuccess.accept(null, pageRequest.getPage(), false, result.getTotalCount());
                        if (pageRequest.getPage() == 1) {
                            dismissLoading();
                        }
                    }
                }, throwable -> {
                    dismissLoading();
                    onError.accept(throwable.getMessage(), pageRequest.getPage(), -1);
                });

        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
    }

    // 功能接口定义
    public interface FoxSdkNetworkErrorConsumer {
        void accept(String error, Integer code);
    }

    public interface FoxSdkPageSuccessConsumer<T> {
        void accept(List<T> data, int page, boolean hasMore, Integer totalCount);
    }

    public interface FoxSdkPageDataSuccessConsumer<T> {
        void accept(T data, int page, boolean hasMore, Integer totalCount);
    }

    public interface FoxSdkPageErrorConsumer {
        void accept(String error, int page, Integer code);
    }

    public interface FoxSdkPageLoadingConsumer {
        void accept(boolean isInitial, int page);
    }

    // 新的请求函数接口，用于处理 Single<FoxSdkNetworkResult<T>> 类型的返回值
    public interface PageRequestFunction<T> {
        Single<FoxSdkNetworkResult<T>> apply(int page, int pageSize);
    }
}
