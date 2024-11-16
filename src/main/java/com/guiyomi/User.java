package com.guiyomi;

import javafx.scene.image.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

public class User {
    private String userName;
    private String profilePhotoURL;
    private String tokenID;
    private String localID;
    private boolean isLogged;

    public User(String tokenID, String localID, String userName) {
        this.userName = userName;
        this.tokenID = tokenID;
        this.localID = localID;
    }
    
    public User(String tokenID, String localID, String userName, String profileURL) {
        this.userName = userName;
        this.tokenID = tokenID;
        this.localID = localID;
        this.profilePhotoURL = profileURL;
    }

    public User(String username, String profileURL, boolean isLogged, String userID) {
        this.userName = username;
        this.profilePhotoURL = profileURL;
        this.isLogged = isLogged;
        this.localID = userID;
    }

    public boolean getisLogged() {
        return isLogged;
    }

    public String getUserName() { 
        return this.userName; 
    }
    
    public String getTokenID() { 
        return this.tokenID; 
    }

    public String getLocalID() { 
        return this.localID; 
    }

    // Load profile picture from cache or Firebase
    public Image getProfilePicture() {
        File cachedImage = new File("cached_profiles/" + this.localID + ".jpg");
        if (cachedImage.exists()) {
            return new Image(cachedImage.toURI().toString());
        } else {
            downloadAndCacheProfilePicture();
            return new Image(cachedImage.toURI().toString());
        }
    }

    public void downloadAndCacheProfilePicture() {
        if (this.profilePhotoURL == null || this.profilePhotoURL.isEmpty()) {
            System.out.println("Profile photo URL is null or empty, cannot download profile picture.");
            return;
        }
    
        try (InputStream in = new URI(this.profilePhotoURL).toURL().openStream();
             FileOutputStream out = new FileOutputStream("cached_profiles/" + this.localID + ".jpg")) {
    
            byte[] buffer = new byte[1024];
            int bytesRead;
    
            // Read image data from the URL and write it to the file
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
    
            System.out.println("Profile picture cached locally for user: " + this.localID);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to download or cache profile picture.");
        }
    }
    

    public void setProfilePhotoURL(String profilePhotoURL) {
        this.profilePhotoURL = profilePhotoURL;
    }

    public String getProfilePhotoURL() {
        return profilePhotoURL;
    }
}
