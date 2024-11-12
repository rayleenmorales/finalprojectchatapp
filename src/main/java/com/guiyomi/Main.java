package com.guiyomi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {
    private User loggedInUser;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage; // Save reference to primary stage
        CacheHelper.createCacheDirIfNotExists(); // Create cache directory if it doesn't exist

        if (SessionManager.isSessionValid()) {
            String tokenID = SessionManager.getTokenID();
            String localID = SessionManager.getLocalID();
            String userName = SessionManager.getUserName();
            if (tokenID != null && localID != null && userName != null) {
                loggedInUser = new User(tokenID, localID, userName);
                loadMainChatScreen();
            } else {
                loadGetStartedScreen();
            }
        } else {
            loadGetStartedScreen();
        }

        primaryStage.setUserData(this);
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        primaryStage.show();
    }
    
    private void loadMainChatScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MAINCHATNEW.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("KaTalk - Chat");
        
        // Set the initial dimensions as minimum bounds to prevent minimizing by resizing
        primaryStage.setOnShown(event -> {
            primaryStage.setMinWidth(primaryStage.getWidth());
            primaryStage.setMinHeight(primaryStage.getHeight());
        });
    
        // Pass Main instance and logged-in user to ChatMainController
        ChatMainController controller = loader.getController();
        System.out.println("Setting mainApp in ChatMainController");
        controller.setMainApp(this);
        controller.setUser(loggedInUser);
    }

    private void loadGetStartedScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GET STARTED.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("KaTalk");
        primaryStage.setScene(new Scene(root));
        primaryStage.centerOnScreen();

        // Get the controller and set the mainApp instance
        GetStartedController controller = loader.getController();
        controller.setMainApp(this); // Set mainApp in GetStartedController
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    private void handleWindowClose(WindowEvent event) {
        System.out.println("Window closed.");
    }

    public void logout() {
        // Clear session data and set the user to logged out in Firebase
        SessionManager.clearSession();
        
        if (loggedInUser != null) {
            Firebase.updateIsLogged(loggedInUser.getLocalID(), loggedInUser.getTokenID(), false);
        }

        // Reset the logged-in user
        loggedInUser = null;

        // Load the Get Started screen on logout
        try {
            loadGetStartedScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
