package com.guiyomi;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.net.URI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;


public class ChatMainController extends Application {

    

    @FXML
    private VBox userContainer;


    @FXML
    private ScrollPane messageScrollPane;


    @FXML
    private VBox messageContainer;


    @FXML
    private TextField messageField;


    @FXML
    private Circle currentUserProfile;


    @FXML
    private Circle selectedUserProfile;


    @FXML
    private Label currentUserLabel;


    @FXML
    private Label selectedUserLabel;


    @FXML
    private Button logoutButton;


    FirebaseService firebaseService = new FirebaseService();
    private Timer messagePollingTimer;

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("MainChat.fxml"));
            primaryStage.setTitle("KaTalk");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        primaryStage.setOnCloseRequest(e ->{
            e.consume();
            LogOutConfirmation(primaryStage);
        });
    }


    @FXML
    public void initialize() {
        // Load user's name and profile picture
        if (UserSession.isLoggedIn()) {
            // Load user data into the FXML elements
            currentUserLabel.setText(UserSession.getUserName());
        
            // Load the profile photo from the URL
            String photoUrl = UserSession.getUserPhotoUrl();
            System.out.println("PhotoUrl: " + photoUrl);
        
            if (photoUrl != null && !photoUrl.isEmpty()) {
                try {
                    // Load the image from the URL
                    Image profilePhoto = new Image(photoUrl);
        
                    // Set the profile photo directly into the Circle (currentUserProfile)
                    currentUserProfile.setFill(new ImagePattern(profilePhoto));
                } catch (Exception e) {
                    System.out.println("Error loading profile photo: " + e.getMessage());
                }
            } else {
                System.out.println("Invalid profile photo URL.");
            }
        }


        // Load users when the controller initializes
        populateUserListWithHttp();
    }


   @FXML
    public void handleLogOutButton(ActionEvent event) throws Exception {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        LogOutConfirmation(stage);
    }

    public void LogOutConfirmation(Stage stage) {
        Dialog<ButtonType> dialog = new Dialog<>();
        DialogPane dPane = new DialogPane();
        VBox content = new VBox(10);
        dialog.setTitle("Logout Confirmation");
        content.getChildren().add(new Label("Are you sure you want to log out?"));
        dPane.setContent(content);
        dPane.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
        dPane.setGraphic(null);
        dialog.setDialogPane(dPane);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES){
                System.out.println("Proceed Logout.");
                
                try{
                    //handle logout here
                    String userId = UserSession.getUserId();
                    String idToken = UserSession.getIdToken(); // Replace with the actual token

                    firebaseService.setUserLoggedInStatus(userId, false, idToken);

                    UserSession.endSession();
                    SelectedUser.endSession();

                    Parent root = FXMLLoader.load(getClass().getResource("LOGIN PAGE.fxml"));
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.show();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Logout Failed.");
                }
                
            }
            if (response == ButtonType.NO){
                System.out.println("Logout Canceled.");
            }
        });
        
    }


    private void populateUserListWithHttp() {
        String url = "https://firestore.googleapis.com/v1/projects/katalk-db42a/databases/(default)/documents/users";
    
        try {
            String jsonResponse = sendHttpGetRequest(url);
            JSONObject jsonResponseObject = new JSONObject(jsonResponse);
            JSONArray documents = jsonResponseObject.getJSONArray("documents");
    
            // Clear the VBox before adding new users
            Platform.runLater(() -> userContainer.getChildren().clear());
    
            // Iterate over the documents array
            for (int i = 0; i < documents.length(); i++) {
                JSONObject document = documents.getJSONObject(i);
                String userId = document.getString("name").split("/")[document.getString("name").split("/").length - 1]; // Extract userId from Firestore document path
                
                JSONObject fields = document.getJSONObject("fields");
    
               
                // Parse the JSON response to extract user data
           
                String firstName = fields.getJSONObject("firstName").getString("stringValue");
                String lastName = fields.getJSONObject("lastName").getString("stringValue");
                Boolean isLoggedIn = fields.getJSONObject("loggedIn").getBoolean("booleanValue");
                String profilePhotoUrl = fields.getJSONObject("profilePhotoUrl").getString("stringValue");
                String fullName = firstName + " " + lastName;

                // Create a new Pane for each user
                Pane userPane = createUserPane(fullName, "Chat: Lorem Ipsum", profilePhotoUrl, isLoggedIn);
    
                // Set the click event to initialize SelectedUser when a user is selected
                userPane.setOnMouseClicked(event -> selectUser(userId, profilePhotoUrl, fullName));
    
                // Add user pane to the user container
                Platform.runLater(() -> userContainer.getChildren().add(userPane));
            }
        } catch (JSONException e) {
            e.printStackTrace(); // Handle the exception appropriately
        } catch (Exception e) {
            e.printStackTrace(); // Handle any other exceptions
        }
    }
    
    private Pane createUserPane(String userName, String chatPreview, String profilePictureUrl, boolean isLoggedIn) {
        Pane userPane = new Pane();
        userPane.setPrefSize(310.0, 79.0);
        userPane.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: lightgray; -fx-border-width: 1;");
    
        // Profile picture placeholder
        Circle profilePic = new Circle(31.0, Color.RED); // Placeholder color
        profilePic.setStroke(Color.BLACK);
        profilePic.setStrokeType(StrokeType.INSIDE);
        profilePic.setLayoutX(45.0);
        profilePic.setLayoutY(40.0);
    
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    Image profilePhoto = new Image(profilePictureUrl, false); // Load image in background
                    Platform.runLater(() -> profilePic.setFill(new ImagePattern(profilePhoto)));
                } catch (Exception e) {
                    System.out.println("Error loading profile photo: " + e.getMessage());
                }
            }).start();
        }
    
        // Name label
        Label nameLabel = new Label(userName);
        nameLabel.setLayoutX(86.0);
        nameLabel.setLayoutY(10.0);  // Moved up to fit all elements
        nameLabel.setPrefHeight(20.0);
        nameLabel.setFont(new Font("Arial Rounded MT Bold", 16.0));
    
        // Chat preview label
        Label chatLabel = new Label(chatPreview);
        chatLabel.setLayoutX(86.0);
        chatLabel.setLayoutY(35.0);  // Positioned below the name
        chatLabel.setPrefHeight(20.0);
        chatLabel.setFont(new Font("Arial", 14.0));
        chatLabel.setTextFill(Color.DARKGRAY);
    
        // Active status label
        String status = isLoggedIn ? "Active" : "Offline";
        Label activeLabel = new Label(status);
        activeLabel.setLayoutX(86.0);  // Positioned directly under the name and chat
        activeLabel.setLayoutY(55.0);  // Moved down to prevent overlap
        activeLabel.setFont(new Font("Arial", 12.0));
        activeLabel.setTextFill(isLoggedIn ? Color.GREEN : Color.GRAY);
    
        // Add all elements to the userPane
        userPane.getChildren().addAll(profilePic, nameLabel, chatLabel, activeLabel);
        return userPane;
    }
    

    public void selectUser(String userId, String profilePicUrl, String fullName) {
        selectedUserLabel.setText(fullName); // Update the label with the selected user's name

        // Initialize SelectedUser with the selected user's data
        SelectedUser.startSession(userId, fullName, profilePicUrl);

        // If a profile picture URL is provided, load and display it
        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
            try {
                Image profileImage = new Image(profilePicUrl);
                Platform.runLater(() -> selectedUserProfile.setFill(new ImagePattern(profileImage)));
            } catch (Exception e) {
                System.out.println("Error loading selected user's profile picture: " + e.getMessage());
            }
        } else {
            // Set a default image or clear the ImageView if no URL is available
            Platform.runLater(() -> selectedUserProfile.setFill(new ImagePattern(null)));
        }
        
        String currentUserId = UserSession.getUserId();

        Platform.runLater(() -> messageContainer.getChildren().clear());

        try {
            loadConversationMessages(currentUserId, userId);
        } catch (Exception e) {
            System.out.println("Error loading conversation messages: " + e.getMessage());
            e.printStackTrace();
        }

        // Start polling for new messages every 5 seconds
        startMessagePolling(currentUserId, userId);
        
    }

    private void startMessagePolling(String senderId, String receiverId) {
    if (messagePollingTimer != null) {
        messagePollingTimer.cancel();
    }
    messagePollingTimer = new Timer(true);
    messagePollingTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            Platform.runLater(() -> {
                try {
                    loadConversationMessages(senderId, receiverId);
                } catch (Exception e) {
                    System.out.println("Failed to poll messages: " + e.getMessage());
                }
            });
        }
    }, 0, 5000); // Poll every 5 seconds
}
   
    private String sendHttpGetRequest(String urlString) throws Exception {
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

    private static final String FIREBASE_PROJECT_ID = "katalk-db42a"; 

    @FXML
    public void handleSendButton(ActionEvent event) throws Exception {
        String messageText = messageField.getText();
        if (!messageText.isEmpty()) {
            String senderId = UserSession.getUserId();  // The current logged-in user (UserA)
            String receiverId = SelectedUser.getUserId();  // The selected user you're chatting with (UserB)
    
            // Generate the document ID based on sender and receiver
            String userIdCombinationSender = senderId + "_" + receiverId;  // UserA's version of the conversation
            String userIdCombinationReceiver = receiverId + "_" + senderId;  // UserB's version of the conversation
    
            // Check if the conversation exists for both users
            boolean conversationExists = checkIfConversationExists(senderId, userIdCombinationSender);
            if (!conversationExists) {
                // Create the conversation document for both users (first-time conversation)
                createConversationDocument(senderId, receiverId, userIdCombinationSender, userIdCombinationReceiver);
            }
    
            // Proceed with sending the message
            sendMessageToUsers(senderId, receiverId, userIdCombinationSender, userIdCombinationReceiver, messageText);
            
            // Dynamically append the message to the chat UI for the sender
            Pane messagePane = createMessagePane(senderId, messageText, Instant.now().toString(), true); // `true` indicates the sender
            Platform.runLater(() -> messageContainer.getChildren().add(messagePane));  // Append to the UI
    
            messageField.setText("");
        }
    }

    private void loadConversationMessages(String senderId, String receiverId) throws Exception {
    String userIdCombination = senderId + "_" + receiverId;
    String firestoreChatUrl = String.format(
        "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s/messages/%s/chat",
        FIREBASE_PROJECT_ID, senderId, userIdCombination
    );

    URI uri = URI.create(firestoreChatUrl);
    URL url = uri.toURL();
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", "Bearer " + UserSession.getIdToken());
    connection.setRequestProperty("Accept", "application/json");

    int responseCode = connection.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String jsonResponse = in.lines().collect(Collectors.joining());
        in.close();

        JSONObject jsonResponseObject = new JSONObject(jsonResponse);
        if (!jsonResponseObject.has("documents")) {
            System.out.println("No messages found in this conversation.");
            return;
        }

        JSONArray documents = jsonResponseObject.getJSONArray("documents");

        List<JSONObject> messageList = new ArrayList<>();
        for (int i = 0; i < documents.length(); i++) {
            messageList.add(documents.getJSONObject(i));
        }

        // Sort messages by timestamp
        messageList.sort((a, b) -> {
            try {
                String timeA = a.getJSONObject("fields").getJSONObject("timestamp").getString("stringValue");
                String timeB = b.getJSONObject("fields").getJSONObject("timestamp").getString("stringValue");
                return Instant.parse(timeA).compareTo(Instant.parse(timeB));
            } catch (JSONException e) {
                return 0;
            }
        });

        // Clear container and load sorted messages
        Platform.runLater(() -> messageContainer.getChildren().clear());
        for (JSONObject document : messageList) {
            JSONObject fields = document.getJSONObject("fields");

            String sender = fields.getJSONObject("sender").getString("stringValue");
            String messageText = fields.getJSONObject("text").getString("stringValue");
            String timestamp = fields.getJSONObject("timestamp").getString("stringValue");

            boolean isSender = sender.equals(senderId);

            Pane messagePane = createMessagePane(sender, messageText, timestamp, isSender);
            Platform.runLater(() -> messageContainer.getChildren().add(messagePane));
        }
    } else {
        System.out.println("Failed to load messages. Response Code: " + responseCode);
    }
}

    
    

    private Pane createMessagePane(String sender, String messageText, String timestamp, boolean isSender) {
        Pane messagePane = new Pane();
        messagePane.setPrefSize(300.0, 60.0);
        messagePane.setStyle("-fx-padding: 10;");
    
        // Create Circle for placeholder profile picture
        Circle profilePic = new Circle(20.0);
        profilePic.setStroke(Color.BLACK);
        profilePic.setStrokeType(StrokeType.INSIDE);
        profilePic.setLayoutX(isSender ? 260.0 : 20.0);  // Position depending on sender/receiver
        profilePic.setLayoutY(30.0);
        profilePic.setFill(isSender ? Color.BLUE : Color.GRAY);  // Placeholder colors
    
        // Create message label
        Label messageLabel = new Label(messageText);
        messageLabel.setWrapText(true);
        messageLabel.setLayoutX(isSender ? 120.0 : 50.0);  // Adjust based on sender/receiver
        messageLabel.setLayoutY(20.0);
        messageLabel.setFont(new Font("Arial", 14.0));
        messageLabel.setStyle(isSender ? "-fx-background-color: lightblue; -fx-padding: 5;" : "-fx-background-color: lightgray; -fx-padding: 5;");
    
        // Create timestamp label
        Label timestampLabel = new Label(formatTimestamp(timestamp));
        timestampLabel.setLayoutX(isSender ? 120.0 : 50.0);
        timestampLabel.setLayoutY(50.0);
        timestampLabel.setFont(new Font("Arial", 10.0));
        timestampLabel.setTextFill(Color.GRAY);
    
        // Add elements to the message pane
        messagePane.getChildren().addAll(profilePic, messageLabel, timestampLabel);
    
        return messagePane;
    }

    private String formatTimestamp(String timestamp) {
        Instant instant = Instant.parse(timestamp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
    
    // Method to check if the conversation already exists
    private boolean checkIfConversationExists(String userId, String conversationId) throws Exception {
        // Set up the Firestore URL to check if the conversation exists
        String firestoreUrl = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s/messages/%s",
                                            FIREBASE_PROJECT_ID, userId, conversationId);
        URI uri = URI.create(firestoreUrl);
        URL url = uri.toURL();
    
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + UserSession.getIdToken());
            connection.setRequestProperty("Accept", "application/json");
    
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Conversation exists
                return true;
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // Conversation does not exist
                return false;
            } else {
                throw new Exception("Unexpected response code: " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    // Method to create a conversation document for both users if it's their first time talking
    private void createConversationDocument(String senderId, String receiverId, String userIdCombinationSender, String userIdCombinationReceiver) throws Exception {
        // Create conversation for sender (UserA)
        String firestoreUrlSender = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s/messages?documentId=%s",
                                                  FIREBASE_PROJECT_ID, senderId, userIdCombinationSender);
    
        // Create conversation for receiver (UserB)
        String firestoreUrlReceiver = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s/messages?documentId=%s",
                                                    FIREBASE_PROJECT_ID, receiverId, userIdCombinationReceiver);
    
        URI uriSender = URI.create(firestoreUrlSender);
        URI uriReceiver = URI.create(firestoreUrlReceiver);
    
        URL urlSender = uriSender.toURL();
        URL urlReceiver = uriReceiver.toURL();
    
        // Create the JSON payload for the new conversation
        String timeStamp = Instant.now().toString();
        JSONObject conversationDocument = new JSONObject();
        JSONObject fields = new JSONObject();
    
        // Add participants array
        JSONArray participantsArray = new JSONArray();
        participantsArray.put(new JSONObject().put("stringValue", senderId));
        participantsArray.put(new JSONObject().put("stringValue", receiverId));
        fields.put("participants", new JSONObject().put("arrayValue", new JSONObject().put("values", participantsArray)));
    
        // Add lastUpdated field
        fields.put("lastUpdated", new JSONObject().put("stringValue", timeStamp));
    
        // Add fields to the document
        conversationDocument.put("fields", fields);
    
        // Create conversation for sender
        sendFirestoreRequest(urlSender, conversationDocument);
    
        // Create conversation for receiver
        sendFirestoreRequest(urlReceiver, conversationDocument);
    }
    
    // Helper method to send Firestore requests (POST)
    private void sendFirestoreRequest(URL url, JSONObject document) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + UserSession.getIdToken());
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
    
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = document.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
    
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorResponse = errorReader.lines().collect(Collectors.joining());
                System.out.println("Error Response: " + errorResponse);
                throw new Exception("Failed to create document. Response Code: " + responseCode);
            }
    
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    // Method to send the actual message once the conversation is initialized or already exists
    private void sendMessageToUsers(String senderId, String receiverId, String userIdCombinationSender, String userIdCombinationReceiver, String messageText) throws Exception {
        // Firestore URLs for the chat subcollection (for both users)
        String firestoreChatUrlSender = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s/messages/%s/chat",
                                                      FIREBASE_PROJECT_ID, senderId, userIdCombinationSender);
        String firestoreChatUrlReceiver = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/users/%s/messages/%s/chat",
                                                        FIREBASE_PROJECT_ID, receiverId, userIdCombinationReceiver);
    
        URI chatUriSender = URI.create(firestoreChatUrlSender);
        URI chatUriReceiver = URI.create(firestoreChatUrlReceiver);
    
        URL chatUrlSender = chatUriSender.toURL();
        URL chatUrlReceiver = chatUriReceiver.toURL();
    
        // Create the message JSON payload
        String timeStamp = Instant.now().toString();
        JSONObject messageDocument = new JSONObject();
        JSONObject fields = new JSONObject();
    
        // Add sender, receiver, text, and timestamp fields
        fields.put("sender", new JSONObject().put("stringValue", senderId));
        fields.put("receiver", new JSONObject().put("stringValue", receiverId));
        fields.put("text", new JSONObject().put("stringValue", messageText));
        fields.put("timestamp", new JSONObject().put("stringValue", timeStamp));
    
        // Add fields to the message document
        messageDocument.put("fields", fields);
    
        // Send the message for both users
        sendFirestoreRequest(chatUrlSender, messageDocument);
        sendFirestoreRequest(chatUrlReceiver, messageDocument);
    }
   
}
