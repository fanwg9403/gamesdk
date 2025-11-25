package com.wishfox.foxsdk.data.model.entity;

public class FSRawResponse {
    private String amt;
    private String externalTraceNo;
    private String merchantId;
    private FSMpayInfo mpayInfo;

    public String getAmt() {
        return amt;
    }

    public void setAmt(String amt) {
        this.amt = amt;
    }

    public String getExternalTraceNo() {
        return externalTraceNo;
    }

    public void setExternalTraceNo(String externalTraceNo) {
        this.externalTraceNo = externalTraceNo;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public FSMpayInfo getMpayInfo() {
        return mpayInfo;
    }

    public void setMpayInfo(FSMpayInfo mpayInfo) {
        this.mpayInfo = mpayInfo;
    }

}
