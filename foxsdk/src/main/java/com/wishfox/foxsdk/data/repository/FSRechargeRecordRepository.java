package com.wishfox.foxsdk.data.repository;

import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.entity.FSPageContainer;
import com.wishfox.foxsdk.data.model.entity.FSRechargeRecord;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.network.FoxSdkApiService;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;

import io.reactivex.rxjava3.core.Single;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:47
 */
public class FSRechargeRecordRepository extends FoxSdkBaseRepository {

    private FoxSdkApiService apiService;

    private FoxSdkApiService getApiService() {
        if (apiService == null) {
            apiService = FoxSdkRetrofitManager.getApiService();
        }
        return apiService;
    }

    /**
     * 获取充值记录列表
     */
    public Single<FoxSdkNetworkResult<FSPageContainer<FSRechargeRecord>>> getRechargeRecords(int page, int size) {
        return executeCall(() -> getApiService().getRechargeRecords(WishFoxSdk.getConfig().getChannelId(), WishFoxSdk.getConfig().getAppId(), page, size).blockingGet());
    }
}
