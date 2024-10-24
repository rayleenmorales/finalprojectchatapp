package com.guiyomi;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


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

    @FXML
    private Button homeBtn;

    private FirebaseService authService = new FirebaseService();
    private File profilePhoto;

    @FXML
    public void handleHomeButton(ActionEvent event) throws Exception {
        // If sign up button pressed, show sign up page
        Parent chatMainParent = FXMLLoader.load(getClass().getResource("GET STARTED.fxml"));
        Scene chatMainScene = new Scene(chatMainParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(chatMainScene);
        window.show();
    }

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
        
        if (!isValidEmail(email)) {
            messageLabel.setText("Email is not valid.");
            return;
        }

        if(password.length() < 6) {
            messageLabel.setText("Password should be more than 6 characters.");
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

        Dialog<ButtonType> proceedToLogin = new Dialog<>();
            proceedToLogin.setTitle("Confirmation");
            DialogPane dPane = new DialogPane();
            VBox content = new VBox(10);
            content.getChildren().add(new Label("Would you like to proceed to the Log-in Page?"));
            dPane.setContent(content);
            dPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dPane.setGraphic(null);
            proceedToLogin.setDialogPane(dPane);

            proceedToLogin.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    System.out.println("OK button clicked");
                    try{
                        Parent chatMainParent = FXMLLoader.load(getClass().getResource("LOGIN PAGE.fxml"));
                        Scene chatMainScene = new Scene(chatMainParent);

                        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
                        window.setScene(chatMainScene);
                        window.show();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Cancel button clicked");
                }
            });
    
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
        Parent chatMainParent = FXMLLoader.load(getClass().getResource("LOGIN PAGE.fxml"));
        Scene chatMainScene = new Scene(chatMainParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(chatMainScene);
        window.show();
    } 
    
    public boolean isValidEmail(String email) {
        // Regular expression for validating email format
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                            "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        
        Pattern pattern = Pattern.compile(emailRegex);
        
        if (email == null) {
            return false;
        }
        
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}