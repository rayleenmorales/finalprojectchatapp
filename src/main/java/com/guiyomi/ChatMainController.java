package com.guiyomi;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.io.IOException;
import java.awt.Desktop;

import javax.swing.SwingUtilities;
import com.google.gson.JsonObject;
import com.guiyomi.Game2.GameWindowTictac;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.application.Platform;

public class ChatMainController {

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


    @FXML
    private ImageView gameLogo;


    @FXML
    private ImageView secondGameLogo;


    private User user;
    private User selectedUser;
    private Main mainApp;

    private AnimationTimer userUpdateLoop;
    private AnimationTimer messageUpdateLoop;
    
    private ArrayList<JsonObject> updateUsers = new ArrayList<>();
    private Map<String, Pane> userPaneMap = new HashMap<>();

    private ArrayList<JsonObject> updateMessage = new ArrayList<>();
    private Map<String, Pane> messagePaneMap = new HashMap<>();
    private int lastMessageCount = 0; 

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

    // Method to set the Main instance for access to logout functionality
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    public void setUser(User user) {
        this.user = user;
        initializeUserProfile();
        startUserUpdateLoop();
    }
    
    private void initializeUserProfile() {
        if (user != null) {
            try {
                Image profilePhoto = user.getProfilePicture();
                currentUserProfile.setFill(new ImagePattern(profilePhoto));
                currentUserLabel.setText(user.getUserName());
            } catch (Exception e) {
                System.out.println("Error loading profile photo: " + e.getMessage());
            }
        }
    }
    
    // Handle logout button click
    @FXML
    public void handleLogOutButton(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to log out?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            if (this.mainApp != null) {
                System.out.println("Logging out user: " + user.getUserName());
                stopAllUpdates(); // Stop both user list and message updates
                mainApp.logout();
            }
        }
    }

    //FOR REAL TIME UPDATES
    private void startUserUpdateLoop() {
        if (userUpdateLoop == null) {
            userUpdateLoop = new AnimationTimer() {
                private long lastUserUpdate = 0;

                @Override
                public void handle(long now) {
                    if (now - lastUserUpdate >= 1_000_000_000) { // 1-second interval
                        lastUserUpdate = now;
                        Thread userUpdateThread = new Thread(userUpdateTask);
                        userUpdateThread.setDaemon(true);
                        userUpdateThread.start();
                    }
                }
            };
        }
        userUpdateLoop.start();
    }

    private void startMessageUpdateLoop() {
        if (messageUpdateLoop == null) {
            messageUpdateLoop = new AnimationTimer() {
                private long lastMessageUpdate = 0;

                @Override
                public void handle(long now) {
                    if (now - lastMessageUpdate >= 1_000_000_000) { // 1-second interval
                        lastMessageUpdate = now;
                        Thread messageUpdateThread = new Thread(messageUpdateTask);
                        messageUpdateThread.setDaemon(true);
                        messageUpdateThread.start();
                    }
                }
            };
        }
        messageUpdateLoop.start();
    }

    public void stopAllUpdates() {
        if (userUpdateLoop != null) {
            userUpdateLoop.stop();
        }
        if (messageUpdateLoop != null) {
            messageUpdateLoop.stop();
        }
    }

    Runnable userUpdateTask = () -> {
        updateUsers = Firebase.getUsers(user.getTokenID());
        Platform.runLater(() -> updateUserContainer(updateUsers));
    };

    Runnable messageUpdateTask = () -> {
        if (user == null || selectedUser == null || selectedUser.getLocalID() == null) {
            return;
        }
        updateMessage = Firebase.getMessages(user.getTokenID(), getCurrentConversationId());
        Platform.runLater(() -> updateMessageContainer(updateMessage, selectedUser));
    };

    public void clearUserData() {
        // Clear user object and selected user ID
        this.user = null;
        this.selectedUser = null;
    
        // Clear user-specific data structures
        userPaneMap.clear();
        messagePaneMap.clear();
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
            selectedUserSideLabel.setText("");
            selectedUserProfile.setFill(Color.TRANSPARENT);
            selectedUserSideProfile.setFill(Color.TRANSPARENT);
            currentUserProfile.setFill(Color.TRANSPARENT);
        });
    }

    //FOR USER LIST
    private Pane createUserPane(String userName, String profileURL, boolean isLogged, String userId) {
        User newUser = new User(userName, profileURL, isLogged, userId);

        Pane userPane = new Pane();
        userPane.setPrefSize(310.0, 79.0);
        userPane.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: lightgray; -fx-border-width: 1;");

        Circle profilePic = new Circle(31.0);
        profilePic.setFill(Color.LIGHTGRAY);  // Initial placeholder
        profilePic.setLayoutX(45.0);
        profilePic.setLayoutY(40.0);

        if (profileURL != null && !profileURL.isEmpty()) {
            new Thread(() -> {
                Image profilePicture = newUser.getProfilePicture(); // Download and cache the profile picture
                SwingUtilities.invokeLater(() -> profilePic.setFill(new ImagePattern(profilePicture)));
            }).start();
        }

        Label nameLabel = new Label(userName);
        nameLabel.setLayoutX(86.0);
        nameLabel.setLayoutY(10.0);
        nameLabel.setFont(javafx.scene.text.Font.font("Arial Rounded MT Bold", 16.0));


        Label activeLabel = new Label(isLogged ? "Active" : "Offline");
        activeLabel.setLayoutX(86.0);
        activeLabel.setLayoutY(30.0);
        activeLabel.setFont(javafx.scene.text.Font.font("Arial", 12.0));
        activeLabel.setTextFill(isLogged ? Color.GREEN : Color.GRAY);

        userPane.getChildren().addAll(profilePic, nameLabel, activeLabel);
        userPane.setUserData(userName);

        userPane.setOnMouseClicked( _ -> selectUser(newUser));

        return userPane;
    }

    private void updateUserPane(Pane userPane, boolean isLogged) {
        Label activeLabel = new Label(isLogged ? "Active" : "Offline");
            activeLabel.setId("activeLabel");

        String currentStatus = activeLabel.getText();
        String newStatus = isLogged ? "Active" : "Offline";
        if (!currentStatus.equals(newStatus)) {
            activeLabel.setText(newStatus);
            activeLabel.setTextFill(isLogged ? Color.GREEN : Color.GRAY);
        } else {
            return;
        }
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
                updateUserPane(existingPane, isLogged);
            } else {
                Pane userPane = createUserPane(username, profileURL, isLogged, userId);
                userPaneMap.put(username, userPane);
                userContainer.getChildren().add(userPane);
            }
        }
    }

    // Method to select a user from the user list
    public void selectUser(User user) {
        this.selectedUser = user;
        // Clear previous messages for the selected conversation
        messagePaneMap.clear();
        messageContainer.getChildren().clear();
        lastMessageCount = 0;

        // Set selected user information
        selectedUserLabel.setText(user.getUserName());
        selectedUserSideLabel.setText(user.getUserName());

        // Set profile picture, using cached image if available
        Image profilePhoto = user.getProfilePicture();
        selectedUserProfile.setFill(new ImagePattern(profilePhoto));
        selectedUserSideProfile.setFill(new ImagePattern(profilePhoto));

        // Retrieve and update previous messages for the selected conversation
        ArrayList<JsonObject> previousMessages = Firebase.getMessages(user.getTokenID(), getCurrentConversationId());
        if (previousMessages != null && !previousMessages.isEmpty()) {
            updateMessageContainer(previousMessages, user);
        } else {
            System.out.println("No previous messages found for this conversation.");
            
        }

        startMessageUpdateLoop(); // Start message updates for the selected user
    }    

    // FOR MESSAGES
    @FXML
    public void handleSendButton(ActionEvent event) {
        String messageText = messageField.getText().trim();

        if (messageText.isEmpty() || selectedUser == null || selectedUser.getLocalID() == null) {
            // No message to send or no user selected
            System.out.println("No user selected or message is empty.");
            return;
        }

        String messageId = generateUniqueMessageId();

        // Create the message object with necessary fields
        JsonObject message = new JsonObject();
        message.addProperty("senderId", user.getLocalID());  // Local user ID as sender
        message.addProperty("receiverId", selectedUser.getLocalID());   // Selected user as receiver
        message.addProperty("content", messageText);
        message.addProperty("timestamp", System.currentTimeMillis());
        message.addProperty("messageId", messageId);

        Firebase.putMessage(user.getTokenID(), getCurrentConversationId(), message);

        // Clear the message field after sending
        messageField.clear();
    }

    @FXML
    public void handleAttachmentButton(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("All Files", "*.*"),
            new ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov"),
            new ExtensionFilter("Documents", "*.pdf", "*.docx", "*.txt")
        );
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            cacheUploadAndSendAttachment(selectedFile);
        }
    }

    public void cacheUploadAndSendAttachment (File file) {
        // Upload the file to Firebase Storage and get the download URL
        String downloadURL = Firebase.uploadFile(user.getTokenID(), file);

        // Cache the file locally for future use
        CacheHelper.cacheFile(file);

        // Send the download URL to Realtime Database as a message
        if (downloadURL != null) {
            // Create the message object with necessary fields
            String messageId = generateUniqueMessageId();
            JsonObject message = new JsonObject();
            message.addProperty("senderId", user.getLocalID());  // Local user ID as sender
            message.addProperty("receiverId", selectedUser.getLocalID());   // Selected user as receiver
            message.addProperty("content", downloadURL);
            message.addProperty("timestamp", System.currentTimeMillis());
            message.addProperty("messageId", messageId);

            Firebase.putMessage(user.getTokenID(), getCurrentConversationId(), message);
        } else {
            System.out.println("Error uploading file to Firebase Storage.");
        }

        // Clear the message field after sending
        messageField.clear();
    }   

    private void updateMessageContainer(ArrayList<JsonObject> newMessages, User selectedUser) {
        System.out.println("Updating message container with " + newMessages.size() + " messages.");
    
        newMessages.sort(Comparator.comparingLong(message -> message.get("timestamp").getAsLong()));
    
        boolean newMessageAdded = false;
    
        for (JsonObject messageJson : newMessages) {
            String messageId = messageJson.get("messageId").getAsString();
    
            if (!messagePaneMap.containsKey(messageId)) {
                String senderId = messageJson.get("senderId").getAsString();
                String content = messageJson.get("content").getAsString();
                String timestamp = messageJson.get("timestamp").getAsString();
    
                boolean isSender = senderId.equals(user.getLocalID());
                User messageUser = isSender ? user : selectedUser;
    
                Pane messagePane;
                if (isMediaMessage(content)) { // Check if the content is a media URL
                    messagePane = createMediaMessagePane(messageUser, content, timestamp, isSender);
                } else {
                    messagePane = createTextMessagePane(messageUser, content, timestamp, isSender);
                }
    
                messagePaneMap.put(messageId, messagePane);
                messageContainer.getChildren().add(messagePane);
                newMessageAdded = true;
            }
        }
    
        if (newMessageAdded && messageContainer.getChildren().size() > lastMessageCount) {
            messageScrollPane.layout();
            messageScrollPane.setVvalue(1.0);
        }
        lastMessageCount = messageContainer.getChildren().size();
    }
    
    private boolean isMediaMessage(String content) {
        if (content.startsWith("http")) {
            // Extract the URL path without query parameters
            String path = content.split("\\?")[0].toLowerCase();
            
            // Check if the path ends with a known media file extension
            return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || 
                   path.endsWith(".gif") || path.endsWith(".mp4") || path.endsWith(".avi") || 
                   path.endsWith(".pdf") || path.endsWith(".docx") || path.endsWith(".txt");
        }
        return false;
    }    
    
    
    private Pane createMediaMessagePane(User user, String mediaUrl, String timestamp, boolean isSender) {
        // Strip query parameters from URL by splitting at "?"
        String path = mediaUrl.split("\\?")[0].toLowerCase();
    
        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".gif")) {
            return createImageMessagePane(user, mediaUrl, timestamp, isSender);
        } else if (path.endsWith(".mp4") || path.endsWith(".avi")) {
            return createVideoMessagePane(user, mediaUrl, timestamp, isSender);
        } else {
            return createAttachmentMessagePane(user, mediaUrl, timestamp, isSender);
        }
    }
    

    private Pane createImageMessagePane(User user, String imageUrl, String timestamp, boolean isSender) {
        HBox messageBox = new HBox();
        messageBox.setStyle("-fx-padding: 10");
        messageBox.setFillHeight(true);
        messageBox.setSpacing(10);
        messageBox.setPrefWidth(Double.MAX_VALUE);
        messageBox.setAlignment(isSender ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(messageBox, Priority.ALWAYS);

        // Only create profilePic Circle if profilePictureUrl is provided
        Circle profilePic = new Circle(20.0);
        Image profilePhoto = user.getProfilePicture();
        Platform.runLater(() -> profilePic.setFill(new ImagePattern(profilePhoto)));
                    
        // Add profilePic to messageBox
        if (isSender) {
            messageBox.getChildren().add(profilePic);
        } else {
            messageBox.getChildren().add(0, profilePic);
        }

        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);

        // Get the cached file path for the image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Image cachedImage = CacheHelper.getCachedImage(imageUrl);
            Platform.runLater(() -> imageView.setImage(cachedImage));
        }

        VBox messageContent = new VBox();
        messageContent.setSpacing(2);
        messageContent.setMaxWidth(400);
        messageContent.getChildren().add(imageView);

        Label timestampLabel = new Label(formatTimestamp(timestamp));
        timestampLabel.setFont(new Font("Arial", 10.0));
        timestampLabel.setTextFill(Color.GRAY);
        messageContent.getChildren().add(timestampLabel);

        if (isSender) {
            messageBox.getChildren().add(0, messageContent);  // Add at the beginning if receiver
            
        } else {
            messageBox.getChildren().add(messageContent);  // Add at the end if sender
        }

        return messageBox;
    }


    private Pane createVideoMessagePane(User user, String videoUrl, String timestamp, boolean isSender) {
        HBox messageBox = new HBox();
        messageBox.setStyle("-fx-padding: 10");
        messageBox.setFillHeight(true);
        messageBox.setSpacing(10);
        messageBox.setPrefWidth(Double.MAX_VALUE);
        messageBox.setAlignment(isSender ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(messageBox, Priority.ALWAYS);

        // Profile picture for the sender or receiver
        Circle profilePic = new Circle(20.0);
        Image profilePhoto = user.getProfilePicture();
        Platform.runLater(() -> profilePic.setFill(new ImagePattern(profilePhoto)));

        if (isSender) {
            messageBox.getChildren().add(profilePic);
        } else {
            messageBox.getChildren().add(0, profilePic);
        }

        // Media setup
        Media media = CacheHelper.getCachedVideo(videoUrl);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        MediaView mediaView = new MediaView(mediaPlayer);
        mediaView.setFitWidth(300);
        mediaView.setPreserveRatio(true);
        mediaPlayer.setAutoPlay(false);

        // Toggle Play/Pause Button
        Button playPauseButton = new Button("Play");
        playPauseButton.setOnAction(_ -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPauseButton.setText("Play");
            } else {
                mediaPlayer.play();
                playPauseButton.setText("Pause");
            }
        });

        // Volume Button with Slider
        Button volumeButton = new Button("🔊");
        Slider volumeSlider = new Slider(0, 1, 0.5); // Ranges from 0 (mute) to 1 (full volume)
        volumeSlider.setOrientation(Orientation.VERTICAL);
        volumeSlider.valueProperty().addListener((_, _, newValue) -> mediaPlayer.setVolume(newValue.doubleValue()));
        volumeSlider.setVisible(false);

        volumeButton.setOnMouseEntered(_ -> volumeSlider.setVisible(true));
        volumeSlider.setOnMouseExited(_ -> volumeSlider.setVisible(false));

        // Video Progress Slider
        Slider progressSlider = new Slider();
        progressSlider.setMaxWidth(200);

        // Update progress slider as video plays
        mediaPlayer.currentTimeProperty().addListener((_, _, newTime) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newTime.toMillis() / mediaPlayer.getTotalDuration().toMillis() * 100);
            }
        });

        // Allow seeking using the progress slider
        progressSlider.valueChangingProperty().addListener((_, _, isChanging) -> {
            if (!isChanging) {
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(progressSlider.getValue() / 100.0));
            }
        });

        // Layout for controls
        HBox controls = new HBox(5, playPauseButton, volumeButton, volumeSlider, progressSlider);
        controls.setAlignment(javafx.geometry.Pos.CENTER);

        // Organizing message content
        VBox messageContent = new VBox(5);
        messageContent.setSpacing(2);
        messageContent.setMaxWidth(400);
        messageContent.getChildren().addAll(mediaView, controls);

        // Timestamp
        Label timestampLabel = new Label(formatTimestamp(timestamp));
        timestampLabel.setFont(new Font("Arial", 10.0));
        timestampLabel.setTextFill(Color.GRAY);
        messageContent.getChildren().add(timestampLabel);

        if (isSender) {
            messageBox.getChildren().add(0, messageContent);  // Add at the beginning if receiver
        } else {
            messageBox.getChildren().add(messageContent);  // Add at the end if sender
        }

        return messageBox;
    }


    private Pane createAttachmentMessagePane(User user, String fileUrl, String timestamp, boolean isSender) {
        HBox messageBox = new HBox();
        messageBox.setStyle("-fx-padding: 10");
        messageBox.setFillHeight(true);
        messageBox.setSpacing(10);
        messageBox.setPrefWidth(Double.MAX_VALUE);
        messageBox.setAlignment(isSender ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(messageBox, Priority.ALWAYS);
        
        // Profile picture for the sender or receiver
        Circle profilePic = new Circle(20.0);
        Image profilePhoto = user.getProfilePicture();
        Platform.runLater(() -> profilePic.setFill(new ImagePattern(profilePhoto)));
                        
        if (isSender) {
            messageBox.getChildren().add(profilePic);
        } else {
            messageBox.getChildren().add(0, profilePic);
        }

        File cachedAttachment = null;

        // Get the cached file path for the attachment
        if (fileUrl != null && !fileUrl.isEmpty()) {
            cachedAttachment = CacheHelper.getCachedAttachment(fileUrl);
        }

        // Display file name as clickable label
        Label attachmentLabel = new Label(cachedAttachment != null ? cachedAttachment.getName() : "Download attachment");
        attachmentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14.0)); // Make text bold by default
        attachmentLabel.setStyle(isSender ? "-fx-background-color: lightblue; -fx-padding: 8; -fx-background-radius: 10;" 
                                        : "-fx-background-color: lightgray; -fx-padding: 8; -fx-background-radius: 10;");
        attachmentLabel.setMaxWidth(400);

        // Set underline effect on hover
        attachmentLabel.setOnMouseEntered(_ -> {
            attachmentLabel.setUnderline(true);
        });
        attachmentLabel.setOnMouseExited(_ -> {
            attachmentLabel.setUnderline(false);
        });

        // Open the cached file in the default application when clicked
        File finalCachedAttachment = cachedAttachment;  // To use inside lambda
        attachmentLabel.setOnMouseClicked(_ -> {
            if (finalCachedAttachment != null && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(finalCachedAttachment);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Failed to open file: " + finalCachedAttachment.getName());
                }
            } else {
                System.out.println("Desktop operations not supported, or file is missing.");
            }
        });

        VBox messageContent = new VBox();
        messageContent.setSpacing(2);
        messageContent.setMaxWidth(400);
        messageContent.getChildren().add(attachmentLabel);

        Label timestampLabel = new Label(formatTimestamp(timestamp));
        timestampLabel.setFont(new Font("Arial", 10.0));
        timestampLabel.setTextFill(Color.GRAY);
        messageContent.getChildren().add(timestampLabel);

        if (isSender) {
            messageBox.getChildren().add(0, messageContent);  // Add at the beginning if receiver
        } else {
            messageBox.getChildren().add(messageContent);  // Add at the end if sender
        }

        return messageBox;
    }


    
    
    public Pane createTextMessagePane(User messengerUser, String messageText, String timestamp, boolean isSender) {
        HBox messageBox = new HBox();
        messageBox.setStyle("-fx-padding: 10");
        messageBox.setFillHeight(true);
        messageBox.setSpacing(10);
        messageBox.setPrefWidth(Double.MAX_VALUE);
        messageBox.setAlignment(isSender ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(messageBox, Priority.ALWAYS);
    
        // Only create profilePic Circle if profilePictureUrl is provided
        Circle profilePic = new Circle(20.0);
        Image profilePhoto = messengerUser.getProfilePicture();
        Platform.runLater(() -> profilePic.setFill(new ImagePattern(profilePhoto)));
                    
        // Add profilePic to messageBox
        if (isSender) {
            messageBox.getChildren().add(profilePic);
        } else {
            messageBox.getChildren().add(0, profilePic);
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

    private String generateUniqueMessageId() {
        return getCurrentConversationId() + "_" + UUID.randomUUID().toString();
    }

    private static String formatTimestamp(String timestamp) {
        Instant instant = Instant.ofEpochMilli(Long.parseLong(timestamp));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
    
    private void setupSearchListener() {
        searchField.textProperty().addListener((_, _, newValue) -> {
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

    private String getCurrentConversationId() {
        if (user == null || selectedUser == null) {
            return ""; // Return empty string if user or selectedUser is null
        }
    
        String userId1 = user.getLocalID();
        String userId2 = selectedUser.getLocalID();
        String conversationId = (userId1.compareTo(userId2) < 0) ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
        return conversationId;
    }

    // FOR GAMES
    @FXML
    private void handleGameLogoClick() {
        new Thread(() -> {
            try {
                if (this.user == null) {
                    System.out.println("User is not initialized.");
                    return;
                }

                // Ensure that the Swing components are created on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    Game game = new Game(this.user); // Pass the User object
                    new GameWindow(GameWindow.WIDTH, GameWindow.HEIGHT, "Flappy Bird", game);
                });
            } catch (Exception e) {
                e.printStackTrace(); // Log the error
            }
        }).start();
    }

    
@FXML
private void handleSecondGameLogoClick() {
    new Thread(() -> {
        try {
            if (this.user == null) {
                System.out.println("User is not initialized.");
                return;
            }
            
            if (selectedUser == null) {
                System.out.println("Please select an opponent first.");
                return;
            }

            // Ensure that the Swing components are created on the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                try {
                    // Initialize TicTacToe with the selected opponent
                    Game2 ticTacToeGame = new Game2(user, selectedUser);
                    // Pass the TicTacToe game to a GameWindowTictac instance
                    new GameWindowTictac(GameWindowTictac.WIDTH, GameWindowTictac.HEIGHT, 
                                         "Tic Tac Toe with " + selectedUser.getUserName(), 
                                         ticTacToeGame);
                } catch (Exception e) {
                    System.out.println("Error launching Tic Tac Toe game: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.out.println("An error occurred in handleSecondGameLogoClick: " + e.getMessage());
            e.printStackTrace();
        }
    }).start();
}
}
