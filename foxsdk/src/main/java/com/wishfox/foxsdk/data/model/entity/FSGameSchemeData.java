package com.wishfox.foxsdk.data.model.entity;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:54
 */
public class FSGameSchemeData {

    private String type;
    private String taskNumer;
    private String tsaskType;
    private boolean isNeedCheckTaskNumber = true;
    private String expand = "";

    public FSGameSchemeData() {}

    public FSGameSchemeData(String type, String taskNumer, String tsaskType,
                            boolean isNeedCheckTaskNumber, String expand) {
        this.type = type;
        this.taskNumer = taskNumer;
        this.tsaskType = tsaskType;
        this.isNeedCheckTaskNumber = isNeedCheckTaskNumber;
        this.expand = expand;
    }

    // Getter和Setter方法
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTaskNumer() { return taskNumer; }
    public void setTaskNumer(String taskNumer) { this.taskNumer = taskNumer; }

    public String getTsaskType() { return tsaskType; }
    public void setTsaskType(String tsaskType) { this.tsaskType = tsaskType; }

    public boolean isNeedCheckTaskNumber() { return isNeedCheckTaskNumber; }
    public void setNeedCheckTaskNumber(boolean needCheckTaskNumber) { isNeedCheckTaskNumber = needCheckTaskNumber; }

    public String getExpand() { return expand; }
    public void setExpand(String expand) { this.expand = expand; }
}
