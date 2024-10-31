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

    @FXML
    public void HandleSignInBtn(ActionEvent event) throws Exception {
        Parent signInParent = FXMLLoader.load(getClass().getResource("LOGIN PAGE.fxml"));
        Scene signScene = new Scene(signInParent);

        Stage window = (Stage)((Node) event.getSource()).getScene().getWindow();
        window.setScene(signScene);
        window.show();
    }

    @FXML
    public void initialize() {
        signupLabel.setOnMouseClicked(event -> {
            try {
                Parent signUpParent = FXMLLoader.load(getClass().getResource("SIGN UP PAGE.fxml"));
                Scene signUpScene = new Scene(signUpParent);

                Stage window = (Stage)((Node) event.getSource()).getScene().getWindow();
                window.setScene(signUpScene);
                window.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
