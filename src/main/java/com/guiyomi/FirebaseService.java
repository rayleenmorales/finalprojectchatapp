package com.guiyomi;

import java.net.URL;
import java.net.URI;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path; 
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

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
            // Log the error response
            Scanner scanner = new Scanner(connection.getErrorStream());
            String errorResponse = scanner.useDelimiter("\\A").next();
            scanner.close();
            System.out.println("Sign-in failed. Response Code: " + responseCode);
            System.out.println("Error Response: " + errorResponse);  // Log the full error message from Firebase
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
            .put("loggedIn", new JSONObject().put("booleanValue", false))
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

    //method to initialize messages subcollection upon sign up
    public void createMessagesSubcollection(String userId, String userToken) throws Exception {
        // Set up the document path and URL for the Firestore request
        String userIdCombination = userId + "_" + userId;
        String firestoreUrl = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s/messages?documentId=%s",
                                            FIREBASE_PROJECT_ID, userId, userIdCombination);
        URI uri = URI.create(firestoreUrl);
        URL url = uri.toURL();

        System.out.println("Firestore Messages URL: " + firestoreUrl);
    
        HttpURLConnection connection = null;
    
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + userToken);
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
    
            // Create the JSON payload for the new document
            String timeStamp = Instant.now().toString();
            JSONObject document = new JSONObject();
            JSONObject fields = new JSONObject();
    
            // Add lastUpdated field
            fields.put("lastUpdated", new JSONObject().put("stringValue", timeStamp));
    
            // Add participants array
            JSONArray participantsArray = new JSONArray();
            participantsArray.put(new JSONObject().put("stringValue", userId));
            participantsArray.put(new JSONObject().put("stringValue", userId));
            fields.put("participants", new JSONObject().put("arrayValue", new JSONObject().put("values", participantsArray)));
    
            // Add fields to document
            document.put("fields", fields);
    
            // Write data to the output stream
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = document.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
    
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                System.out.println("Messages subcollection created successfully.");
                initializeFirstMessage(userId, userToken);

            } else {
                System.out.println("Failed to create messages subcollection in Firestore. Response Code: " + responseCode);
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorResponse = errorReader.lines().collect(Collectors.joining());
                System.out.println("Error Response: " + errorResponse);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Method to initialize messages subcollection upon sign up with the first welcome message
    public void initializeFirstMessage(String userId, String userToken) throws Exception {
        // Set up the document path and URL for the Firestore request
        String userIdCombination = userId + "_" + userId;
        String firestoreUrl = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s/messages/%s/chat",
                                            FIREBASE_PROJECT_ID, userId, userIdCombination);
        URI uri = URI.create(firestoreUrl);
        URL url = uri.toURL();

        System.out.println("Firestore Messages URL: " + firestoreUrl);

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + userToken);
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Create JSON payload for the welcome message
            String timeStamp = Instant.now().toString();
            JSONObject document = new JSONObject();
            JSONObject fields = new JSONObject();

            // Add sender, receiver, text, and timestamp fields
            fields.put("sender", new JSONObject().put("stringValue", userId));
            fields.put("receiver", new JSONObject().put("stringValue", userId));
            fields.put("text", new JSONObject().put("stringValue", "Welcome to Ka-Talk!"));
            fields.put("timestamp", new JSONObject().put("stringValue", timeStamp));

            // Add fields to document
            document.put("fields", fields);

            // Write data to the output stream
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = document.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                System.out.println("Welcome message created successfully.");
            } else {
                System.out.println("Failed to create chat subcollection in Firestore. Response Code: " + responseCode);
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorResponse = errorReader.lines().collect(Collectors.joining());
                System.out.println("Error Response: " + errorResponse);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    // Method to handle user sign-in (login)
    public Map<String, String> signIn(String email, String password) throws Exception {
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
            // Read the response
            Scanner scanner = new Scanner(connection.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            // Parse the response and extract the ID token and UID
            JSONObject jsonResponse = new JSONObject(response);
            String idToken = jsonResponse.getString("idToken");  // ID token
            String uid = jsonResponse.getString("localId");      // UID of the authenticated user
            System.out.println("Login successful. UID: " + uid + ", ID Token: " + idToken);

            // Check Firestore for the user's login status
            if (isUserLoggedIn(uid, idToken)) {
                System.out.println("Login attempt denied. User is already logged in.");
                return null;  // Prevent login
            }

            System.out.println("Login successful. UID: " + uid + ", ID Token: " + idToken);

            // Set the user as logged in
            setUserLoggedInStatus(uid, true, idToken);

            // Create a Map to hold the ID token and UID
            Map<String, String> authData = new HashMap<>();
            authData.put("uid", uid);
            authData.put("idToken", idToken);

            connection.disconnect();
            return authData;
        } else {
            System.out.println("Login failed. Response Code: " + responseCode);
            connection.disconnect();
            return null;
        }
    }

    // Method to check if user is already logged in
    public boolean isUserLoggedIn(String userId, String idToken) throws Exception {
        String firestoreUrl = String.format("https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/users/" + userId);
        URI uri = URI.create(firestoreUrl);
        URL url = uri.toURL();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + idToken);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Scanner scanner = new Scanner(connection.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            JSONObject jsonResponse = new JSONObject(response);
            JSONObject fields = jsonResponse.getJSONObject("fields");
            boolean loggedIn = fields.getJSONObject("loggedIn").getBoolean("booleanValue");

            connection.disconnect();
            return loggedIn;
        } else {
            System.out.println("Failed to check user login status. Response Code: " + responseCode);
            connection.disconnect();
            return false;
        }
    }

    // Method to update the user's loggedIn status in Firestore without overwriting other fields
    public void setUserLoggedInStatus(String userId, boolean loggedIn, String idToken) throws Exception {
        // Step 1: Fetch the current document
        String firestoreUrlGet = String.format("https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/users/" + userId);
        URI uriGet = URI.create(firestoreUrlGet);
        URL urlGet = uriGet.toURL();

        HttpURLConnection connectionGet = (HttpURLConnection) urlGet.openConnection();
        connectionGet.setRequestMethod("GET");  // GET request to fetch the document
        connectionGet.setRequestProperty("Authorization", "Bearer " + idToken);
        connectionGet.setRequestProperty("Accept", "application/json");

        int responseCodeGet = connectionGet.getResponseCode();
        if (responseCodeGet == HttpURLConnection.HTTP_OK) {
            Scanner scanner = new Scanner(connectionGet.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            // Parse the response to get the existing document
            JSONObject jsonResponse = new JSONObject(response);
            JSONObject fields = jsonResponse.getJSONObject("fields");

            // Step 2: Update the loggedIn field in the existing document
            fields.put("loggedIn", new JSONObject().put("booleanValue", loggedIn));

            // Step 3: Write the updated document back to Firestore using POST with an override for PATCH
            String firestoreUrlPost = String.format("https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/users/" + userId);
            URI uriPost = URI.create(firestoreUrlPost);
            URL urlPost = uriPost.toURL();

            HttpURLConnection connectionPost = (HttpURLConnection) urlPost.openConnection();
            connectionPost.setRequestMethod("POST");  // Using POST to simulate PATCH
            connectionPost.setRequestProperty("Authorization", "Bearer " + idToken);
            connectionPost.setRequestProperty("Content-Type", "application/json; utf-8");
            connectionPost.setRequestProperty("X-HTTP-Method-Override", "PATCH");  // Override POST to act as PATCH
            connectionPost.setRequestProperty("Accept", "application/json");
            connectionPost.setDoOutput(true);

            // Send the modified document back to Firestore
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("fields", fields);

            try (OutputStream os = connectionPost.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCodePost = connectionPost.getResponseCode();
            if (responseCodePost == HttpURLConnection.HTTP_OK) {
                System.out.println("User loggedIn status updated successfully.");
            } else {
                System.out.println("Failed to update user loggedIn status. Response Code: " + responseCodePost);
            }

            connectionPost.disconnect();
        } else {
            System.out.println("Failed to fetch user document. Response Code: " + responseCodeGet);
        }

        connectionGet.disconnect();
    }

    // Method to fetch current user from Firestore
    public void fetchUserDetails(String userId, String idToken) throws Exception {
        String firestoreUrl = String.format("https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/users/" + userId);
        URI uri = URI.create(firestoreUrl);
        URL url = uri.toURL();

        System.out.println("Firestore URL: " + firestoreUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + idToken);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Read the response
            Scanner scanner = new Scanner(connection.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            // Parse the JSON response to extract user data
            JSONObject jsonResponse = new JSONObject(response);
            JSONObject fields = jsonResponse.getJSONObject("fields");
            String firstName = fields.getJSONObject("firstName").getString("stringValue");
            String lastName = fields.getJSONObject("lastName").getString("stringValue");
            String profilePhotoUrl = fields.getJSONObject("profilePhotoUrl").getString("stringValue");
            String userName = firstName + " " + lastName;

            UserSession.startSession(userId, userName, profilePhotoUrl, idToken);
            System.out.println("User session initialized for: " + userName);

        } else {
            System.out.println("Failed to fetch user details. Response Code: " + responseCode);
        }

        connection.disconnect();
    }

    public boolean sendMessage(Map<String, Object> messageData) {
    String url2 = "https://firestore.googleapis.com/v1/projects/katalk-db42a/databases/(default)/documents/messages"; // Ensure this path is correct
    try {
        URI uri = URI.create(url2);
        URL obj = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + UserSession.getIdToken());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        // Construct the JSON payload
        JSONObject json = new JSONObject();
        JSONObject fields = new JSONObject();
        
        // Iterate through the message data and construct fields
        for (Map.Entry<String, Object> entry : messageData.entrySet()) {
            JSONObject fieldValue = new JSONObject();
            // Handle different types (you may need to adjust this based on your actual data types)
            if (entry.getValue() instanceof String) {
                fieldValue.put("stringValue", entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                fieldValue.put("integerValue", entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                fieldValue.put("booleanValue", entry.getValue());
            } else {
                // Handle other data types as necessary
            }
            fields.put(entry.getKey(), fieldValue);
        }
        
        json.put("fields", fields);

        // Write JSON data to request body
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Message sent successfully.");
        } else {
            // Handle failure response
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            System.out.println("POST request failed with response code " + responseCode + ": " + errorResponse.toString());
        }
        return true;
    } catch (Exception e) {
        e.printStackTrace();
    }
    return false;
}

    public String sendHttpGetRequest(String urlString) throws Exception {
        // Create a URL object
        URI uri = URI.create(urlString);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set request method to GET
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + UserSession.getIdToken());

        // Get response code and handle response
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Read response from the input stream
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } else {
            throw new Exception("GET request failed. Response code: " + responseCode);
        }
    }
     

}
