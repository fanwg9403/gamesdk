package com.wishfox.foxsdk.ui.viewstate;

import com.wishfox.foxsdk.data.model.entity.FSCoinInfo;
import com.wishfox.foxsdk.data.model.entity.FSHomeBanner;
import com.wishfox.foxsdk.data.model.entity.FSUserInfo;

import java.util.List;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:57
 */
public class FSHomeViewState implements FoxSdkViewState {
    private FSUserInfo userInfo;
    private List<FSHomeBanner> bannerList;
    private FSCoinInfo coinInfo;
    private boolean loginSuccess;

    public FSHomeViewState() {
        this(null, null, null, true);
    }

    public FSHomeViewState(FSUserInfo userInfo, List<FSHomeBanner> bannerList,
                           FSCoinInfo coinInfo, boolean loginSuccess) {
        this.userInfo = userInfo;
        this.bannerList = bannerList;
        this.coinInfo = coinInfo;
        this.loginSuccess = loginSuccess;
    }

    // Getters and Setters
    public FSUserInfo getUserInfo() { return userInfo; }
    public void setUserInfo(FSUserInfo userInfo) { this.userInfo = userInfo; }

    public List<FSHomeBanner> getBannerList() { return bannerList; }
    public void setBannerList(List<FSHomeBanner> bannerList) { this.bannerList = bannerList; }

    public FSCoinInfo getCoinInfo() { return coinInfo; }
    public void setCoinInfo(FSCoinInfo coinInfo) { this.coinInfo = coinInfo; }

    public boolean isLoginSuccess() { return loginSuccess; }
    public void setLoginSuccess(boolean loginSuccess) { this.loginSuccess = loginSuccess; }
}
