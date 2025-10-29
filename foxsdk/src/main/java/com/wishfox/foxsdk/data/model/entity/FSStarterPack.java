package com.wishfox.foxsdk.data.model.entity;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:55
 */
public class FSStarterPack {

    private String code;
    private String created_time;
    private Object end_time;
    private Long id;
    private String mail_content;
    private String mail_title;
    private Integer status;
    private Long type;
    private String users;
    private String users_name_list;
    private String img;

    public FSStarterPack() {}

    // Getter和Setter方法
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCreated_time() { return created_time; }
    public void setCreated_time(String created_time) { this.created_time = created_time; }

    public Object getEnd_time() { return end_time; }
    public void setEnd_time(Object end_time) { this.end_time = end_time; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMail_content() { return mail_content; }
    public void setMail_content(String mail_content) { this.mail_content = mail_content; }

    public String getMail_title() { return mail_title; }
    public void setMail_title(String mail_title) { this.mail_title = mail_title; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getType() { return type; }
    public void setType(Long type) { this.type = type; }

    public String getUsers() { return users; }
    public void setUsers(String users) { this.users = users; }

    public String getUsers_name_list() { return users_name_list; }
    public void setUsers_name_list(String users_name_list) { this.users_name_list = users_name_list; }

    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
}
