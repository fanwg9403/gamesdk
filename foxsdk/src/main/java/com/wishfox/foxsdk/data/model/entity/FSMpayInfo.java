package com.wishfox.foxsdk.data.model.entity;

public class FSMpayInfo {
    private String appletInfo;
    private FSOrderRequestInfo orderRequestInfo;

    public String getAppletInfo() {
        return appletInfo;
    }
    public void setAppletInfo(String appletInfo) {
        this.appletInfo = appletInfo;
    }
    public FSOrderRequestInfo getOrderRequestInfo() {
        return orderRequestInfo;
    }
    public void setOrderRequestInfo(FSOrderRequestInfo orderRequestInfo) {
        this.orderRequestInfo = orderRequestInfo;
    }
}
