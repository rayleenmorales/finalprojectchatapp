package com.guiyomi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {
    private User loggedInUser;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GET STARTED.fxml"));
        Parent root = loader.load();
        
        primaryStage.setTitle("KaTalk");
        primaryStage.setScene(new Scene(root));

        // Set Main instance as user data for access in controllers
        primaryStage.setUserData(this);

        // Set on close request for logout confirmation
        primaryStage.setOnCloseRequest(e -> handleLogoutConfirmation(primaryStage, e));
        primaryStage.show();
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    private void handleLogoutConfirmation(Stage stage, WindowEvent event) {
        // Check if the current root is the chat main scene
        Parent currentRoot = stage.getScene().getRoot();
        System.out.println("Current Root ID: " + currentRoot.getId()); // Debugging
        
        if (currentRoot.getId() != null && currentRoot.getId().equals("chatMainScene") && loggedInUser != null) {
            event.consume();  // Prevent immediate window close
            System.out.println("Triggered logout confirmation."); // Debugging
    
            // Retrieve the controller associated with chat main scene
            ChatMainController controller = (ChatMainController) currentRoot.getUserData();
            if (controller != null) {
                SessionService.LogOutConfirmation(stage, loggedInUser, controller.getMainLoop(), controller);
            } else {
                System.out.println("Controller is null, cannot trigger logout confirmation."); // Debugging
            }
        } else {
            System.out.println("Root ID or logged in user missing, no confirmation triggered."); // Debugging
        }
    }    

    public static void main(String[] args) {
        launch(args);
    }
}
