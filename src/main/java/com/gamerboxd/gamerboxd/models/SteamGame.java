package com.gamerboxd.gamerboxd.models;

/*
A non-database model used to represent a game in a user's Steam library
*/
public class SteamGame {
    private int appId;
    private String gameName;
    private String iconURL;

    public SteamGame(int appId, String gameName, String iconURL) {
        this.appId = appId;
        this.gameName = gameName;
        this.iconURL = iconURL;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getIconURL() {
        return iconURL;
    }

    public void setIconURL(String iconURL) {
        this.iconURL = iconURL;
    }
}
