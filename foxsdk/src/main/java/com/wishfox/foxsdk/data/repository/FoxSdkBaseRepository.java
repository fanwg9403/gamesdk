package com.wishfox.foxsdk.data.repository;

import com.wishfox.foxsdk.data.model.FoxSdkBaseResponse;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.model.paging.FoxSdkPageRequest;
import com.wishfox.foxsdk.data.network.FoxSdkNetworkExecutor;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:50
 */
public abstract class FoxSdkBaseRepository {

    protected <T> Single<FoxSdkNetworkResult<T>> executeCall(
            FoxSdkNetworkExecutor.FoxSdkApiCall<FoxSdkBaseResponse<T>> call) {
        return FoxSdkNetworkExecutor.execute(call);
    }

    protected <T> Observable<FoxSdkNetworkResult<T>> executeCallAsObservable(
            FoxSdkNetworkExecutor.FoxSdkApiCall<FoxSdkBaseResponse<T>> call) {
        return FoxSdkNetworkExecutor.executeAsObservable(call);
    }

    protected <T> Observable<FoxSdkNetworkResult<T>> executePageCall(
            FoxSdkPageRequest pageRequest,
            FoxSdkNetworkExecutor.FoxSdkPageApiCall<T> apiCall) {
        return FoxSdkNetworkExecutor.executePage(pageRequest, apiCall);
    }
}
