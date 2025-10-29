package com.wishfox.foxsdk.data.repository;

import com.wishfox.foxsdk.data.model.entity.FSWinFoxCoin;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.network.FoxSdkApiService;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:49
 */
public class FSWinFoxCoinRepository extends FoxSdkBaseRepository {

    private FoxSdkApiService apiService;

    private FoxSdkApiService getApiService() {
        if (apiService == null) {
            apiService = FoxSdkRetrofitManager.getApiService();
        }
        return apiService;
    }

    /**
     * 获取赢狐币列表
     */
    public Single<FoxSdkNetworkResult<List<FSWinFoxCoin>>> getWinFoxCoinList(int page, int size) {
        return executeCall(() -> getApiService().getWinFoxCoins(page, size).blockingGet());
    }
}