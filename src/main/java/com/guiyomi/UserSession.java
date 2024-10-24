package com.guiyomi;

public class UserSession {
    private static String currentUserId;
    private static String currentUserName;
    private static String currentUserPhotoUrl;
    private static String idToken; // Store the idToken

    public static void startSession(String userId, String userName, String photoUrl, String token) {
        currentUserId = userId;
        currentUserName = userName;
        currentUserPhotoUrl = photoUrl;
        idToken = token; 
    }

    public static void endSession() {
        currentUserId = null;
        currentUserName = null;
        currentUserPhotoUrl = null;
        idToken = null; // Clear the token
    }

    public static boolean isLoggedIn() {
        return currentUserId != null;
    }

    public static String getUserId() {
        return currentUserId;
    }

    public static String getUserName() {
        return currentUserName;
    }

    public static String getUserPhotoUrl() {
        return currentUserPhotoUrl;
    }

    public static String getIdToken() { // Provide a method to get the idToken
        return idToken;
    }
}
