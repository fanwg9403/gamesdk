package com.wishfox.foxsdk.data.model.entity;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.wishfox.foxsdk.core.FoxSdkSPKeys;
import com.wishfox.foxsdk.utils.FoxSdkSPUtils;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:54
 */
public class FSLoginResult {

    @SerializedName("app_id")
    private Long appId;

    @SerializedName("channel_id")
    private Long channelId;

    @SerializedName("game_name")
    private String gameName;

    @SerializedName("game_url")
    private String gameUrl;

    @SerializedName("open_id")
    private String openId;

    private String phone;
    private String sign;

    @SerializedName("time_stamp")
    private Long timeStamp;

    private String token;

    public FSLoginResult() {}

    // Getter和Setter方法
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }

    public String getGameUrl() { return gameUrl; }
    public void setGameUrl(String gameUrl) { this.gameUrl = gameUrl; }

    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }

    public Long getTimeStamp() { return timeStamp; }
    public void setTimeStamp(Long timeStamp) { this.timeStamp = timeStamp; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    /**
     * 获取单例实例
     */
    public static FSLoginResult getInstance() {
        String json = FoxSdkSPUtils.getInstance().getString(FoxSdkSPKeys.LOGIN_RESULT);
        if (!TextUtils.isEmpty(json)) {
            return new Gson().fromJson(json, FSLoginResult.class);
        }
        return null;
    }

    /**
     * 保存到SP
     */
    public static void save(FSLoginResult loginResult) {
        if (loginResult != null) {
            String json = new Gson().toJson(loginResult);
            FoxSdkSPUtils.getInstance().put(FoxSdkSPKeys.LOGIN_RESULT, json);
        }
    }

    /**
     * 获取token
     */
    public static String getTokenEd() {
        FSLoginResult instance = getInstance();
        return instance != null ? instance.getToken() : "";
    }

    /**
     * 清除SP中的数据
     */
    public static void clear() {
        FoxSdkSPUtils.getInstance().remove(FoxSdkSPKeys.LOGIN_RESULT);
    }
}
