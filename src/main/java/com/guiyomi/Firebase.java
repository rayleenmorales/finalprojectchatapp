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

            // Step 2: If upload is successful, retrieve the download token
            if (storageRes.statusCode() == 200) {
                JsonObject resultData = JsonParser.parseString(storageRes.body()).getAsJsonObject();
                String downloadToken = resultData.get("downloadTokens").getAsString();

                // Step 3: Construct and return the complete download URL
                return storageURL + "o/profilePhotos%2F" + profilePhoto.getName() + "?alt=media&token=" + downloadToken;
            }
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
                
                // Get the username and profile url from the database
                // Check the isLogged status from the database
                HttpRequest databaseReq = HttpRequest.newBuilder()
                        .uri(URI.create(databaseURL + "users/" + localId + ".json?auth=" + idToken))
                        .GET().build();
                HttpResponse<String> databaseRes = client.sendAsync(databaseReq, HttpResponse.BodyHandlers.ofString()).get();
    
                if (databaseRes.statusCode() == 200) {
                    JsonObject databaseResultData = JsonParser.parseString(databaseRes.body()).getAsJsonObject();
                    String username = databaseResultData.get("username").getAsString();
                    String profileURL = databaseResultData.get("profilePhotoURL").getAsString();

                    System.out.println("User profile url when signing in: " + profileURL);
                    return new User(idToken, localId, username, profileURL);
                    
                
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

    // Method to upload file to Firebase Storage
    public static String uploadFile(String idToken, File file) {
        String absolutePath = file.getAbsolutePath();
        Path filePath = Paths.get(absolutePath);
    
        try {
            // Step 1: Prepare and send the upload request to Firebase Storage
            HttpRequest storageReq = HttpRequest.newBuilder()
                .uri(URI.create(storageURL + "o?name=sentMedia/" + file.getName()))
                .POST(HttpRequest.BodyPublishers.ofFile(filePath))
                .header("Authorization", "Firebase " + idToken)
                .build();
            HttpResponse<String> storageRes = client.sendAsync(storageReq, HttpResponse.BodyHandlers.ofString()).get();
            System.out.println("Upload File Response: " + storageRes.statusCode());
    
            // Step 2: If upload is successful, retrieve the download token
            if (storageRes.statusCode() == 200) {
                JsonObject resultData = JsonParser.parseString(storageRes.body()).getAsJsonObject();
                String downloadToken = resultData.get("downloadTokens").getAsString();
                
                String returnURL = storageURL + "o/sentMedia%2F" + file.getName() + "?alt=media&token=" + downloadToken;
                // Step 3: Construct and return the complete download URL
                return returnURL;
            }
        } catch (ExecutionException | InterruptedException | FileNotFoundException err) {
            err.printStackTrace();
        }
        return "";
    };

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

    // GAMES

    // FLAPPY BIRD: Fetch the high score for a user from Firebase
    public static int fetchHighScore(String idToken, String localId) {
        int highScore = 0; // Default value if no high score is found
    
        try {
            // Build the request to fetch the high score for the userId
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "games/flappybird/highscores/" + localId + ".json?auth=" + idToken))
                    .GET()
                    .build();
    
            // Send the request asynchronously
            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();
    
            // Check the response status
            if (response.statusCode() == 200 && !response.body().equals("null")) {
                // Parse the JSON response to get the score
                JsonObject resultData = JsonParser.parseString(response.body()).getAsJsonObject();
                highScore = resultData.get("highscore").getAsInt();
                System.out.println("Fetched high score: " + highScore);
            } else {
                System.out.println("No high score found for user: " + localId);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    
        return highScore;
    }
    
    // TIC TAC TOE: Checks if a game exists in Firebase
    public static boolean doesGameExist(String gameId, String tokenId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(databaseURL + "games/Tictactoe/" + gameId + ".json?auth=" + tokenId))
                    .GET()
                    .build();
            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();
    
            return response.statusCode() == 200 && !response.body().equals("null");
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    // TIC TAC TOE: Load the game state from Firebase
    public static JsonObject loadGameStateFromFirebase(String gameId, String authToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(databaseURL + "games/Tictactoe/" + gameId + ".json?auth=" + authToken))
                .GET()
                .build();
    
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            if (response.statusCode() == 200 && !response.body().equals("null")) {
                JsonObject gameState = JsonParser.parseString(response.body()).getAsJsonObject();
                return gameState;
            } else {
                System.out.println("No existing game state found in Firebase.");
                return null;
            }
    
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static void initializeNewGameInFirebase(String gameId, String tokenId) {
        try{
            JsonObject gameState = new JsonObject();
            JsonObject boardData = new JsonObject();

            // Initialize an empty board
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    boardData.addProperty(i + "_" + j, 0); // Start with all cells empty
                }
            }

            // Add all initial game state properties
            gameState.add("board", boardData);
            gameState.addProperty("currentTurn", "X"); // X starts the game
            gameState.addProperty("gameDone", false);
            gameState.addProperty("winner", -1);
            gameState.addProperty("player1wins", 0);
            gameState.addProperty("player2wins", 0);

            String gameStateJson = gameState.toString();
    
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(databaseURL + "games/Tictactoe/" + gameId + ".json?auth=" + tokenId))
                .PUT(HttpRequest.BodyPublishers.ofString(gameStateJson))
                .header("Content-Type", "application/json")
                .build();
    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            if (response.statusCode() == 200) {
                System.out.println("New game initialized successfully.");
            } else {
                System.out.println("Failed to initialize new game: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Firebase method to send updated game state to Realtime Database
    public static void sendUpdatedGameState(String gameId, JsonObject gameState, String authToken) {
        String firebaseUrl = databaseURL + "/games/Tictactoe/" + gameId + ".json?auth=" + authToken;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(firebaseUrl))
                .PUT(HttpRequest.BodyPublishers.ofString(gameState.toString()))
                .header("Content-Type", "application/json")
                .build();

            HttpClient client = HttpClient.newHttpClient();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                if (response.statusCode() == 200) {
                    System.out.println("Game state updated successfully.");
                } else {
                    System.err.println("Failed to update game state: " + response.statusCode());
                }
            }).exceptionally(e -> {
                System.err.println("Error updating game state: " + e.getMessage());
                return null;
            });
        } catch (Exception e) {
            System.err.println("Exception in sendUpdatedGameState: " + e.getMessage());
        }
    }


    // FLAPPY BIRD: Save the high score for a user to Firebase
    public static void saveHighScore (String idToken, String localId, String username, String profileURL, int highscoreValue) throws FirebaseAuthException {
        try {
            JsonObject highscoreData = new JsonObject();
            highscoreData.addProperty("userId", localId);
            highscoreData.addProperty("highscore", highscoreValue);
            highscoreData.addProperty("username", username);
            highscoreData.addProperty("profileURL", profileURL);

            System.out.println("profileURL when sending to firebase: " + profileURL);
        
            HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create(databaseURL + "games/flappybird/highscores/" + localId + ".json?auth=" + idToken))
                .PUT(HttpRequest.BodyPublishers.ofString(highscoreData.toString()))
                .header("Content-Type", "application/json")
                .build();
        
            HttpResponse<String> putResponse = client.sendAsync(putRequest, HttpResponse.BodyHandlers.ofString()).get();
        
                if (putResponse.statusCode() == 200) {
                    System.out.println("Highscore updated successfully for user: " + localId);
                } else {
                    System.out.println("Failed to update highscore: " + putResponse.body());
                    throw new FirebaseAuthException("Failed to update highscore"); // Or handle the error differently
                }
            } catch (ExecutionException | InterruptedException err) {
                err.printStackTrace();
                throw new FirebaseAuthException("Error updating highscore"); // Or handle the error differently
            }
    }

    // FLAPPY BIRD: Fetch the high scores for all users from Firebase
    public static ArrayList<JsonObject> fetchAllHighScores(String idToken) {
        ArrayList<JsonObject> highScores = new ArrayList<>();
    
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(databaseURL + "games/flappybird/highscores.json?auth=" + idToken))
                .GET()
                .build();
    
            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();
    
            if (response.statusCode() == 200 && !response.body().equals("null")) {
                JsonObject resultData = JsonParser.parseString(response.body()).getAsJsonObject();
                for (String key : resultData.keySet()) {
                    JsonObject highScoreData = resultData.getAsJsonObject(key);
                    highScores.add(highScoreData);
                }
            } else {
                System.out.println("No high scores found in Firebase.");
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    
        return highScores;
    }
}
