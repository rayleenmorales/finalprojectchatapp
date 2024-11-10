package com.guiyomi;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class GetStartedController {
    @FXML
    private Button signinBtn;

    @FXML
    private Label signupLabel;

    private Main mainApp;

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void HandleSignInBtn(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LOGIN PAGE.fxml"));
        Parent signInParent = loader.load();
        Scene signScene = new Scene(signInParent);
    
        // Get LoginPageController and set mainApp
        LoginPageController loginController = loader.getController();
        loginController.setMainApp(this.mainApp); // Pass mainApp to LoginPageController
    
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(signScene);
        window.show();
    }

    @FXML
    public void initialize() {
        signupLabel.setOnMouseClicked(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("SIGN UP PAGE.fxml"));
                Parent signUpParent = loader.load();
                Scene signUpScene = new Scene(signUpParent);

                // Get SignUpPageController and set mainApp
                SignUpPageController signupController = loader.getController();
                signupController.setMainApp(this.mainApp); // Pass mainApp to SignUpPageController

                Stage window = (Stage)((Node) event.getSource()).getScene().getWindow();
                window.setScene(signUpScene);
                window.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
