package com.guiyomi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;

import com.google.gson.JsonObject;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ChatMainController extends Application {

    @FXML
    private BorderPane chatMainScene;


    @FXML
    private VBox userContainer;


    @FXML
    private Circle selectedUserSideProfile;


    @FXML
    private Label selectedUserSideLabel;


    @FXML
    private VBox userBarContainer;


    @FXML
    private ScrollPane userScrollPane;


    @FXML
    private ScrollPane messageScrollPane;


    @FXML
    private VBox messageContainer;


    @FXML
    private TextField messageField;


    @FXML
    private TextField searchField;


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


    private User user;
    private String selectedUserId;
    private AnimationTimer mainLoop;
    private ArrayList<JsonObject> updateUsers = new ArrayList<>();
    private Map<String, Pane> userPaneMap = new HashMap<>();
    private ArrayList<JsonObject> updateMessage = new ArrayList<>();
    private Map<String, Pane> messagePaneMap = new HashMap<>();
    private Map<String, Image> profileImageCache = new HashMap<>();
    private int lastMessageCount = 0; 

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MAINCHAT5.fxml"));
            Parent root = loader.load();
            
            // Set the controller as user data
            ChatMainController controller = loader.getController();
            root.setUserData(controller);

            primaryStage.setTitle("KaTalk");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            SessionService.LogOutConfirmation(primaryStage, this.user, mainLoop, this);
        });
    }


    @FXML
    public void initialize() {
        chatMainScene.setId("chatMainScene");
        setupSearchListener();

        messageField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    try {
                        handleSendButton(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    public AnimationTimer getMainLoop() {
        return mainLoop;
    }
        

    public void setUser(User user) {
        this.user = user;

        try {
            Image profilePhoto = new Image(this.user.getProfilePhotoURL());
            currentUserProfile.setFill(new ImagePattern(profilePhoto));
        } catch (Exception e) {
            System.out.println("Error loading profile photo: " + e.getMessage());
        }

        currentUserLabel.setText(user.getUserName());
        initializeUpdateLoop();
    }

    public User getUser() {
        return user;
    }

    @FXML
    public void handleLogOutButton(ActionEvent event) throws Exception {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SessionService.LogOutConfirmation(stage, this.user, mainLoop, this);
    }


    //FOR REAL-TIME UPDATES
    private void initializeUpdateLoop() {
        mainLoop = new AnimationTimer() {
            private long lastUserUpdate = 0;
            private long lastMessageUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUserUpdate >= 1_000_000_000) { // 1-second interval
                    lastUserUpdate = now;
                    Thread userUpdateThread = new Thread(userUpdateTask);
                    userUpdateThread.setDaemon(true);
                    userUpdateThread.start();
                }

                if (now - lastMessageUpdate >= 1_000_000_000) {
                    lastMessageUpdate = now;
                    Thread messageUpdateThread = new Thread(messageUpdateTask);
                    messageUpdateThread.setDaemon(true);
                    messageUpdateThread.start();
                }
            }
        };
        mainLoop.start();
    }

    Runnable userUpdateTask = () -> {
        updateUsers = Firebase.getUsers(user.getTokenID());
        Platform.runLater(() -> updateUserContainer(updateUsers));
    };

    Runnable messageUpdateTask = () -> {
        if (user == null || user.getTokenID() == null || selectedUserId == null) {
            return;
        }
    
        // Fetch messages from Firebase for the current conversation
        updateMessage = Firebase.getMessages(user.getTokenID(), getCurrentConversationId());
        
        // Update the message container with new messages
        Platform.runLater(() -> updateMessageContainer(updateMessage));
    };
    
    public void clearUserData() {
        // Clear user object and selected user ID
        this.user = null;
        this.selectedUserId = null;
    
        // Clear user-specific data structures
        userPaneMap.clear();
        messagePaneMap.clear();
        profileImageCache.clear();
        updateUsers.clear();
        updateMessage.clear();
    
        // Reset last message count
        lastMessageCount = 0;
    
        // Clear UI containers
        Platform.runLater(() -> {
            userContainer.getChildren().clear();
            messageContainer.getChildren().clear();
            currentUserLabel.setText("");
            selectedUserLabel.setText("");
            selectedUserProfile.setFill(Color.TRANSPARENT);
            currentUserProfile.setFill(Color.TRANSPARENT);
        });
    }       

    //FOR TEXT MESSAGES (DOESN'T SUPPORT MEDIA YET)
    @FXML
    public void handleSendButton(ActionEvent event) throws Exception {
        String messageText = messageField.getText().trim();

        if (messageText.isEmpty() || selectedUserId == null) {
            // No message to send or no user selected
            return;
        }

        String messageId = generateUniqueMessageId();

        // Create the message object with necessary fields
        JsonObject message = new JsonObject();
        message.addProperty("senderId", user.getLocalID());  // Local user ID as sender
        message.addProperty("receiverId", selectedUserId);   // Selected user as receiver
        message.addProperty("content", messageText);
        message.addProperty("timestamp", System.currentTimeMillis());
        message.addProperty("messageId", messageId);

        Firebase.putMessage(user.getTokenID(), getCurrentConversationId(), message);

        // Clear the message field after sending
        messageField.clear();
    }


    private void updateMessageContainer(ArrayList<JsonObject> newMessages) {
        System.out.println("Updating message container with " + newMessages.size() + " messages.");
    
        // Sort messages by their timestamp in ascending order (oldest to latest)
        newMessages.sort(Comparator.comparingLong(message -> message.get("timestamp").getAsLong()));
    
        boolean newMessageAdded = false; // Track if a new message is added
    
        for (JsonObject messageJson : newMessages) {
            String messageId = messageJson.get("messageId").getAsString();
    
            if (!messagePaneMap.containsKey(messageId)) {
                String senderId = messageJson.get("senderId").getAsString();
                String receiverId = messageJson.get("receiverId").getAsString();
                String content = messageJson.get("content").getAsString();
                String timestamp = messageJson.get("timestamp").getAsString();
    
                System.out.println("Displaying message: " + content + ", Sender: " + senderId + ", Receiver: " + receiverId);
    
                // Determine if the current user is the sender or receiver
                boolean isSender = senderId.equals(user.getLocalID());
                boolean isReceiver = receiverId.equals(user.getLocalID());
    
                if (isSender || isReceiver) {
                    String displayName = isSender ? user.getUserName() : selectedUserLabel.getText();
                    Image profileImage = null;
    
                    // Check if a valid URL exists in the cache; otherwise, leave profileImage as null
                    if (isSender && user.getProfilePhotoURL() != null && profileImageCache.containsKey(user.getProfilePhotoURL())) {
                        profileImage = profileImageCache.get(user.getProfilePhotoURL());
                    } else if (!isSender && selectedUserId != null && profileImageCache.containsKey(selectedUserId)) {
                        profileImage = profileImageCache.get(selectedUserId);
                    }
    
                    // Create the message pane, passing null for profile image if not found
                    Pane messagePane = createMessagePane(displayName, content, profileImageCache, timestamp, profileImage != null ? profileImage.getUrl() : null, isSender);
                    messagePaneMap.put(messageId, messagePane);
                    messageContainer.getChildren().add(messagePane);  // Append to the UI
                    newMessageAdded = true; // A new message was added
                }
            }
        }
    
        // Check if a new message was added and the message count has increased
        if (newMessageAdded && messageContainer.getChildren().size() > lastMessageCount) {
            // Scroll to the bottom of the message container to show the latest messages
            messageScrollPane.layout();
            messageScrollPane.setVvalue(1.0);
        }
    
        // Update lastMessageCount to the current message count
        lastMessageCount = messageContainer.getChildren().size();
    }
    
    
    public Pane createMessagePane(String username, String messageText, Map<String, Image> profileImageCache, String timestamp, String profilePictureUrl, boolean isSender) {
        HBox messageBox = new HBox();
        messageBox.setStyle("-fx-padding: 10");
        messageBox.setFillHeight(true);
        messageBox.setSpacing(10);
        messageBox.setPrefWidth(Double.MAX_VALUE);
        messageBox.setAlignment(isSender ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(messageBox, Priority.ALWAYS);
    
        // Only create profilePic Circle if profilePictureUrl is provided
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            Circle profilePic = new Circle(20.0);
    
            if (profileImageCache.containsKey(profilePictureUrl)) {
                profilePic.setFill(new ImagePattern(profileImageCache.get(profilePictureUrl)));
            } else {
                new Thread(() -> {
                    try {
                        Image profilePhoto = new Image(profilePictureUrl, false);
                        profileImageCache.put(profilePictureUrl, profilePhoto);
                        Platform.runLater(() -> profilePic.setFill(new ImagePattern(profilePhoto)));
                    } catch (Exception e) {
                        System.out.println("Error loading profile photo: " + e.getMessage());
                    }
                }).start();
            }
    
            // Add profilePic to messageBox
            if (isSender) {
                messageBox.getChildren().add(profilePic);
            } else {
                messageBox.getChildren().add(0, profilePic);
            }
        }
    
        VBox messageContent = new VBox();
        messageContent.setSpacing(2);
        messageContent.setMaxWidth(400);
    
        Label messageLabel = new Label(messageText);
        messageLabel.setWrapText(true);
        messageLabel.setFont(new Font("Arial", 14.0));
        messageLabel.setStyle(isSender ? "-fx-background-color: lightblue; -fx-padding: 8; -fx-background-radius: 10;" 
                                       : "-fx-background-color: lightgray; -fx-padding: 8; -fx-background-radius: 10;");
        messageLabel.setMaxWidth(400);
    
        Label timestampLabel = new Label(formatTimestamp(timestamp));
        timestampLabel.setFont(new Font("Arial", 10.0));
        timestampLabel.setTextFill(Color.GRAY);
    
        messageContent.getChildren().addAll(messageLabel, timestampLabel);
    
        if (isSender) {
            messageBox.getChildren().add(0, messageContent);  // Add at the beginning if receiver
            
        } else {
            messageBox.getChildren().add(messageContent);  // Add at the end if sender
        }
        
    
        return messageBox;
    }
    
    

    private static String formatTimestamp(String timestamp) {
        Instant instant = Instant.ofEpochMilli(Long.parseLong(timestamp));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    public void selectUser(String userId, String username, String profileURL) {
        // Clear previous messages for the selected conversation
        messagePaneMap.clear();
        messageContainer.getChildren().clear();
        lastMessageCount = 0;
    
        // Set selected user information
        selectedUserId = userId;
        selectedUserLabel.setText(username);
        selectedUserSideLabel.setText(username);
    
        // Load and set profile image if available, and cache it
        if (profileURL != null && !profileURL.isEmpty()) {
            if (!profileImageCache.containsKey(profileURL)) {
                Image profileImage = new Image(profileURL, false);
                profileImageCache.put(profileURL, profileImage);
            }
            selectedUserProfile.setFill(new ImagePattern(profileImageCache.get(profileURL)));
            selectedUserSideProfile.setFill(new ImagePattern(profileImageCache.get(profileURL)));
        }
    
        // Retrieve and update previous messages for the selected conversation
        ArrayList<JsonObject> previousMessages = Firebase.getMessages(user.getTokenID(), getCurrentConversationId());
        if (previousMessages != null && !previousMessages.isEmpty()) {
            updateMessageContainer(previousMessages);
        } else {
            System.out.println("No previous messages found for this conversation.");
        }
    }    

    private String generateUniqueMessageId() {
        return getCurrentConversationId() + "_" + UUID.randomUUID().toString();
    }

    private String getCurrentConversationId() {
        String userId1 = user.getLocalID();
        String userId2 = selectedUserId;
        String conversationId = (userId1.compareTo(userId2) < 0) ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
        System.out.println("Generated conversation ID: " + conversationId);
        return conversationId;
    }
    


    //FOR USER LIST
    private Pane createUserPane(String userName, String profileURL, boolean isLogged, String userId) {
        Pane userPane = new Pane();
        userPane.setPrefSize(310.0, 79.0);
        userPane.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: lightgray; -fx-border-width: 1;");

        Circle profilePic = new Circle(31.0);
        profilePic.setFill(Color.LIGHTGRAY);  // Initial placeholder
        profilePic.setLayoutX(45.0);
        profilePic.setLayoutY(40.0);

        if (profileURL != null && !profileURL.isEmpty()) {
            new Thread(() -> {
                try {
                    Image profilePhoto = new Image(profileURL, false);
                    profileImageCache.put(profileURL, profilePhoto);
                    Platform.runLater(() -> profilePic.setFill(new ImagePattern(profilePhoto)));
                } catch (Exception e) {
                    System.out.println("Error loading profile photo: " + e.getMessage());
                }
            }).start();
        }

        Label nameLabel = new Label(userName);
        nameLabel.setLayoutX(86.0);
        nameLabel.setLayoutY(10.0);
        nameLabel.setFont(javafx.scene.text.Font.font("Arial Rounded MT Bold", 16.0));

        Label chatLabel = new Label("Chat: Lorem Ipsum");
        chatLabel.setLayoutX(86.0);
        chatLabel.setLayoutY(35.0);  
        chatLabel.setPrefHeight(20.0);
        chatLabel.setFont(new Font("Arial", 14.0));
        chatLabel.setTextFill(Color.DARKGRAY);

        Label activeLabel = new Label(isLogged ? "Active" : "Offline");
        activeLabel.setLayoutX(86.0);
        activeLabel.setLayoutY(55.0);
        activeLabel.setFont(javafx.scene.text.Font.font("Arial", 12.0));
        activeLabel.setTextFill(isLogged ? Color.GREEN : Color.GRAY);

        userPane.getChildren().addAll(profilePic, nameLabel, chatLabel, activeLabel);
        userPane.setUserData(userId);

        userPane.setOnMouseClicked( event -> selectUser(userId, userName, profileURL));

        return userPane;
    }

    private void updateUserPane(Pane userPane, String profileURL, boolean isLogged) {
        Circle profilePic = (Circle) userPane.getChildren().get(0);
        Label activeLabel = (Label) userPane.getChildren().get(3);

        String currentStatus = activeLabel.getText();
        String newStatus = isLogged ? "Active" : "Offline";
        if (!currentStatus.equals(newStatus)) {
            activeLabel.setText(newStatus);
            activeLabel.setTextFill(isLogged ? Color.GREEN : Color.GRAY);
        }

        if (profileURL != null && !profileURL.isEmpty()) {
            if (profilePic.getFill() instanceof ImagePattern) {
                ImagePattern currentImage = (ImagePattern) profilePic.getFill();
                if (!currentImage.getImage().getUrl().equals(profileURL)) {
                    loadProfileImage(profilePic, profileURL);
                }
            } else {
                loadProfileImage(profilePic, profileURL);
            }
        } else {
            profilePic.setFill(Color.LIGHTGRAY); // Fallback if no profile URL
        }
    }

    private void loadProfileImage(Circle profilePic, String profileURL) {
        new Thread(() -> {
            try {
                Image profilePhoto = new Image(profileURL, false);
                Platform.runLater(() -> profilePic.setFill(new ImagePattern(profilePhoto)));
            } catch (Exception e) {
                System.out.println("Error loading profile photo: " + e.getMessage());
            }
        }).start();
    }

    private void updateUserContainer(ArrayList<JsonObject> updatedUserList) {
        for (JsonObject userJson : updatedUserList) {
            String username = userJson.has("username") && !userJson.get("username").isJsonNull()
                    ? userJson.get("username").getAsString() : "Unknown User";
            String profileURL = userJson.has("profilePhotoURL") && !userJson.get("profilePhotoURL").isJsonNull()
                    ? userJson.get("profilePhotoURL").getAsString() : "";
            boolean isLogged = userJson.has("isLogged") && !userJson.get("isLogged").isJsonNull()
                    && userJson.get("isLogged").getAsBoolean();
            String userId = userJson.get("userID").getAsString();

            if (userPaneMap.containsKey(username)) {
                Pane existingPane = userPaneMap.get(username);
                updateUserPane(existingPane, profileURL, isLogged);
            } else {
                Pane userPane = createUserPane(username, profileURL, isLogged, userId);
                userPaneMap.put(username, userPane);
                userContainer.getChildren().add(userPane);
            }
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUserList(newValue.trim().toLowerCase());
        });
    }

    private void filterUserList(String query) {
        userContainer.getChildren().clear(); // Clear the container to show filtered results only

        userPaneMap.values().forEach(userPane -> {
            String userName = (String) userPane.getUserData();
            if (userName.toLowerCase().contains(query)) {
                userContainer.getChildren().add(userPane);
            }
        });
    }
}