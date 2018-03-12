package com.allisonoler;

import com.google.api.services.gmail.Gmail;

public class User {
    private String phoneNumber;
    private String refreshToken;
    private String lableID;
    private String userName;
    private Gmail service;

    public User(String phoneNumber, String refreshToken, String lableID, String userName) {
        this.phoneNumber = phoneNumber;
        this.refreshToken = refreshToken;
        this.lableID = lableID;
        this.userName = userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getLableID() {
        return lableID;
    }

    public String getUserName() {
        return userName;
    }

    public Gmail getService() {
        return service;
    }

    public void setService(Gmail service) {
        this.service = service;
    }
}
