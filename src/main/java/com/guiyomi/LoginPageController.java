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

    private User user;

    @FXML
    public void handleHomeButton(ActionEvent event) throws Exception {
        Parent chatMainParent = FXMLLoader.load(getClass().getResource("GET STARTED.fxml"));
        Scene chatMainScene = new Scene(chatMainParent);

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(chatMainScene);
        window.centerOnScreen();
        window.show();
    }

    @FXML
    public void handleLoginButton(ActionEvent event) throws Exception {
        this.user = Firebase.signIn(emailField.getText(), passwordField.getText());

        if (this.user != null) {
            Firebase.updateIsLogged(this.user.getLocalID(), this.user.getTokenID(), true);
            this.user.setFileName(Firebase.getFileName(this.user.getLocalID(), this.user.getTokenID()));
            this.user.setProfilePhotoURL(Firebase.getProfilePhotoURL(this.user.getLocalID(), this.user.getTokenID()));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MAINCHAT5.fxml"));
            Parent chatMainParent = loader.load();

            // Pass User to MainChatController
            ChatMainController mainChatController = loader.getController();
            mainChatController.setUser(this.user);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Main mainApp = (Main) stage.getUserData();
            mainApp.setLoggedInUser(this.user);

            // Set controller as user data for logout handling
            chatMainParent.setUserData(mainChatController);

            Scene chatMainScene = new Scene(chatMainParent);
            stage.setScene(chatMainScene);
            stage.show();
        } else {
            messageLabel.setText("Invalid login credentials.");
        }
    }

}
