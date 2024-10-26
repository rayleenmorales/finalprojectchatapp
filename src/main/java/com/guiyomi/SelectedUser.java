package com.guiyomi;

public class SelectedUser {
    private static String selectedUserId;
    private static String selectedUserName;
    private static String selectedUserPhotoUrl;

    public static void startSession(String userId, String userName, String photoUrl) {
        selectedUserId = userId;
        selectedUserName = userName;
        selectedUserPhotoUrl = photoUrl;
    }

    public static void endSession() {
        selectedUserId = null;
        selectedUserName = null;
        selectedUserPhotoUrl = null;
    }

    public static boolean isLoggedIn() {
        return selectedUserId != null;
    }

    public static String getUserId() {
        return selectedUserId;
    }

    public static String getUserName() {
        return selectedUserName;
    }

    public static String getUserPhotoUrl() {
        return selectedUserPhotoUrl;
    }
}
