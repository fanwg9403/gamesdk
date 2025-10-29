package com.wishfox.foxsdk.data.repository;

import android.text.TextUtils;

import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.entity.FSPageContainer;
import com.wishfox.foxsdk.data.model.entity.FSStarterPack;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.network.FoxSdkApiService;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:48
 */
public class FSStarterPackRepository extends FoxSdkBaseRepository {

    private FoxSdkApiService apiService;

    private FoxSdkApiService getApiService() {
        if (apiService == null) {
            apiService = FoxSdkRetrofitManager.getApiService();
        }
        return apiService;
    }

    /**
     * 获取新手礼包列表
     */
    public Single<FoxSdkNetworkResult<FSPageContainer<FSStarterPack>>> getStarterPackList(
            int page,
            int size,
            String mailTitle) {
        Map<String, Object> params = new HashMap<>();
        params.put("page_num", page);
        params.put("page_size", size);
        if (!TextUtils.isEmpty(mailTitle)) {
            params.put("mail_title", mailTitle);
        }
        return executeCall(() -> getApiService().getStarterPacks(params).blockingGet());
    }

    /**
     * 获取新手礼包列表（重载方法）
     */
    public Single<FoxSdkNetworkResult<FSPageContainer<FSStarterPack>>> getStarterPackList(int page, int size) {
        return getStarterPackList(page, size, "");
    }

    /**
     * 领取新手礼包
     */
    public Single<FoxSdkNetworkResult<Object>> receiveStarterPack(String mailId) {
        Map<String, String> params = new HashMap<>();
        params.put("mail_id", mailId);
        params.put("channel_id", WishFoxSdk.getConfig().getChannelId());
        params.put("app_id", WishFoxSdk.getConfig().getAppId());
        return executeCall(() -> getApiService().receiveStarterPack(params).blockingGet());
    }
}
