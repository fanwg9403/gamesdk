package com.wishfox.foxsdk.data.model.entity;

public class FSSchemeData {

    private String ext;
    private String gameId = "";
    private String gameChannel = "";

    public FSSchemeData(String ext, String gameId, String gameChannel) {
        this.ext = ext;
        this.gameId = gameId;
        this.gameChannel = gameChannel;
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
}
