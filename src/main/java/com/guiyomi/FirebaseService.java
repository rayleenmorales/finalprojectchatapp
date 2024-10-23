package com.guiyomi;

import java.net.URL;
import java.net.URI;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import org.json.JSONObject;
import java.nio.file.Path; 
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.File;

public class FirebaseService {

    private static final String API_KEY = "AIzaSyBNtPixNZI2wecaRO37a3l0hkJsjBSYMsQ";
    private static final String FIREBASE_PROJECT_ID = "katalk-db42a"; 
    private static final String FIREBASE_STORAGE_BUCKET = "katalk-db42a.appspot.com"; 


    // Method to sign up user email and password
    public Map<String, String> signUp(String email, String password) throws Exception {
        URI uri = URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY);
        URL url = uri.toURL();  // Converts the URI to a URL
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        
        connection.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("email", email);
        jsonBody.put("password", password);
        jsonBody.put("returnSecureToken", true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Read the response
            Scanner scanner = new Scanner(connection.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            // Parse the response and extract the ID token
            JSONObject jsonResponse = new JSONObject(response);
            String idToken = jsonResponse.getString("idToken");
            System.out.println("User signed in. ID Token: " + idToken);

            String uid = jsonResponse.getString("localId");  // UID of the authenticated user
            System.out.println("User UID: " + uid);

            Map<String, String> authData = new HashMap<>();
            authData.put("uid", uid);
            authData.put("idToken", idToken);

            connection.disconnect();
            return authData; 
        } else {
            // Handle errors
            System.out.println("Sign-in failed. Response Code: " + responseCode);
            connection.disconnect();
            return null;
        }
 
    }

    /// Method to upload the profile picture to Firebase Storage and get the download URL
    public String uploadProfilePhoto(File profilePhoto, String idToken) throws Exception {
        String storageUrl = String.format("https://firebasestorage.googleapis.com/v0/b/" + FIREBASE_STORAGE_BUCKET + "/o?uploadType=media&name=profilePhotos/" + profilePhoto.getName());
        URI uri = URI.create(storageUrl);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        System.out.println("Storage URL: " + storageUrl);
        
        connection.setRequestProperty("Authorization", "Bearer " + idToken);

        String absolutePath = profilePhoto.getAbsolutePath();
        System.out.println("Profile photo absolute path: " + absolutePath);
        Path filePath = Paths.get(absolutePath);

        // Get the correct content type for the file
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "image/jpeg";  // Default to JPEG if content type cannot be detected
        }
        System.out.println("Content-Type detected: " + contentType);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setDoOutput(true);
        
        // Upload the file
        try (OutputStream os = connection.getOutputStream()) {
            byte[] fileBytes = Files.readAllBytes(filePath);
            System.out.println("File size: " + fileBytes.length + " bytes");
            os.write(fileBytes, 0, fileBytes.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Profile photo uploaded successfully.");
        } else {
            System.out.println("Profile photo upload failed. Response Code: " + responseCode);
            return null;
        }

        // Read the response and get the download URL
        String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/" + FIREBASE_STORAGE_BUCKET + "/o/profilePhotos%2F" + profilePhoto.getName() + "?alt=media";

        connection.disconnect();
        return downloadUrl;
    }

    // Method to save user information to Firestore
    public void saveUserInfo(String userId, String firstName, String lastName, String profilePhotoUrl, String idToken) throws Exception {
        String firestoreUrl = String.format("https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/users?documentId=" + userId);
        URI uri = URI.create(firestoreUrl);
        URL url = uri.toURL();

        System.out.println("Firestore URL: " + firestoreUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + idToken);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("fields", new JSONObject()
            .put("firstName", new JSONObject().put("stringValue", firstName))
            .put("lastName", new JSONObject().put("stringValue", lastName))
            .put("profilePhotoUrl", new JSONObject().put("stringValue", profilePhotoUrl))
        );

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("User info saved to Firestore successfully.");
        } else {
            System.out.println("Failed to save user info to Firestore. Response Code: " + responseCode);
        }

        connection.disconnect();
    }

    // Method to handle user sign-in (login)
    public int signIn(String email, String password) throws Exception {
        URI uri = URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY);
        URL url = uri.toURL();  // Converts the URI to a URL
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("email", email);
        jsonBody.put("password", password);
        jsonBody.put("returnSecureToken", true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Login successful.");
        } else {
            System.out.println("Login failed. Response Code: " + responseCode);
        }

        connection.disconnect();

        return responseCode;
    }

    // Method to 
}
