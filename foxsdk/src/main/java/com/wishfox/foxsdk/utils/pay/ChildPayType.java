package com.wishfox.foxsdk.utils.pay;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 17:05
 */
public enum ChildPayType {
    WX_PAY(503, "微信支付"),
    AL_PAY(502, "支付宝支付"),
    ERROR(-1, "错误");

    private final int value;
    private final String explain;

    ChildPayType(int value, String explain) {
        this.value = value;
        this.explain = explain;
    }

    public int getValue() {
        return value;
    }

    public String getExplain() {
        return explain;
    }
}
