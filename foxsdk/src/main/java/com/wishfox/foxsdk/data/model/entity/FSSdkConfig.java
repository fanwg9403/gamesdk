package com.wishfox.foxsdk.data.model.entity;

public class FSSdkConfig {

    private int pay_type;
    private int wechat_pay_open_status;
    private int ali_pay_open_status;
    private String wechat_scheme;

    public int getPay_type() {
        return pay_type;
    }

    public void setPay_type(int pay_type) {
        this.pay_type = pay_type;
    }

    public int getWechat_pay_open_status() {
        return wechat_pay_open_status;
    }

    public void setWechat_pay_open_status(int wechat_pay_open_status) {
        this.wechat_pay_open_status = wechat_pay_open_status;
    }

    public int getAli_pay_open_status() {
        return ali_pay_open_status;
    }

    public void setAli_pay_open_status(int ali_pay_open_status) {
        this.ali_pay_open_status = ali_pay_open_status;
    }

    public String getWechat_scheme() {
        return wechat_scheme;
    }

    public void setWechat_scheme(String wechat_scheme) {
        this.wechat_scheme = wechat_scheme;
    }
}
