package com.guiyomi;

import java.io.File;

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

    private File profilePhoto;

    @FXML
    public void handleHomeButton(ActionEvent event) throws Exception {
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
        String userName = firstName + " " + lastName;

        // Validating inputs before sign-up
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmedPassword.isEmpty() || profilePhoto == null) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }
        
        if (!SessionService.isValidEmail(email)) {
            messageLabel.setText("Email is not valid.");
            return;
        }

        if (password.length() < 6) {
            messageLabel.setText("Password should be more than 6 characters.");
            return;
        }

        if (!password.equals(confirmedPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        new Thread(() -> {
            try {
                User newUser = Firebase.signUp(email, confirmedPassword, userName);
                
                Platform.runLater(() -> {
                    if (newUser != null) {
                        messageLabel.setText("Signup successful!");

                        // Show confirmation dialog immediately after successful sign-up
                        showProceedToLoginDialog(event);
                    }
                });

                if (newUser != null) {
                    String profilePhotoURL = Firebase.uploadProfile(newUser.getTokenID(), profilePhoto);
                    Firebase.updateUserInfo(newUser.getLocalID(), newUser.getTokenID(), profilePhotoURL, profilePhoto.getName());
                }

            } catch (FirebaseAuthException ex) {
                Platform.runLater(() -> handleFirebaseAuthException(ex));
            }
        }).start();
    }

    // Method to show confirmation dialog box
    private void showProceedToLoginDialog(ActionEvent event) {
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
                try {
                    Parent chatMainParent = FXMLLoader.load(getClass().getResource("LOGIN PAGE.fxml"));
                    Scene chatMainScene = new Scene(chatMainParent);

                    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    window.setScene(chatMainScene);
                    window.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Cancel button clicked");
            }
        });
    }

    // Method to handle FirebaseAuthException
    private void handleFirebaseAuthException(FirebaseAuthException ex) {
        String errorCode = ex.getErrorCode();
        switch (errorCode) {
            case "EMAIL_EXISTS":
                messageLabel.setText("Error. The email address is already in use.");
                break;
            case "INVALID_EMAIL":
                messageLabel.setText("Error. The email address is not valid.");
                break;
            case "USERNAME_EXISTS":
                messageLabel.setText("Error. The username is already taken.");
                break;
            case "WEAK_PASSWORD : Password should be at least 6 characters":
                messageLabel.setText("Error. The password is too weak.");
                break;
            default:
                messageLabel.setText("Error. Signup failed. Please try again.");
                break;
        }
    }
      

    @FXML
    public void handleChooseProfileButton(ActionEvent event) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        profilePhoto = fileChooser.showOpenDialog(chooseProfileBtn.getScene().getWindow());

        if (profilePhoto == null) {
            System.out.println("No profile photo selected.");
        }
    }

    @FXML
    public void handleLoginButton(ActionEvent event) throws Exception {
        Parent chatMainParent = FXMLLoader.load(getClass().getResource("LOGIN PAGE.fxml"));
        Scene chatMainScene = new Scene(chatMainParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(chatMainScene);
        window.show();
    }
}
