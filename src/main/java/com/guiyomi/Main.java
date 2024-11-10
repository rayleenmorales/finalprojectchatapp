package com.guiyomi;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;

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
        // Initialize Firebase
        initializeFirebase();

        this.primaryStage = primaryStage; // Save reference to primary stage
        CacheHelper.createCacheDirIfNotExists(); // Create cache directory if it doesn't exist

        // Enable Firebase persistence
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true); 

        // Set up connectivity listener for Firebase
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    System.out.println("Connected to Firebase");
                    // Optional: Notify the user they are back online
                } else {
                    System.out.println("Disconnected from Firebase");
                    // Optional: Notify the user they are offline
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Connectivity listener was cancelled: " + error.getMessage());
            }
        });

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

    private void initializeFirebase() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FileInputStream serviceAccount = new FileInputStream("config/katalkfirebase-firebase-adminsdk-lcpcd-3edcaacb73.json");

            try {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://katalkfirebase-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .build();
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully.");
            } catch (IOException e) {
                System.err.println("Error initializing Firebase: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("General error initializing Firebase: " + e.getMessage());
                e.printStackTrace();
            }
            
        } else {
            System.out.println("Firebase is already initialized.");
        }
    }
    
    private void loadMainChatScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MAINCHAT5.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("KaTalk - Chat");

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
