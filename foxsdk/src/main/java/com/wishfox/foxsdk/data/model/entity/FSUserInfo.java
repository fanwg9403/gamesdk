package com.wishfox.foxsdk.data.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:56
 */
public class FSUserInfo {

    @SerializedName("id")
    private String userId;
    private String userName;
    private String avatar;
    private String mobile;
    private Integer foxCoin;

    public FSUserInfo() {
    }

    public FSUserInfo(String userId, String userName, String avatar, String mobile, Integer foxCoin) {
        this.userId = userId;
        this.userName = userName;
        this.avatar = avatar;
        this.mobile = mobile;
        this.foxCoin = foxCoin;
    }

    // Getter和Setter方法
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Integer getFoxCoin() {
        return foxCoin;
    }

    public void setFoxCoin(Integer foxCoin) {
        this.foxCoin = foxCoin;
    }

    /**
     * 获取单例实例
     */
    public static FSUserInfo getInstance() {
        FSUserProfile userProfile = FSUserProfile.getInstance();
        return userProfile != null ? userProfile.getUserInfo() : null;
    }
}
