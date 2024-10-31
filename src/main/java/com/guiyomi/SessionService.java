package com.guiyomi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SessionService {
    
    public static boolean isValidEmail(String email) {
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

    public static void LogOutConfirmation(Stage stage, User user, AnimationTimer mainLoop, ChatMainController controller) {
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
                System.out.println("Proceeding with logout.");
    
                try {
                    // Update user status in Firebase
                    Firebase.updateIsLogged(user.getLocalID(), user.getTokenID(), false);
    
                    // Stop the update loop and clear user data
                    if (mainLoop != null) {
                        mainLoop.stop();
                    }
                    controller.clearUserData();
    
                    // Redirect to login screen
                    Parent root = FXMLLoader.load(SessionService.class.getResource("LOGIN PAGE.fxml"));
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Logout failed.");
                }
            } else if (response == ButtonType.NO) {
                System.out.println("Logout canceled.");
            }
        });
    }    
    
}
