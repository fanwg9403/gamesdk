package com.wishfox.foxsdk.data.repository;

import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.entity.FSMessage;
import com.wishfox.foxsdk.data.model.entity.FSPageContainer;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.network.FoxSdkApiService;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;
import okhttp3.RequestBody;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 18:47
 */
public class FSMessageRepository extends FoxSdkBaseRepository {

    private FoxSdkApiService service;
    private int pageSize = 10;

    private FoxSdkApiService getService() {
        if (service == null) {
            service = FoxSdkRetrofitManager.getApiService();
        }
        return service;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Single<FoxSdkNetworkResult<FSPageContainer<FSMessage>>> getMessageList(int page, int size, String channelId, String appId) {
        return executeCall(() -> getService().getSystemMessages(page, size, channelId, appId, 1).blockingGet());
    }

    public Single<FoxSdkNetworkResult<Object>> read(Long id) {
        return executeCall(() -> {
            if (id != null) {
                Map<String, Object> bodyMap = new HashMap<>();
                bodyMap.put("mail_id", id);
                bodyMap.put("channel_id", WishFoxSdk.getConfig().getChannelId());
                bodyMap.put("app_id", WishFoxSdk.getConfig().getAppId());

                // 假设有 toBody() 方法将 Map 转换为 RequestBody
                RequestBody body = mapToRequestBody(bodyMap);
                return getService().readWithBody(body).blockingGet();
            } else {
                return getService().read(WishFoxSdk.getConfig().getChannelId(), WishFoxSdk.getConfig().getAppId()).blockingGet();
            }
        });
    }

    // 辅助方法：将 Map 转换为 RequestBody
    private RequestBody mapToRequestBody(Map<String, Object> map) {
        // 这里需要根据实际情况实现 Map 到 RequestBody 的转换
        // 这只是一个示例，实际实现可能不同
        return RequestBody.create(null, new byte[0]);
    }
}
