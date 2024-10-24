package com.guiyomi;

// import java.io.BufferedReader;
// import java.io.InputStreamReader;
// import java.io.OutputStream;
// import java.net.HttpURLConnection;
// import java.net.URL;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Timer;
// import java.util.TimerTask;

// import org.json.JSONArray;
// import org.json.JSONObject;

// import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatMainController {
    @FXML
    private VBox userContainer; 

    @FXML
    private VBox messageContainer;

    @FXML
    private TextField messageField; 

    @FXML
    private ImageView currentUserProfile;

    @FXML 
    private ImageView selectedUserProfile;

    @FXML
    private Label currentUserLabel;

    @FXML 
    private Label selectedUserLabel;

    FirebaseService firebaseService = new FirebaseService();

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
                    Image profilePhoto = new Image(photoUrl);
                    currentUserProfile.setImage(profilePhoto);
                } catch (Exception e) {
                    System.out.println("Error loading profile photo: " + e.getMessage());
                }
            } else {
                System.out.println("Invalid profile photo URL.");
            }
        }

        // // Start fetching messages periodically
        // timer = new Timer();
        // timer.schedule(new TimerTask() {
        //     @Override
        //     public void run() {
        //         fetchMessages();
        //     }
        // }, 0, 5000); // Fetch every 5 seconds

        // // Load users when the controller initializes
        // loadUsers();
    }

    @FXML
    public void handleLogOutButton(ActionEvent event) throws Exception {
        // Update the logged-in status in Firestore to false
        String userId = UserSession.getUserId();
        String idToken = UserSession.getIdToken(); // Replace with the actual token

        firebaseService.setUserLoggedInStatus(userId, false, idToken);

        // End the session and load the login page
        UserSession.endSession();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("LogInPage.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


    @FXML
    public void handleSendButton(ActionEvent event) throws Exception {

    }
    
    

    // private final String FIREBASE_USERS_URL = "https://firestore.googleapis.com/v1/projects/katalk-db42a/databases/(default)/documents/users";
    // private final String FIREBASE_MESSAGES_URL = "https://firestore.googleapis.com/v1/projects/katalk-db42a/databases/(default)/documents/messages"; // For fetching messages
    // private final String FIREBASE_API_KEY = "AIzaSyBNtPixNZI2wecaRO37a3l0hkJsjBSYMsQ"; // Your Firebase API key

    //private Timer timer;

    // Fetch users from Firestore
    // private void loadUsers() {
    //     new Thread(() -> {
    //         HttpURLConnection conn = null;
    //         try {
    //             URL url = new URL(FIREBASE_USERS_URL);
    //             conn = (HttpURLConnection) url.openConnection();
    //             conn.setRequestMethod("GET");
    //             conn.setRequestProperty("Authorization", "Bearer " + userToken); // Add your token for authorization

    //             int responseCode = conn.getResponseCode();
    //             System.out.println("Response Code: " + responseCode);

    //             if (responseCode == HttpURLConnection.HTTP_OK) {
    //                 BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    //                 StringBuilder response = new StringBuilder();
    //                 String inputLine;

    //                 while ((inputLine = in.readLine()) != null) {
    //                     response.append(inputLine);
    //                 }
    //                 in.close();

    //                 // Parse the response and update the UI
    //                 List<String> users = parseUserList(response.toString());
    //                 Platform.runLater(() -> updateUserList(users));
    //             } else {
    //                 System.out.println("Failed to fetch users: " + conn.getResponseMessage());
    //             }
    //         } catch (Exception e) {
    //             e.printStackTrace();
    //         } finally {
    //             if (conn != null) {
    //                 conn.disconnect();
    //             }
    //         }
    //     }).start();
    // }

    // private List<String> parseUserList(String jsonResponse) {
    //     List<String> users = new ArrayList<>();
    //     JSONObject jsonObject = new JSONObject(jsonResponse);
    //     JSONArray documents = jsonObject.getJSONArray("documents");

    //     for (int i = 0; i < documents.length(); i++) {
    //         JSONObject userDocument = documents.getJSONObject(i);
    //         String userName = userDocument.getJSONObject("fields").getJSONObject("name").getString("stringValue");
    //         users.add(userName);
    //     }
    //     return users;
    // }

    // private void updateUserList(List<String> users) {
    //     userContainer.getChildren().clear(); // Clear existing user list
    //     for (String user : users) {
    //         // Create a label for each user
    //         Label userLabel = new Label(user);
    //         userContainer.getChildren().add(userLabel);
    //     }
    // }

    // Fetch messages from Firestore
    // private void fetchMessages() {
    //     HttpURLConnection conn = null;
    //     try {
    //         URL url = new URL(FIREBASE_MESSAGES_URL);
    //         conn = (HttpURLConnection) url.openConnection();
    //         conn.setRequestMethod("GET");
    //         conn.setRequestProperty("Authorization", "Bearer " + userToken); // Add your token for authorization
    //         conn.setRequestProperty("Content-Type", "application/json");

    //         int responseCode = conn.getResponseCode();
    //         System.out.println("Response Code: " + responseCode);

    //         if (responseCode == HttpURLConnection.HTTP_OK) {
    //             BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    //             StringBuilder response = new StringBuilder();
    //             String inputLine;
    //             while ((inputLine = in.readLine()) != null) {
    //                 response.append(inputLine);
    //             }
    //             in.close();

    //             // Parse and display messages
    //             Platform.runLater(() -> displayMessages(response.toString()));
    //         } else {
    //             System.out.println("Failed to fetch messages: " + conn.getResponseMessage());
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } finally {
    //         if (conn != null) {
    //             conn.disconnect();
    //         }
    //     }
    // }

    // Display messages in the VBox
    // private void displayMessages(String jsonResponse) {
    //     // Clear the current messages
    //     messageContainer.getChildren().clear();

    //     // Parse the JSON response
    //     JSONObject jsonObject = new JSONObject(jsonResponse);
    //     JSONArray documents = jsonObject.getJSONArray("documents");

    //     for (int i = 0; i < documents.length(); i++) {
    //         JSONObject messageDocument = documents.getJSONObject(i);
    //         String msgText = messageDocument.getJSONObject("fields").getJSONObject("text").getString("stringValue");
    //         Label messageLabel = new Label(msgText);
    //         messageContainer.getChildren().add(messageLabel);
    //     }
    // }

    // Send a message to Firestore
    // private void sendMessage() {
    //     String messageText = messageInput.getText();
    //     String senderId = UserSession.getUserId(); // Get the logged-in user's ID
    //     if (!messageText.isEmpty() && senderId != null) {
    //         HttpURLConnection conn = null;
    //         try {
    //             URL url = new URL(FIREBASE_MESSAGES_URL); // Directly POST to messages collection
    //             conn = (HttpURLConnection) url.openConnection();
    //             conn.setRequestMethod("POST"); // Use POST for creating new document
    //             conn.setRequestProperty("Authorization", "Bearer " + userToken); // Add your token for authorization
    //             conn.setRequestProperty("Content-Type", "application/json");
    //             conn.setDoOutput(true);
    
    //             // Create JSON object for the message
    //             JSONObject message = new JSONObject();
    //             message.put("fields", new JSONObject()
    //                 .put("text", new JSONObject().put("stringValue", messageText))
    //                 .put("senderId", new JSONObject().put("stringValue", senderId))
    //                 .put("timestamp", new JSONObject().put("timestampValue", System.currentTimeMillis()))
    //                 .put("type", new JSONObject().put("stringValue", "text"))
    //                 .put("attachments", new JSONObject().put("arrayValue", new JSONArray()))); // Empty array for attachments
                
    //             // Debugging output
    //             System.out.println("Sending message: " + message.toString());
    
    //             // Send the message
    //             OutputStream os = conn.getOutputStream();
    //             os.write(message.toString().getBytes());
    //             os.flush();
    //             os.close();
    
    //             // Check response code
    //             int responseCode = conn.getResponseCode();
    //             if (responseCode == HttpURLConnection.HTTP_OK) {
    //                 // Clear input after sending
    //                 messageInput.clear();
    //                 // Optionally fetch messages after sending
    //                 fetchMessages(); // Fetch messages to update UI immediately
    //             } else {
    //                 System.out.println("Error sending message: " + conn.getResponseMessage());
    //             }
    //         } catch (Exception e) {
    //             e.printStackTrace();
    //         } finally {
    //             if (conn != null) {
    //                 conn.disconnect();
    //             }
    //         }
    //     }
    // }
    

    // Method to handle user token
    // public void setUserToken(String token) {
    //     if (token != null && !token.isEmpty()) {
    //         this.userToken = token; // Set the token only if it's valid
    //         System.out.println("User token set: " + userToken);
    //     } else {
    //         System.out.println("Invalid token provided.");
    //     }
    // }

    // Clean up the timer when closing
    // public void stop() {
    //     if (timer != null) {
    //         timer.cancel();
    //     }
    // }
}
