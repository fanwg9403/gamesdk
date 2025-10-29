package com.wishfox.foxsdk.data.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:55
 */
public class FSRechargeRecord {

    /**
     * 游戏ID
     */
    @SerializedName("app_id")
    private Long appID;

    /**
     * 游戏名称
     */
    @SerializedName("app_name")
    private String appName;

    /**
     * 渠道ID
     */
    @SerializedName("channel_id")
    private Long channelID;

    /**
     * 第三方平台订单ID
     */
    @SerializedName("cp_order_id")
    private String cpOrderID;

    /**
     * 订单创建时间
     */
    @SerializedName("create_time")
    private String createTime;

    /**
     * 货币单位（CNY）
     */
    private String currency;

    /**
     * 回调次数
     */
    @SerializedName("handle_num")
    private Integer handleNum;

    /**
     * 狐少少昵称
     */
    @SerializedName("hss_nick_name")
    private String hssNickName;

    /**
     * 狐少少ID
     */
    @SerializedName("hss_user_id")
    private String hssUserID;

    private Long id;

    /**
     * 商品名称
     */
    @SerializedName("mall_name")
    private String mallName;

    /**
     * 商品SKU
     */
    @SerializedName("mall_sku")
    private String mallSku;

    /**
     * 狐少少订单ID
     */
    @SerializedName("order_id")
    private String orderID;

    /**
     * 支付类型 1支付宝 2微信 3微信app支付
     */
    @SerializedName("pay_type")
    private Integer payType;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 订单价格（单位分）
     */
    private Double price;

    /**
     * 角色ID
     */
    @SerializedName("role_id")
    private String roleID;

    /**
     * 角色名称
     */
    @SerializedName("role_name")
    private String roleName;

    /**
     * 服务器ID
     */
    @SerializedName("server_id")
    private Long serverID;

    /**
     * 服务器名称
     */
    @SerializedName("server_name")
    private String serverName;

    /**
     * 支付状态-1取消支付 0未支付 1已支付 2已发货
     */
    private Integer status;

    /**
     * 是否测试订单
     */
    @SerializedName("test_status")
    private Integer testStatus;

    /**
     * 订单修改时间
     */
    @SerializedName("updated_time")
    private String updatedTime;

    /**
     * 用户编号
     */
    @SerializedName("user_id")
    private Long userID;

    public FSRechargeRecord() {}

    // Getter和Setter方法
    public Long getAppID() { return appID; }
    public void setAppID(Long appID) { this.appID = appID; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public Long getChannelID() { return channelID; }
    public void setChannelID(Long channelID) { this.channelID = channelID; }

    public String getCpOrderID() { return cpOrderID; }
    public void setCpOrderID(String cpOrderID) { this.cpOrderID = cpOrderID; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getHandleNum() { return handleNum; }
    public void setHandleNum(Integer handleNum) { this.handleNum = handleNum; }

    public String getHssNickName() { return hssNickName; }
    public void setHssNickName(String hssNickName) { this.hssNickName = hssNickName; }

    public String getHssUserID() { return hssUserID; }
    public void setHssUserID(String hssUserID) { this.hssUserID = hssUserID; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMallName() { return mallName; }
    public void setMallName(String mallName) { this.mallName = mallName; }

    public String getMallSku() { return mallSku; }
    public void setMallSku(String mallSku) { this.mallSku = mallSku; }

    public String getOrderID() { return orderID; }
    public void setOrderID(String orderID) { this.orderID = orderID; }

    public Integer getPayType() { return payType; }
    public void setPayType(Integer payType) { this.payType = payType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getRoleID() { return roleID; }
    public void setRoleID(String roleID) { this.roleID = roleID; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public Long getServerID() { return serverID; }
    public void setServerID(Long serverID) { this.serverID = serverID; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getTestStatus() { return testStatus; }
    public void setTestStatus(Integer testStatus) { this.testStatus = testStatus; }

    public String getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(String updatedTime) { this.updatedTime = updatedTime; }

    public Long getUserID() { return userID; }
    public void setUserID(Long userID) { this.userID = userID; }
}
