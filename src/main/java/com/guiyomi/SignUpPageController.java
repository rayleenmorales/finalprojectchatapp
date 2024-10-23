package com.guiyomi;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.Map;


public class SignUpPageController {
    @FXML
    private TextField firstNameField;

    @FXML 
    private TextField lastNameField;

    @FXML 
    private TextField emailField;

    @FXML
    private TextField passwordField;

    @FXML
    private TextField confirmPasswordField;

    @FXML
    private Button chooseProfileBtn;

    @FXML
    private Label messageLabel;

    @FXML
    private Button signUpBtn;

    @FXML
    private Button loginBtn;

    private FirebaseService authService = new FirebaseService();
    private File profilePhoto;

    @FXML
    public void handleSignUpButton(ActionEvent event) throws Exception {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmedPassword = confirmPasswordField.getText();

        System.out.println(profilePhoto);

        // Validating inputs before sign-up
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmedPassword.isEmpty() || profilePhoto == null) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }        

        if(password.length() < 6) {
            messageLabel.setText("Invalid password.");
            return;
        }

        if (!password.equals(confirmedPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        try {
            // Sign up the user
            Map<String, String> authData = authService.signUp(email, password);

            String uid = authData.get("uid");
            String idToken = authData.get("idToken");
    
            // Upload the profile photo and get the download URL
            String profilePhotoUrl = authService.uploadProfilePhoto(profilePhoto, idToken);
    
            if (profilePhotoUrl != null) {
                // Save user info to Firestore with profile photo URL
                authService.saveUserInfo(uid, firstName, lastName, profilePhotoUrl, idToken);
                messageLabel.setText("User signed up successfully!");
            } else {
                messageLabel.setText("Failed to upload profile photo.");
            }
    
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Sign up failed.");
        }
    }

    @FXML
    public void handleChooseProfileButton(ActionEvent event) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        profilePhoto = fileChooser.showOpenDialog(chooseProfileBtn.getScene().getWindow());

        if (profilePhoto != null) {
            System.out.println("Profile photo selected: " + profilePhoto.getAbsolutePath());
        } else {
            System.out.println("No profile photo selected.");
        }
    }

    @FXML
    public void handleLoginButton(ActionEvent event) throws Exception {
        // If sign up button pressed, show sign up page
        Parent chatMainParent = FXMLLoader.load(getClass().getResource("LogInPage.fxml"));
        Scene chatMainScene = new Scene(chatMainParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(chatMainScene);
        window.show();
    } 
    
}