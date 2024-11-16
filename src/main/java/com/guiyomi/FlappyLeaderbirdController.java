package com.guiyomi;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;

public class FlappyLeaderbirdController {
    @FXML
    private VBox leaderBoard;

    private User user;

    @FXML
    public void initialize() {
        initializeScoresUpdateLoop();
    }

    private void initializeScoresUpdateLoop() {
        new Thread(() -> {
            while (true) {
                try {
                    // Fetch highscores from Firebase
                    ArrayList<JsonObject> highscores = Firebase.fetchAllHighScores(user.getTokenID());
    
                    Platform.runLater(() -> {
                        if (leaderBoard == null || leaderBoard.getScene() == null || leaderBoard.getScene().getWindow() == null) {
                            return;
                        }
    
                        // Clear the leaderboard safely
                        leaderBoard.getChildren().clear();
    
                        // Sort highscores before creating panes
                        highscores.sort((a, b) -> {
                            int scoreA = a.has("highscore") ? a.get("highscore").getAsInt() : 0;
                            int scoreB = b.has("highscore") ? b.get("highscore").getAsInt() : 0;
                            return Integer.compare(scoreB, scoreA);
                        });
    
                        // Limit to top 10 scores
                        int limit = Math.min(10, highscores.size());
                        for (int i = 0; i < limit; i++) {
                            JsonObject highscore = highscores.get(i);
                            String name = highscore.has("username") ? highscore.get("username").getAsString() : "Unknown";
                            String profileURL = highscore.has("profileURL") ? highscore.get("profileURL").getAsString() : "";
                            String userId = highscore.has("userId") ? highscore.get("userId").getAsString() : "";
                            int score = highscore.has("highscore") ? highscore.get("highscore").getAsInt() : 0;
    
                            Pane highscorePane = createHighScorePane(name, profileURL, userId, score);
                            leaderBoard.getChildren().add(highscorePane);
                        }
                    });
    
                    // Pause for 5 seconds before updating again
                    Thread.sleep(5000);
    
                    // Break the loop if the window is closed
                    if (leaderBoard.getScene() == null || !leaderBoard.getScene().getWindow().isShowing()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    

    private Pane createHighScorePane(String name, String profileURL, String userId, int score) {
        Pane highscorePane = new Pane();
        highscorePane.setPrefSize(306, 79);
        highscorePane.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: lightgray; -fx-border-width: 1;");

        Circle profilePic = new Circle(31.0);
        profilePic.setFill(Color.LIGHTGRAY);
        profilePic.setLayoutX(45.0);
        profilePic.setLayoutY(40.0);

        if (profileURL != null && !profileURL.isEmpty()) {
            Platform.runLater(() -> {
                try {
                    // Load profile picture from cache or Firebase
                    File cachedImage = new File("cached_profiles/" + userId + ".jpg");
                    if (cachedImage.exists()) {
                        profilePic.setFill(new ImagePattern(new Image(cachedImage.toURI().toString())));
                    } else {
                        // Use a default profile picture from resources
                        profilePic.setFill(new ImagePattern(new Image(getClass().getResourceAsStream("src\\main\\resources\\com\\guiyomi\\Images\\default.jpg"))));
                    }

                } catch (Exception e) {
                    profilePic.setFill(Color.LIGHTGRAY);
                    System.out.println("Failed to load profile picture: " + e.getMessage());
                }
            });
        }

        Label nameLabel = new Label(name);
        nameLabel.setLayoutX(86.0);
        nameLabel.setLayoutY(10.0);
        nameLabel.setFont(javafx.scene.text.Font.font("Arial Rounded MT Bold", 16.0));

        Label scoreLabel = new Label("Score: " + score);
        scoreLabel.setLayoutX(86.0);
        scoreLabel.setLayoutY(30.0);
        scoreLabel.setFont(javafx.scene.text.Font.font("Arial", 14.0));

        highscorePane.getChildren().addAll(profilePic, nameLabel, scoreLabel);
        highscorePane.setUserData(name);

        return highscorePane;
    }

    @FXML
    public void handleStartButton(ActionEvent event) {
        // Close the leaderboard window
        Stage stage = (Stage) leaderBoard.getScene().getWindow();
        stage.close();

        // Launch the Flappy Bird game
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    Game game = new Game(this.user);
                    new GameWindow(GameWindow.WIDTH, GameWindow.HEIGHT, "Flappy Bird", game);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setUser(User user) {
        this.user = user;
    }
}
