package com.guiyomi;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginPageController {
    @FXML
    private TextField emailField;

    @FXML
    private TextField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginBtn;

    @FXML
    private Button homeBtn;

    private Main mainApp;

    @FXML
    public void handleHomeButton(ActionEvent event) throws Exception {
        Parent chatMainParent = FXMLLoader.load(getClass().getResource("GET STARTED.fxml"));
        Scene chatMainScene = new Scene(chatMainParent);

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(chatMainScene);
        window.centerOnScreen();
        window.show();
    }

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    public void handleLoginButton(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();
    
        new Thread(() -> {
            try {
                User user = Firebase.signIn(email, password);

                if (user != null) {
                    System.out.println("User: " + user.getUserName());
                    System.out.println("Token ID: " + user.getTokenID());
                    System.out.println("Local ID: " + user.getLocalID());

                    // Start session and save user data
                    SessionManager.saveSession(user.getTokenID(), user.getLocalID(), user.getUserName());
    
                    // Update user status to logged in and navigate to main chat
                    Firebase.updateIsLogged(user.getLocalID(), user.getTokenID(), true);
                    Platform.runLater(() -> navigateToMainChat(event, user));
                } else {
                    Platform.runLater(() -> messageLabel.setText("Invalid login credentials."));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> messageLabel.setText("An error occurred during login."));
            }
        }).start();
    }

    private void navigateToMainChat(ActionEvent event, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MAINCHAT5.fxml"));
            Parent chatMainParent = loader.load();
    
            // Pass User and Main instance to ChatMainController
            ChatMainController mainChatController = loader.getController();
            mainChatController.setUser(user);
            mainChatController.setMainApp(this.mainApp); // Set mainApp in ChatMainController
    
            System.out.println("Navigating to Main Chat...");
            System.out.println("User name passed to Main Chat: " + user.getUserName());
            System.out.println("MainApp passed to Main Chat: " + mainApp);
    
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            mainApp.setLoggedInUser(user); // Set loggedInUser in mainApp for consistency
            stage.setScene(new Scene(chatMainParent));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error navigating to main chat.");
        }
    }
    
    
}
