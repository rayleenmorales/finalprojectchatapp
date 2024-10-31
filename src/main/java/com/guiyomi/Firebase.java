package com.guiyomi;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

    public static User signUp(String email, String password, String username) throws FirebaseAuthException{
        JsonObject signUpData = new JsonObject();
        signUpData.addProperty("email", email);
        signUpData.addProperty("password", password);
        signUpData.addProperty("returnSecureToken", true);

        JsonObject databaseData = new JsonObject();
        databaseData.addProperty("username", username);

        try {
            HttpRequest registerReq = HttpRequest.newBuilder().uri(URI.create(signUpURL))
            .header("Content-Type", "application/json")
                                                                .POST(HttpRequest.BodyPublishers.ofString(signUpData.toString()))
                                                                .build();
            HttpResponse<String> registerRes = client.sendAsync(registerReq, HttpResponse.BodyHandlers.ofString()).get();
            System.out.println("Status Code: " + registerRes.statusCode());
            if (registerRes.statusCode() == 200) {
                JsonObject resultData = JsonParser.parseString(registerRes.body()).getAsJsonObject();
                String idToken = resultData.get("idToken").getAsString();
                String localId = resultData.get("localId").getAsString();
                HttpRequest databaseReq = HttpRequest.newBuilder()
                        .uri(URI.create(databaseURL + "users/" + localId + ".json?auth=" + idToken))
                        .PUT(HttpRequest.BodyPublishers.ofString(databaseData.toString()))
                        .build();
                HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
                if (databaseRes.statusCode() == 200) {
                    return new User(idToken, localId, username);
                } else {
                    System.out.println("databaseReq failed");
                    return null;
                }
            }else {
                // Parse error code from response
                JsonObject errorObj = JsonParser.parseString(registerRes.body()).getAsJsonObject();
                String errorCode = errorObj.getAsJsonObject("error").get("message").getAsString();
                throw new FirebaseAuthException(errorCode);
            }
        }catch (ExecutionException | InterruptedException err) {
            err.printStackTrace();
        }
        return null;
    }
    
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
            System.out.println(storageRes.statusCode());

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
    
    public static void updateUserInfo(String localID, String tokenID, String profilePhotoURL, String fileName) {
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
            
            boolean isLogged = false;

            existingData.addProperty("profilePhotoURL", profilePhotoURL);
            existingData.addProperty("fileName", fileName);
            existingData.addProperty("userID", localID);
            existingData.addProperty("isLogged", isLogged);
    
            HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create(databaseURL + "users/" + localID + ".json?auth=" + tokenID))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(existingData.toString()))
                .build();
            HttpResponse<String> putResponse = client.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString()).get();
    
            if (putResponse.statusCode() == 200) {
                System.out.println("User info updated successfully.");
            } else {
                System.out.println("Failed to update user info: " + putResponse.body());
            }
        } catch (ExecutionException | InterruptedException err) {
            err.printStackTrace();
        }
    }

    public static User signIn(String email, String password) {
        JsonObject signInData = new JsonObject();
        signInData.addProperty("email", email);
        signInData.addProperty("password", password);
        signInData.addProperty("returnSecureToken", true);
    
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(signInURL))
                    .POST(HttpRequest.BodyPublishers.ofString(signInData.toString())).build();
            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();
    
            System.out.println("Status Code: " + response.statusCode());
            if (response.statusCode() == 200) {
                JsonObject resultData = JsonParser.parseString(response.body()).getAsJsonObject();
                String idToken = resultData.get("idToken").getAsString();
                String localId = resultData.get("localId").getAsString();
    
                // Check the isLogged status from the database
                HttpRequest databaseReq = HttpRequest.newBuilder()
                        .uri(URI.create(databaseURL + "users/" + localId + ".json?auth=" + idToken))
                        .GET().build();
                HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
    
                if (databaseRes.statusCode() == 200) {
                    JsonObject databaseResultData = JsonParser.parseString(databaseRes.body()).getAsJsonObject();
                    boolean isLogged = databaseResultData.has("isLogged") && databaseResultData.get("isLogged").getAsBoolean();
    
                    if (isLogged) {
                        System.out.println("User is already logged in from another device.");
                        return null; // Prevent multiple login
                    }
    
                    // Allow login and return user if not logged in
                    return new User(idToken, localId, databaseResultData.get("username").getAsString());
                }
            } else {
                System.out.println("Something went wrong while signing in");
            }
        } catch (ExecutionException | InterruptedException err) {
            err.printStackTrace();
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
                System.out.println("User info updated successfully.");
            } else {
                System.out.println("Failed to update user info: " + putResponse.body());
            }
        } catch (ExecutionException | InterruptedException err) {
            err.printStackTrace();
        }
    }

    public static void putMessage(String idToken, String conversationId, JsonObject newMessage) {
        String firebaseUrl = databaseURL + "conversations/" + conversationId + "/messages.json?auth=" + idToken;
    
        try {
            // Step 1: Get existing messages
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(firebaseUrl))
                    .GET()
                    .build();
    
            HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
    
            JsonObject allMessages;
            
            // Parse the existing messages or initialize a new JsonObject if empty
            if (getResponse.statusCode() == 200 && !getResponse.body().equals("null")) {
                allMessages = JsonParser.parseString(getResponse.body()).getAsJsonObject();
            } else {
                allMessages = new JsonObject();
            }
    
            // Step 2: Add the new message with its unique ID to the JsonObject of messages
            String messageId = newMessage.get("messageId").getAsString();
            allMessages.add(messageId, newMessage);
    
            // Step 3: Update Firebase with the combined messages
            HttpRequest putRequest = HttpRequest.newBuilder()
                    .uri(URI.create(firebaseUrl))
                    .PUT(HttpRequest.BodyPublishers.ofString(allMessages.toString()))
                    .header("Content-Type", "application/json")
                    .build();
    
            HttpResponse<String> putResponse = client.send(putRequest, HttpResponse.BodyHandlers.ofString());
    
            if (putResponse.statusCode() == 200) {
                System.out.println("Message sent successfully: " + newMessage);
            } else {
                System.out.println("Failed to send message: " + putResponse.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    public static ArrayList<JsonObject> getMessages(String idToken, String conversationID) {
        ArrayList<JsonObject> conversations = new ArrayList<>();
        System.out.println("Fetching messages for conversation ID: " + conversationID);
    
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "conversations/" + conversationID + "/messages.json?auth=" + idToken))
                    .GET()
                    .build();
            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();
            
            if (response.statusCode() == 200 && !response.body().equals("null")) {
                JsonObject resultData = JsonParser.parseString(response.body()).getAsJsonObject();
                for (String key : resultData.keySet()) {
                    JsonObject messageData = resultData.getAsJsonObject(key);
                    System.out.println("Retrieved message: " + messageData);  // Log each message retrieved
                    conversations.add(messageData);
                }
            } else {
                System.out.println("No messages found or error retrieving messages for conversation ID: " + conversationID);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    
        return conversations;
    }
    
    

    public static String getUsername(String localId, String idToken) {
        try {
            HttpRequest databaseReq = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "users/" + localId + "/username.json?auth=" + idToken))
                    .GET()
                    .build();
            HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
            if (databaseRes.statusCode() == 200) {
                return databaseRes.body().substring(1, databaseRes.body().length()-1);
            }
        }catch (ExecutionException | InterruptedException err) {
            err.printStackTrace();
        }
        return "";
    }

    public static String getFileName(String localId, String idToken) {
        try {
            HttpRequest databaseReq = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "users/" + localId + "/fileName.json?auth=" + idToken))
                    .GET()
                    .build();
            HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
            if (databaseRes.statusCode() == 200) {
                return databaseRes.body().substring(1, databaseRes.body().length()-1);
            }
        }catch (ExecutionException | InterruptedException err) {
            err.printStackTrace();
        }
        return "";
    }

    public static String getProfilePhotoURL(String localId, String idToken) {
        try {
            HttpRequest databaseReq = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "users/" + localId + "/profilePhotoURL.json?auth=" + idToken))
                    .GET()
                    .build();
            HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
            if (databaseRes.statusCode() == 200) {
                return databaseRes.body().substring(1, databaseRes.body().length()-1);
            }
        }catch (ExecutionException | InterruptedException err) {
            err.printStackTrace();
        }
        return "";
    }

    

    public static String getPhotoURL (String localId, String idToken, String fileName) {
        try {
            HttpRequest databaseReq = HttpRequest.newBuilder()
                    .uri(URI.create(storageURL + "o/profilePhotos%2F" + fileName + "?alt=media&token=" + idToken))
                    .GET()
                    .build();
            HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
            System.out.println("Error Code = " + databaseRes.statusCode());
            if (databaseRes.statusCode() == 200) {
                return databaseRes.body().substring(1, databaseRes.body().length()-1);  // Remove quotes around URL
            }
        } catch (ExecutionException | InterruptedException err) {
            err.printStackTrace();
        }
        return "";
    }

    public static ArrayList<JsonObject> getUsers(String idToken) {
        ArrayList<JsonObject> users = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "users.json?auth=" + idToken))
                    .GET()
                    .build();
            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();

            if (response.statusCode() == 200 && !response.body().contentEquals("null")) {
                JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                for (String key : result.keySet()) {
                    users.add(result.get(key).getAsJsonObject());
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return users;
    }
}