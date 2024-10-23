package com.guiyomi;

public class AuthResponse {
    private String uid;
    private String idToken;

    public AuthResponse(String uid, String idToken) {
        this.uid = uid;
        this.idToken = idToken;
    }

    public String getUid() {
        return uid;
    }

    public String getIdToken() {
        return idToken;
    }
}
