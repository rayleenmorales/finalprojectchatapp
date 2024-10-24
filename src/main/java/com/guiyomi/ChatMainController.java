package com.guiyomi;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.stage.Stage;


public class ChatMainController extends Application {


    @FXML
    private VBox userContainer;


    @FXML
    private ScrollPane messageContainer;


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
        


        // Uncomment below if you want to periodically fetch messages
        // timer = new Timer();
        // timer.schedule(new TimerTask() {
        //     @Override
        //     public void run() {
        //         fetchMessages();
        //     }
        // }, 0, 5000); // Fetch every 5 seconds


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
                    Parent root = FXMLLoader.load(getClass().getResource("LOGIN PAGE.fxml"));
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
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
            JSONObject fields = document.getJSONObject("fields");


            // Extract first and last names
            String firstName = fields.has("firstName") ?
                fields.getJSONObject("firstName").getString("stringValue") : "Unknown";
            String lastName = fields.has("lastName") ?
                fields.getJSONObject("lastName").getString("stringValue") : "Unknown";


            // Create a new Pane for each user
            Pane userPane = createUserPane(firstName + " " + lastName, "Chat: Lorem Ipsum");
            userPane.setOnMouseClicked(event -> selectUser(firstName + " " + lastName, "")); // Add logic for profile picture if needed


            // Add user pane to the user container
            Platform.runLater(() -> userContainer.getChildren().add(userPane));
        }
    } catch (JSONException e) {
        e.printStackTrace(); // Handle the exception appropriately
    } catch (Exception e) {
        e.printStackTrace(); // Handle any other exceptions
    }
}


private Pane createUserPane(String userName, String chatPreview) {
    Pane userPane = new Pane();
    userPane.setPrefSize(310.0, 79.0);
    userPane.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: lightgray; -fx-border-width: 1;");


    // Create the Circle for the profile picture
    Circle profilePic = new Circle(31.0, Color.RED); // Placeholder for now
    profilePic.setStroke(Color.BLACK);
    profilePic.setStrokeType(StrokeType.INSIDE);
    profilePic.setLayoutX(45.0);
    profilePic.setLayoutY(40.0);


    // Create the name label
    Label nameLabel = new Label(userName);
    nameLabel.setLayoutX(86.0);
    nameLabel.setLayoutY(9.0);
    nameLabel.setPrefHeight(38.0);
    nameLabel.setPrefWidth(180.0);
    nameLabel.setFont(new Font("Arial Rounded MT Bold", 16.0));


    // Create the chat preview label
    Label chatLabel = new Label(chatPreview);
    chatLabel.setLayoutX(86.0);
    chatLabel.setLayoutY(40.0);
    chatLabel.setPrefHeight(24.0);
    chatLabel.setPrefWidth(180.0);
    chatLabel.setFont(new Font("Arial", 16.0));


    // Add all elements to the userPane
    userPane.getChildren().addAll(profilePic, nameLabel, chatLabel);
    return userPane;
}


   




public void selectUser(String fullName, String profilePicUrl) {
    selectedUserLabel.setText(fullName); // Update the label with the selected user's name


    // Clear previous messages in the messageContainer
    messageContainer.getChildrenUnmodifiable().clear();  


    // Load messages history for the selected user
    loadMessageHistory(fullName);  


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
}




   
    public void handleSendButton(ActionEvent event) {
        String messageText = messageField.getText();
        if (!messageText.isEmpty()) {
            String selectedUser = selectedUserLabel.getText();
            String currentUser = UserSession.getUserName();  // Assuming the current user's name is stored in session
   
            // Prepare the message data
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("sender", currentUser); // Use 'sender'
            messageData.put("receiver", selectedUser); // Use 'receiver'
            messageData.put("text", messageText); // Use 'text'
            messageData.put("timestamp", Instant.now().toString()); // Use the current timestamp
   
            // Call the HTTP-based sendMessage method
            boolean success = firebaseService.sendMessage(messageData); // This should now return a boolean
            if (success) {
                messageField.clear();  // Clear message field
                displayMessage(messageText, true);  // Display the sent message in chat
            } else {
                // Display an error message to the user
                System.out.println("Message sending failed. Please try again.");
            }
        }
    }
   
   


   


    public void displayMessage(String message, boolean isCurrentUser) {
        HBox messageBox = new HBox();
        messageBox.setSpacing(10);


        Label messageLabel = new Label(message);
        messageLabel.setFont(new Font("Arial", 12));


        if (isCurrentUser) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }


        messageBox.getChildren().add(messageLabel);
        Platform.runLater(() -> messageContainer.getChildrenUnmodifiable().add(messageBox));
    }


    private void loadMessageHistory(String userName) {
        String currentUserUid = UserSession.getUserId();  // Get the current logged-in user's UID
        String selectedUserUid = firebaseService.getUserUidByName(userName); // Get the UID for the selected user
        String url = "https://firestore.googleapis.com/v1/projects/katalk-db42a/databases/(default)/documents/messages";
   
        try {
            // Prepare the request URL with query parameters to filter by sender and receiver UID
            String query = String.format("?where=(sender==\"%s\" AND receiver==\"%s\") OR (sender==\"%s\" AND receiver==\"%s\")",
                                          currentUserUid, selectedUserUid, selectedUserUid, currentUserUid);
            URI uri = new URI(url + query);  // Create a URI object with query
            URL obj = uri.toURL();   // Convert URI to URL
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
   
            // Set request method to GET
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + UserSession.getIdToken()); // Add the token here
   
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                // Read the response from the input stream
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
   
                // Parse the JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray documents = jsonResponse.getJSONArray("documents");
   
                // Clear the VBox before adding new messages
                Platform.runLater(() -> messageContainer.getChildrenUnmodifiable().clear());
   
                for (int i = 0; i < documents.length(); i++) {
                    JSONObject document = documents.getJSONObject(i);
                    JSONObject fields = document.getJSONObject("fields");
   
                    String sender = fields.getJSONObject("sender").getString("stringValue");
                    String message = fields.getJSONObject("text").getString("stringValue"); // Assuming your field is named "text"
   
                    // Display messages based on sender
                    boolean isCurrentUser = sender.equals(UserSession.getUserName());
                    displayMessage(message, isCurrentUser);
                }
            } else {
                // Handle failure response
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                System.out.println("GET request failed with response code " + responseCode + ": " + errorResponse.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
   
   
   
   
    private String sendHttpGetRequest(String urlString) throws Exception {
        // Create a URL object
        URL url = new URL(urlString);
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
   
   
    private void parseUsers(JSONArray documents) {
        Platform.runLater(() -> userContainer.getChildren().clear());
        for (int i = 0; i < documents.length(); i++) {
            JSONObject document = documents.getJSONObject(i);
            JSONObject fields = document.getJSONObject("fields");
   
            String userName = fields.getJSONObject("firstName").getString("stringValue") + " " +
                              fields.getJSONObject("lastName").getString("stringValue");
            String profilePicUrl = fields.has("profilePic") ?
                                   fields.getJSONObject("profilePic").getString("stringValue") : null;
   
            // Create Pane for each user
            Pane userPane = new Pane();
            userPane.setPrefHeight(79);
            userPane.setPrefWidth(310);
            userPane.getStyleClass().add("chat-pane"); // Add CSS class for styling
   
            // Create Circle for profile picture
            Circle profileCircle = new Circle(31);
            profileCircle.setFill(Color.RED); // Default color if image fails to load
            profileCircle.setStroke(Color.BLACK);
            profileCircle.setLayoutX(45);
            profileCircle.setLayoutY(40);
   
            // Load profile picture
            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                try {
                    Image profileImage = new Image(profilePicUrl, 62, 62, false, false);
                    profileCircle.setFill(new ImagePattern(profileImage)); // Set image as fill for Circle
                } catch (Exception e) {
                    System.out.println("Error loading profile picture: " + e.getMessage());
                }
            }
   
            // Create Name Label
            Label nameLabel = new Label(userName);
            nameLabel.setLayoutX(86);
            nameLabel.setLayoutY(9);
            nameLabel.setPrefHeight(38);
            nameLabel.setPrefWidth(180);
            nameLabel.setFont(new Font("Arial Rounded MT Bold", 16));
   
            // Create Chat Label
            Label chatLabel = new Label("Chat: Lorem Ipsum"); // Modify as needed
            chatLabel.setLayoutX(86);
            chatLabel.setLayoutY(40);
            chatLabel.setPrefHeight(24);
            chatLabel.setPrefWidth(180);
            chatLabel.setFont(new Font("Arial", 16));
   
            // Add elements to Pane
            userPane.getChildren().addAll(profileCircle, nameLabel, chatLabel);
   
            // Add mouse click event to select user
            userPane.setOnMouseClicked(event -> selectUser(userName, profilePicUrl));
   
            // Add Pane to userContainer
            Platform.runLater(() -> userContainer.getChildren().add(userPane));
        }
    }
   




   
   
}
