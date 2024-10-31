package com.guiyomi;


public class User {
    private String userName;
    private String profilePhotoURL;
    private String fileName;
    private String tokenID;
    private String localID;
    private boolean isLogged;

    public User(String tokenID, String localID, String userName) {
        this.userName = userName;
        this.tokenID = tokenID;
        this.localID = localID;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getProfilePhotoURL() {
        return this.profilePhotoURL;
    }

    public void setProfilePhotoURL(String profilePhotoURL) {
        this.profilePhotoURL = profilePhotoURL;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTokenID() {
        return this.tokenID;
    }

    public String getLocalID() {
        return this.localID;
    }

    public void setIsLogged(boolean isLogged) {
        this.isLogged = isLogged;
    }

    public boolean getIsLogged() {
        return this.isLogged;
    }
}

