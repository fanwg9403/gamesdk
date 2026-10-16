package com.wishfox.foxsdk.data.model.entity;

public class FSSchemeData {

    private String ext;
    private String gameId = "";
    private String gameChannel = "";
    private String dynamicParam = "";

    public FSSchemeData(String ext, String gameId, String gameChannel) {
        this.ext = ext;
        this.gameId = gameId;
        this.gameChannel = gameChannel;
    }

    public FSSchemeData(String ext, String gameId, String gameChannel, String dynamicParam) {
        this.ext = ext;
        this.gameId = gameId;
        this.gameChannel = gameChannel;
        this.dynamicParam = dynamicParam;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getGameChannel() {
        return gameChannel;
    }

    public void setGameChannel(String gameChannel) {
        this.gameChannel = gameChannel;
    }

    public String getDynamicParam() {
        return dynamicParam;
    }

    public void setDynamicParam(String dynamicParam) {
        this.dynamicParam = dynamicParam;
    }
}
