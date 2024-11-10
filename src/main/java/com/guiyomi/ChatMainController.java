package com.guiyomi;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

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
    private Main mainApp;
    private String selectedUserId;


    // Method to set the Main instance for access to logout functionality
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
        System.out.println("mainApp set in ChatMainController: " + mainApp);
    }

    public void setUser(User user) {
        this.user = user;
        initializeUserProfile();
        initializeUserListListener();
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
            System.out.println("Checking if mainApp is null: " + this.mainApp);
            if (this.mainApp != null) {
                System.out.println("Logging out user: " + user.getUserName());
                mainApp.logout();
            }
        }
    }

    //FOR USER LIST
    // Real-time listener for user list
    private void initializeUserListListener() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                System.out.println("DataSnapshot received: " + dataSnapshot);

                // Check if dataSnapshot has children (i.e., users data)
                if (dataSnapshot.exists()) {
                    System.out.println("Users data found.");
                    // Clear the current user list in the UI
                    userContainer.getChildren().clear();
                }

                
                    
                
                // Loop through each user and create a pane for each one
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String username = userSnapshot.child("username").getValue(String.class);
                    String profilePhotoURL = userSnapshot.child("profilePhotoURL").getValue(String.class);
                    boolean isLogged = userSnapshot.child("isLogged").getValue(Boolean.class);
                    String userID = userSnapshot.getKey();

                    // Create a User object
                    User user = new User(username, profilePhotoURL, isLogged, userID);
                    
                    
                    // Create user pane and add to UI
                    Pane userPane = createUserPane(user);

                    // Add user pane to the user container
                    userContainer.getChildren().add(userPane);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("Failed to read user list: " + databaseError.getMessage());
            }
        });
    }

    // Helper method to add a user pane to the UI
    private Pane createUserPane(User user) {
        Pane userPane = new Pane();
        userPane.setPrefSize(310.0, 79.0);
        userPane.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: lightgray; -fx-border-width: 1;");

        // Profile Picture Circle
        Circle profilePic = new Circle(31.0);
        profilePic.setFill(Color.LIGHTGRAY);  // Placeholder color
        profilePic.setLayoutX(45.0);
        profilePic.setLayoutY(40.0);

        // Load or cache profile picture
        Image profilePhoto = user.getProfilePicture();
        if (profilePhoto != null) {
            profilePic.setFill(new ImagePattern(profilePhoto));
        } else {
            System.out.println("Failed to load profile picture for user: " + user.getUserName());
        }

        // User Labels
        Label nameLabel = new Label(user.getUserName());
        nameLabel.setLayoutX(86.0);
        nameLabel.setLayoutY(10.0);
        nameLabel.setFont(javafx.scene.text.Font.font("Arial Rounded MT Bold", 16.0));

        Label chatLabel = new Label("Chat: Lorem Ipsum");
        chatLabel.setLayoutX(86.0);
        chatLabel.setLayoutY(35.0);  
        chatLabel.setPrefHeight(20.0);
        chatLabel.setFont(new Font("Arial", 14.0));
        chatLabel.setTextFill(Color.DARKGRAY);

        Label activeLabel = new Label(user.getisLogged() ? "Active" : "Offline");
        activeLabel.setLayoutX(86.0);
        activeLabel.setLayoutY(55.0);
        activeLabel.setFont(javafx.scene.text.Font.font("Arial", 12.0));
        activeLabel.setTextFill(user.getisLogged() ? Color.GREEN : Color.GRAY);

        userPane.getChildren().addAll(profilePic, nameLabel, chatLabel, activeLabel);
        userPane.setUserData(user.getLocalID());

        // Set event for selecting user pane
        userPane.setOnMouseClicked(_ -> selectUser(user));

        return userPane;
    }


    // Method to select a user from the user list
    public void selectUser(User user) {
        // Clear previous messages for the selected conversation
        messageContainer.getChildren().clear();

        // Set selected user information
        selectedUserId = user.getLocalID();
        selectedUserLabel.setText(user.getUserName());
        selectedUserSideLabel.setText(user.getUserName());

        // Set profile picture, using cached image if available
        Image profilePhoto = user.getProfilePicture();
        selectedUserProfile.setFill(new ImagePattern(profilePhoto));
    }    


    // FOR MESSAGES
    @FXML
    public void handleSendButton(ActionEvent event) {
        // Placeholder for sending messages
        System.out.println("Send button clicked.");
    }

    // FOR GAMES
    @FXML
    private void handleGameLogoClick() {
        // Your logic here
    }

    @FXML  
    private void handleSecondGameLogoClick() {
        // Your logic here
    }
}
