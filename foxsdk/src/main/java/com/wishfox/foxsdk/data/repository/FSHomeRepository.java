package com.wishfox.foxsdk.data.repository;

import com.wishfox.foxsdk.core.FoxSdkConfig;
import com.wishfox.foxsdk.core.WishFoxSdk;
import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.data.model.entity.FSGameRecord;
import com.wishfox.foxsdk.data.model.entity.FSHomeBanner;
import com.wishfox.foxsdk.data.model.entity.FSLoginResult;
import com.wishfox.foxsdk.data.model.entity.FSPageContainer;
import com.wishfox.foxsdk.data.model.entity.FSRechargeRecord;
import com.wishfox.foxsdk.data.model.entity.FSUserProfile;
import com.wishfox.foxsdk.data.model.network.FoxSdkNetworkResult;
import com.wishfox.foxsdk.data.network.FoxSdkApiService;
import com.wishfox.foxsdk.data.network.FoxSdkRetrofitManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:59
 */
public class FSHomeRepository extends FoxSdkBaseRepository {

    private FoxSdkApiService service;

    public FSHomeRepository() {
        this.service = FoxSdkRetrofitManager.getApiService();
    }

    public Single<FoxSdkNetworkResult<FSLoginResult>> loginByVerifyCode(String phone, String verifyCode) {
        Map<String, String> params = new HashMap<>();
        params.put("channel_id", WishFoxSdk.getConfig().getChannelId());
        params.put("app_id", WishFoxSdk.getConfig().getAppId());
        params.put("user_name", phone);
        params.put("code", verifyCode);
        params.put("login_type", "2");

        return executeCall(() -> service.login(params).blockingGet());
    }

    public Single<FoxSdkNetworkResult<FSLoginResult>> loginByPassword(String phone, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("channel_id", WishFoxSdk.getConfig().getChannelId());
        params.put("app_id", WishFoxSdk.getConfig().getAppId());
        params.put("user_name", phone);
        params.put("pass_word", password);
        params.put("login_type", "1");

        return executeCall(() -> service.login(params).blockingGet());
    }

    public Single<FoxSdkNetworkResult<FSLoginResult>> login(String phone, String code, int type) {
        if (type == 1) {
            return loginByPassword(phone, code);
        } else {
            return loginByVerifyCode(phone, code);
        }
    }

    public Single<FoxSdkNetworkResult<FSUserProfile>> getUserInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("channel_id", WishFoxSdk.getConfig().getChannelId());
        params.put("app_id", WishFoxSdk.getConfig().getAppId());

        return executeCall(() -> service.getUserInfo(params).blockingGet());
    }

    public Single<FoxSdkNetworkResult<Object>> logout() {
        return executeCall(() -> service.logout().blockingGet());
    }

    public Single<FoxSdkNetworkResult<FSCoinInfo>> getUserVirtualInfo() {
        return executeCall(() -> service.getUserVirtualInfo().blockingGet());
    }

    public Single<FoxSdkNetworkResult<List<FSHomeBanner>>> getAdvertiseList() {
        boolean isLandscape = WishFoxSdk.getConfig().getScreenOrientation() == FoxSdkConfig.ORIENTATION_LANDSCAPE;
        String position = "app-yx-grzx" + (isLandscape ? "-hp" : "");

        return executeCall(() -> service.getAdvertiseList(1, 10, position).blockingGet());
    }

    // 其他API方法可以根据需要添加
    public Single<FoxSdkNetworkResult<Object>> sendSmsCode(String phone) {
        Map<String, String> params = new HashMap<>();
        params.put("channel_id", WishFoxSdk.getConfig().getChannelId());
        params.put("app_id", WishFoxSdk.getConfig().getAppId());
        params.put("user_name", phone);

        return executeCall(() -> service.sendSmsCode(params).blockingGet());
    }
}
