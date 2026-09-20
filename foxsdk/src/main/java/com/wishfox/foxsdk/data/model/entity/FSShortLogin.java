package com.wishfox.foxsdk.data.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2026年09月18日 14:58
 */
public class FSShortLogin {

    @SerializedName("expire_at")
    private Long expireAt;

    // 有效期
    @SerializedName("expires_in")
    private Long expiresIn;

    // 短时token
    @SerializedName("short_token")
    private String shortToken;

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getShortToken() {
        return shortToken;
    }

    public void setShortToken(String shortToken) {
        this.shortToken = shortToken;
    }
}
