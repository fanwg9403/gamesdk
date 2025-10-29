package com.wishfox.foxsdk.data.model.entity;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.wishfox.foxsdk.core.FoxSdkSPKeys;
import com.wishfox.foxsdk.utils.FoxSdkSPUtils;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:53
 */
public class FSCoinInfo {

    private String balanceCoin;
    private String rmbToFoxCoinRatio;

    public FSCoinInfo() {}

    public FSCoinInfo(String balanceCoin, String rmbToFoxCoinRatio) {
        this.balanceCoin = balanceCoin;
        this.rmbToFoxCoinRatio = rmbToFoxCoinRatio;
    }

    public String getBalanceCoin() {
        return balanceCoin;
    }

    public void setBalanceCoin(String balanceCoin) {
        this.balanceCoin = balanceCoin;
    }

    public String getRmbToFoxCoinRatio() {
        return rmbToFoxCoinRatio;
    }

    public void setRmbToFoxCoinRatio(String rmbToFoxCoinRatio) {
        this.rmbToFoxCoinRatio = rmbToFoxCoinRatio;
    }

    /**
     * 获取狐币余额（整数）
     */
    public int getFoxCoin() {
        if (balanceCoin != null) {
            try {
                return (int) Float.parseFloat(balanceCoin);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 获取单例实例
     */
    public static FSCoinInfo getInstance() {
        String json = FoxSdkSPUtils.getInstance().getString(FoxSdkSPKeys.COIN_INFO);
        if (!TextUtils.isEmpty(json)) {
            return new Gson().fromJson(json, FSCoinInfo.class);
        }
        return null;
    }

    /**
     * 保存到SP
     */
    public static void save(FSCoinInfo coinInfo) {
        if (coinInfo != null) {
            String json = new Gson().toJson(coinInfo);
            FoxSdkSPUtils.getInstance().put(FoxSdkSPKeys.COIN_INFO, json);
        }
    }

    /**
     * 清除SP中的数据
     */
    public static void clear() {
        FoxSdkSPUtils.getInstance().remove(FoxSdkSPKeys.COIN_INFO);
    }
}
