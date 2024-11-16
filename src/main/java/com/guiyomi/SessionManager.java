package com.guiyomi;

import java.util.prefs.Preferences;

public class SessionManager {
    private static final String TOKEN_KEY = "tokenID";
    private static final String LOCAL_ID_KEY = "localID";
    private static final String USER_NAME_KEY = "userName";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String PROFILE_PHOTO_URL_KEY = "profilePhotoURL";

    // Save session data
    public static void saveSession(String tokenID, String localID, String userName, String profilePhotoURL) {
        Preferences prefs = Preferences.userRoot().node(SessionManager.class.getName());
        prefs.put(TOKEN_KEY, tokenID);
        prefs.put(LOCAL_ID_KEY, localID);
        prefs.put(USER_NAME_KEY, userName);
        prefs.put(PROFILE_PHOTO_URL_KEY, profilePhotoURL);
        prefs.putLong(TIMESTAMP_KEY, System.currentTimeMillis());
    
        System.out.println("Session saved with userName: " + userName);
    }
    

    // Retrieve session data
    public static String getTokenID() { return Preferences.userRoot().node(SessionManager.class.getName()).get(TOKEN_KEY, null); }
    public static String getLocalID() { return Preferences.userRoot().node(SessionManager.class.getName()).get(LOCAL_ID_KEY, null); }
    public static String getUserName() { return Preferences.userRoot().node(SessionManager.class.getName()).get(USER_NAME_KEY, null); }
    public static String getProfilePhotoURL() { return Preferences.userRoot().node(SessionManager.class.getName()).get(PROFILE_PHOTO_URL_KEY, null); }

    // Check session validity (e.g., 1-hour expiration)
    public static boolean isSessionValid() {
        long timestamp = Preferences.userRoot().node(SessionManager.class.getName()).getLong(TIMESTAMP_KEY, 0);
        return (System.currentTimeMillis() - timestamp) < (60 * 60 * 1000);
    }

    // Clear session data
    public static void clearSession() {
        Preferences prefs = Preferences.userRoot().node(SessionManager.class.getName());
        prefs.remove(TOKEN_KEY);
        prefs.remove(LOCAL_ID_KEY);
        prefs.remove(USER_NAME_KEY);
        prefs.remove(TIMESTAMP_KEY);
        prefs.remove(PROFILE_PHOTO_URL_KEY);
    }
}
