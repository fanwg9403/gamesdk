package com.wishfox.foxsdk.data.model.entity;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:53
 */
public class FSCheckOrder {

    private int status;
    private String order_id;

    public FSCheckOrder() {}

    public FSCheckOrder(int status, String order_id) {
        this.status = status;
        this.order_id = order_id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }
}
