package com.wishfox.foxsdk.data.repository;

import com.wishfox.foxsdk.data.model.entity.FSGameRecord;
import com.wishfox.foxsdk.data.model.entity.FSPageContainer;
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
 * @date: 2025年10月28日 18:46
 */
public class FSGameRecordRepository extends FoxSdkBaseRepository {

    private FoxSdkApiService apiService;

    private FoxSdkApiService getApiService() {
        if (apiService == null) {
            apiService = FoxSdkRetrofitManager.getApiService();
        }
        return apiService;
    }

    /**
     * 获取游戏记录列表
     */
    public Single<FoxSdkNetworkResult<FSPageContainer<FSGameRecord>>> getGameRecordList(int page, int size,String channelId, String appId) {
        return executeCall(() -> getApiService().getGameRecordList(page, size, channelId, appId).blockingGet());
    }
}
