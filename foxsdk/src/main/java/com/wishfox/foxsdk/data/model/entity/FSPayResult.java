package com.wishfox.foxsdk.data.model.entity;

import com.wishfox.foxsdk.utils.FoxSdkPayEnum;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:55
 */
public class FSPayResult {

    private boolean isCheckPay;
    private String orderId;
    private FoxSdkPayEnum payType;

    public FSPayResult() {}

    public FSPayResult(boolean isCheckPay, String orderId, FoxSdkPayEnum payType) {
        this.isCheckPay = isCheckPay;
        this.orderId = orderId;
        this.payType = payType;
    }

    public boolean isCheckPay() { return isCheckPay; }
    public void setCheckPay(boolean checkPay) { isCheckPay = checkPay; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public FoxSdkPayEnum getPayType() { return payType; }
    public void setPayType(FoxSdkPayEnum payType) { this.payType = payType; }
}
