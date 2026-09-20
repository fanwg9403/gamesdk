package com.wishfox.foxsdk.data.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2026年09月18日 17:01
 */
public class FSFloatIcon {

    @SerializedName("version")
    private Long version;

    @SerializedName("float_icon")
    private String floatIcon;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getFloatIcon() {
        return floatIcon;
    }

    public void setFloatIcon(String floatIcon) {
        this.floatIcon = floatIcon;
    }
}
