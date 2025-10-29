package com.wishfox.foxsdk.data.model.entity;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:53
 */
public class FSGameRecord {

    private int app_id;
    private String app_name;
    private String create_time;
    private int hss_user_id;
    private int id;
    private String last_play_time;
    private int play_duration;
    private Object server_id;
    private String server_name;
    private String update_time;
    private String game_index_img_file_name;

    public FSGameRecord() {}

    // Getter和Setter方法
    public int getApp_id() { return app_id; }
    public void setApp_id(int app_id) { this.app_id = app_id; }

    public String getApp_name() { return app_name; }
    public void setApp_name(String app_name) { this.app_name = app_name; }

    public String getCreate_time() { return create_time; }
    public void setCreate_time(String create_time) { this.create_time = create_time; }

    public int getHss_user_id() { return hss_user_id; }
    public void setHss_user_id(int hss_user_id) { this.hss_user_id = hss_user_id; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLast_play_time() { return last_play_time; }
    public void setLast_play_time(String last_play_time) { this.last_play_time = last_play_time; }

    public int getPlay_duration() { return play_duration; }
    public void setPlay_duration(int play_duration) { this.play_duration = play_duration; }

    public Object getServer_id() { return server_id; }
    public void setServer_id(Object server_id) { this.server_id = server_id; }

    public String getServer_name() { return server_name; }
    public void setServer_name(String server_name) { this.server_name = server_name; }

    public String getUpdate_time() { return update_time; }
    public void setUpdate_time(String update_time) { this.update_time = update_time; }

    public String getGame_index_img_file_name() { return game_index_img_file_name; }
    public void setGame_index_img_file_name(String game_index_img_file_name) { this.game_index_img_file_name = game_index_img_file_name; }
}
