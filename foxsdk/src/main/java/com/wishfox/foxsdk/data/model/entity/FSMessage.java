package com.wishfox.foxsdk.data.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:54
 */
public class FSMessage {

    @SerializedName("created_time")
    private String createdTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("mail_id")
    private Long mailId;

    private Long id;
    private String img;

    @SerializedName("mail_content")
    private String mailContent;

    @SerializedName("mail_title")
    private String mailTitle;

    /**
     * 状态，0未读 1已读
     */
    private Integer is_read;

    private Long type;
    private String users;

    @SerializedName("users_name_list")
    private String usersNameList;

    public FSMessage() {}

    // Getter和Setter方法
    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Long getMailId() { return mailId; }
    public void setMailId(Long mailId) { this.mailId = mailId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }

    public String getMailContent() { return mailContent; }
    public void setMailContent(String mailContent) { this.mailContent = mailContent; }

    public String getMailTitle() { return mailTitle; }
    public void setMailTitle(String mailTitle) { this.mailTitle = mailTitle; }

    public Integer getIs_read() { return is_read; }
    public void setIs_read(Integer is_read) { this.is_read = is_read; }

    public Long getType() { return type; }
    public void setType(Long type) { this.type = type; }

    public String getUsers() { return users; }
    public void setUsers(String users) { this.users = users; }

    public String getUsersNameList() { return usersNameList; }
    public void setUsersNameList(String usersNameList) { this.usersNameList = usersNameList; }

    /**
     * 是否已读
     */
    public boolean isRead() {
        return is_read != null && is_read == 1;
    }

    /**
     * 获取标题
     */
    public String getTitle() {
        return mailTitle;
    }

    /**
     * 获取内容
     */
    public String getContent() {
        return mailContent;
    }

    /**
     * 获取时间
     */
    public String getTime() {
        return createdTime;
    }
}
