package com.guiyomi;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Firebase {
    private static HttpClient client = HttpClient.newHttpClient();
        private static String webAPIKey = "AIzaSyBnhQYc8oXh-ydkEOka0Pu8JDKQyfCfXHY";
        private static String signUpURL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + webAPIKey;
        private static String signInURL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + webAPIKey;
        private static String databaseURL = "https://katalkfirebase-default-rtdb.asia-southeast1.firebasedatabase.app/";
        private static String storageURL = "https://firebasestorage.googleapis.com/v0/b/katalkfirebase.appspot.com/";


        public static User signUp(String email, String password, String username) throws FirebaseAuthException {
            JsonObject signUpData = new JsonObject();
            signUpData.addProperty("email", email);
            signUpData.addProperty("password", password);
            signUpData.addProperty("returnSecureToken", true);
        
            try {
                HttpRequest registerReq = HttpRequest.newBuilder()
                    .uri(URI.create(signUpURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(signUpData.toString()))
                    .build();
                HttpResponse<String> registerRes = client.sendAsync(registerReq, HttpResponse.BodyHandlers.ofString()).get();
        
                if (registerRes.statusCode() == 200) {
                    JsonObject resultData = JsonParser.parseString(registerRes.body()).getAsJsonObject();
                    String idToken = resultData.get("idToken").getAsString();
                    String localId = resultData.get("localId").getAsString();
        
                    // Save user information to Firebase Realtime Database
                    JsonObject databaseData = new JsonObject();
                    databaseData.addProperty("username", username);
                    HttpRequest databaseReq = HttpRequest.newBuilder()
                        .uri(URI.create(databaseURL + "users/" + localId + ".json?auth=" + idToken))
                        .PUT(HttpRequest.BodyPublishers.ofString(databaseData.toString()))
                        .build();
                    HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
        
                    if (databaseRes.statusCode() == 200) {
                        // Create and return the new User instance
                        return new User(idToken, localId, username);
                    } else {
                        System.out.println("Failed to save user to database.");
                    }
                } else {
                    JsonObject errorObj = JsonParser.parseString(registerRes.body()).getAsJsonObject();
                    String errorCode = errorObj.getAsJsonObject("error").get("message").getAsString();
                    throw new FirebaseAuthException(errorCode);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
        
        
        // Method to upload a profile picture to Firebase Storage and return the download URL
        public static String uploadProfile(String tokenID, File profilePhoto) {
            String absolutePath = profilePhoto.getAbsolutePath();
            Path filePath = Paths.get(absolutePath);

            try {
                // Step 1: Prepare and send the upload request to Firebase Storage
                HttpRequest storageReq = HttpRequest.newBuilder()
                        .uri(URI.create(storageURL + "o?name=profilePhotos/" + profilePhoto.getName()))
                        .POST(HttpRequest.BodyPublishers.ofFile(filePath))
                        .header("Authorization", "Firebase " + tokenID)
                        .build();
                HttpResponse<String> storageRes = client.sendAsync(storageReq, HttpResponse.BodyHandlers.ofString()).get();
                System.out.println("Upload Profile Response: " + storageRes.statusCode());

                // Step 2: If upload is successful, retrieve the download token
                if (storageRes.statusCode() == 200) {
                    JsonObject resultData = JsonParser.parseString(storageRes.body()).getAsJsonObject();
                    String downloadToken = resultData.get("downloadTokens").getAsString();

                    // Step 3: Construct and return the complete download URL
                    return storageURL + "o/profilePhotos%2F" + profilePhoto.getName() + "?alt=media&token=" + downloadToken;
                }
                System.out.println(storageRes.body());
            } catch (ExecutionException | InterruptedException | FileNotFoundException err) {
                err.printStackTrace();
            }
            return "";
        }
        
        // Method to update user information in the Firebase Realtime Database
        public static void updateUserInfo(String localID, String tokenID, String profilePhotoURL, String fileName) {
            try {
                // Get the existing user data if available
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(URI.create(databaseURL + "users/" + localID + ".json?auth=" + tokenID))
                        .GET()
                        .build();

                HttpResponse<String> getResponse = client.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString()).get();
                JsonObject existingData;
                if (getResponse.statusCode() == 200 && !getResponse.body().equals("null")) {
                    existingData = JsonParser.parseString(getResponse.body()).getAsJsonObject();
                } else {
                    existingData = new JsonObject();
                }

                // Update data fields
                existingData.addProperty("profilePhotoURL", profilePhotoURL);
                existingData.addProperty("fileName", fileName);
                existingData.addProperty("userID", localID);
                existingData.addProperty("isLogged", true); // Mark as logged in

                // Send the updated data to Firebase
                HttpRequest putRequest = HttpRequest.newBuilder()
                        .uri(URI.create(databaseURL + "users/" + localID + ".json?auth=" + tokenID))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(existingData.toString()))
                        .build();

                HttpResponse<String> putResponse = client.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString()).get();
                if (putResponse.statusCode() == 200) {
                    System.out.println("UpdateUserInfo: User info updated successfully.");
                } else {
                    System.out.println("Failed to update user info: " + putResponse.body());
                }
            } catch (ExecutionException | InterruptedException err) {
                err.printStackTrace();
            }
        }

        public static User signIn(String email, String password) throws FirebaseAuthException {
            JsonObject signInData = new JsonObject();
            signInData.addProperty("email", email);
            signInData.addProperty("password", password);
            signInData.addProperty("returnSecureToken", true);
        
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(signInURL))
                    .POST(HttpRequest.BodyPublishers.ofString(signInData.toString()))
                    .build();
                HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();
        
                if (response.statusCode() == 200) {
                    JsonObject resultData = JsonParser.parseString(response.body()).getAsJsonObject();
                    String idToken = resultData.get("idToken").getAsString();
                    String localId = resultData.get("localId").getAsString();
                    
                    // Get the username from the database
                    // Check the isLogged status from the database
                    HttpRequest databaseReq = HttpRequest.newBuilder()
                            .uri(URI.create(databaseURL + "users/" + localId + ".json?auth=" + idToken))
                            .GET().build();
                    HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
        
                    if (databaseRes.statusCode() == 200) {
                        JsonObject databaseResultData = JsonParser.parseString(databaseRes.body()).getAsJsonObject();
                        String username = databaseResultData.get("username").getAsString();
                        
                        // boolean isLogged = databaseResultData.has("isLogged") && databaseResultData.get("isLogged").getAsBoolean();
        
                        // if (isLogged) {
                        //     System.out.println("User is already logged in from another device.");
                        //     return null; // Prevent multiple login
                        // }
        
                        // Return a new User instance with the session data
                        return new User(idToken, localId, username);
                    
                    } else {
                        System.out.println("Failed to get user data from database.");
                    }

                } else {
                    JsonObject errorObj = JsonParser.parseString(response.body()).getAsJsonObject();
                    String errorCode = errorObj.getAsJsonObject("error").get("message").getAsString();
                    throw new FirebaseAuthException(errorCode);
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
        
        
        

        public static void updateIsLogged (String localID, String tokenID, boolean isLogged) {
            try {
                HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "users/" + localID + ".json?auth=" + tokenID))
                    .GET()
                    .build();
                HttpResponse<String> getResponse = client.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString()).get();
        
                JsonObject existingData;
                if (getResponse.statusCode() == 200 && !getResponse.body().equals("null")) {
                    existingData = JsonParser.parseString(getResponse.body()).getAsJsonObject();
                } else {
                    // If no existing data, create a new JSON object
                    existingData = new JsonObject();
                }
        
                existingData.addProperty("isLogged", isLogged);
        
                HttpRequest putRequest = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "users/" + localID + ".json?auth=" + tokenID))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(existingData.toString()))
                    .build();
                HttpResponse<String> putResponse = client.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString()).get();
        
                if (putResponse.statusCode() == 200) {
                    System.out.println("User isLogged updated successfully.");
                } else {
                    System.out.println("Failed to update user info: " + putResponse.body());
                }
            } catch (ExecutionException | InterruptedException err) {
                err.printStackTrace();
            }
        }
}
