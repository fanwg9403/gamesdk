package com.wishfox.foxsdk.data.model;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 15:48
 */
public class FoxSdkBaseResponse<T> {
    private int code;
    private String msg;
    private T data;
    private int total;

    public FoxSdkBaseResponse() {}

    public FoxSdkBaseResponse(int code, String msg, T data, int total) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.total = total;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean isSuccess() {
        return code == 200 || code == 0;
    }
}
