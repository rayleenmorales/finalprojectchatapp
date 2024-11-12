package com.guiyomi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TicTacToe extends JPanel implements ActionListener {

    // Core logic variables
    static boolean playerX; // true if player X's turn, false if player O's turn
        boolean gameDone = false; // true if game is over
        int winner = -1; // 0 if X wins, 1 if O wins, -1 if no winner yet
        int player1wins = 0, player2wins = 0; // number of wins for each player
        static int[][] board = new int[3][3]; // 0 if empty, 1 if X, 2 if O
            
                // Paint variables
                int lineWidth = 5;
                int lineLength = 270;
                int x = 15, y = 100;
                int offset = 95;
                int selX = 0;
                int selY = 0;
            
                // Colors
                Color turtle = new Color(152, 109, 142);
                Color orange = new Color(255, 165, 0);
                Color offwhite = new Color(0xf7f7f7);
                Color darkgray = new Color(239, 227, 208);
                Color pink = new Color(130, 92, 121);
            
                // Firebase URL and button component
                private static final String DATABASE_URL = "https://katalkfirebase-default-rtdb.asia-southeast1.firebasedatabase.app";
            
                private static String gameId;  // Declare as static
            
                public static String generateUniqueGameId() {
                    return UUID.randomUUID().toString();
                }
            
                // Method to generate Firebase auth token for the game
            public static String getAuthToken(User user) {
                // Generate the Firebase auth token using the user object (e.g., a token provided during user sign-in)
                String tokenID = user.getTokenID();  // Assuming you have the token stored in the User class
                return tokenID;  // Return the token for authorization in HTTP requests
            }
            
            public void startGameSession() {
                gameId = createGameSession(user);  // Creates a new game session in Firebase
                System.out.println("Game session started with ID: " + gameId);
                playerX = true; // The current user (inviter) is always player X (first player)
                player1wins = 0;  // Reset player wins, if needed
                player2wins = 0;
            }

            public void joinGameSession(String gameId, User invitee) {
                // Use the gameId to fetch the game session from Firebase
                String authToken = getAuthToken(invitee);
                
                // Update game state to reflect the second player joining
                JsonObject gameState = new JsonObject();
                gameState.addProperty("playerO", invitee.getUserName());  // Assign the second player as playerO
                updateGameStateInFirebase(gameId, authToken);  // Update Firebase with new game state
            }
            
            
            
            
                
            public static String createGameSession(User user) {
                gameId = generateUniqueGameId();  // Generate a unique game ID
                String authToken = getAuthToken(user);  // Get the auth token for Firebase
                updateGameStateInFirebase(gameId, authToken);  // Update Firebase with initial game state
                    return gameId;
                }
                
                public String fetchGameIdFromFirebase(String authToken) {
                    try {
                        String firebaseUrl = DATABASE_URL + "/Tictactoe/" + gameId + ".json?auth=" + authToken;
                        URL url = new URL(firebaseUrl);
                
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setDoOutput(true);
                
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                            JsonObject responseJson = JsonParser.parseReader(reader).getAsJsonObject();
                
                            if (responseJson != null && responseJson.has("value")) {
                                return responseJson.get("value").getAsString();
                            }
                        } else {
                            System.err.println("Error fetching game ID: " + connection.getResponseMessage());
                        }
                    } catch (Exception e) {
                        System.err.println("Error fetching game ID: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                }
                
                
                
                    // Method to update the game state in Firebase
                    public static void updateGameStateInFirebase(String gameId, String authToken) {
                    try {
                        JsonObject gameState = new JsonObject();
                        JsonObject boardData = new JsonObject();
                
                        // Populate board state for Firebase
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) {
                                boardData.addProperty(i + "_" + j, board[i][j]);
                    }
                }
        
                gameState.add("board", boardData);
                gameState.addProperty("currentTurn", playerX ? "X" : "O");
    
            // Convert the state to JSON and update Firebase
            String gameStateJson = gameState.toString();
            String firebaseUrl = DATABASE_URL + "/Tictactoe/" + gameId + ".json?auth=" + authToken;
            URL url = new URL(firebaseUrl);
    
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
    
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = gameStateJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
    
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Game state updated successfully");
            } else {
                // Log the error response
                String responseMessage = connection.getResponseMessage();
                String errorResponse = new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                System.err.println("Error updating game state: " + responseMessage + " - " + errorResponse);
            }
            
    
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("Error updating game state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    


    
    
    JButton jButton;

    // Add fields for User
    private User user;

    // Make GameWindow static to avoid non-static inner class error
    public static class GameWindowTictac {
        public static final int WIDTH = 420;
        public static final int HEIGHT = 320;
        
        public GameWindowTictac(int width, int height, String title, JPanel gamePanel) {
            JFrame frame = new JFrame(title);
            frame.setSize(width, height);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Close frame when done
            frame.setResizable(false);
            frame.add(gamePanel); // Add TicTacToe or any game panel directly here
            frame.setVisible(true);
        }
    }

    // Constructor to accept User object
    public TicTacToe(User user) {
        if (user != null) {
            this.user = user;
        } else {
            System.err.println("Please select an active user first.");
        }
    
        String idToken = user.getTokenID();  // Get the token for authentication
        String gameId = fetchGameIdFromFirebase(idToken);  // Try fetching the gameId from Firebase
    
        if (gameId != null) {
            this.gameId = gameId;
        } else {
            // Create a game session using the instance method
            this.gameId = createGameSession(user);
        }
    
    
        Dimension size = new Dimension(420, 300);
        setPreferredSize(size);
        setMaximumSize(size);
        setMinimumSize(size);
        addMouseListener(new XOListener());
        jButton = new JButton("New Game");
        jButton.addActionListener(this);
        jButton.setBounds(315, 210, 100, 30);
        add(jButton);
        resetGame();
        Firebase.listenForGameUpdates(gameId, this);  // Use the gameId for real-time updates
    }

    // Method to end the game and update the game state
    public void endGame(int winner) {
        this.winner = winner;
        gameDone = true;
        updateGameStateInFirebase(gameId, user.getTokenID());  // Update the game state when the game ends
        
        // Update win count for player X or player O
        if (winner == 1) {
            player1wins++;
        } else if (winner == 2) {
            player2wins++;
        }
        
        // Update the Firebase win count
        JsonObject winCount = new JsonObject();
        winCount.addProperty("player1wins", player1wins);
        winCount.addProperty("player2wins", player2wins);
        
        String firebaseUrl = DATABASE_URL + "/Tictactoe/winCount.json?auth=" + user.getTokenID();
        try {
            URL url = new URL(firebaseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
    
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = winCount.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
    
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Win count updated successfully");
            } else {
                System.err.println("Error updating win count: " + connection.getResponseMessage());
            }
    
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("Error updating win count: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Logic for checking the game state and determining the winner
    public void checkWinner() {
        // Check rows, columns, and diagonals for a winner
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != 0 && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                endGame(board[i][0]);  // Call endGame with the winner (1 for X, 2 for O)
                return;
            }
            if (board[0][i] != 0 && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                endGame(board[0][i]);  // Call endGame with the winner
                return;
            }
        }

        if (board[0][0] != 0 && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            endGame(board[0][0]);  // Call endGame with the winner
            return;
        }

        if (board[0][2] != 0 && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            endGame(board[0][2]);  // Call endGame with the winner
            return;
        }

        // Check for a draw (if the board is full and no winner)
        boolean fullBoard = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 0) {
                    fullBoard = false;
                    break;
                }
            }
        }

        if (fullBoard) {
            endGame(0);  // Draw
        }
    }


    // Trigger the check for a winner after each move
    public void makeMove(int row, int col) {
        if (gameDone || board[row][col] != 0) return;  // Ignore moves if the game is over or the spot is taken

        board[row][col] = playerX ? 1 : 2;  // Player X = 1, Player O = 2
        playerX = !playerX;  // Switch turn
        updateGameStateInFirebase(gameId, getAuthToken(user));  // Update Firebase with new board state

        checkWinner();  // Check if the current move results in a winner or draw
    }

    public void resumeGameSession() {
        String existingGameId = fetchGameIdFromFirebase(getAuthToken(user));
        if (existingGameId != null) {
            gameId = existingGameId;
            System.out.println("Resumed game session with ID: " + gameId);
            Firebase.listenForGameUpdates(gameId, this);
        } else {
            System.out.println("No active game session found.");
        }
    }
    


    
    

    public void resetGame() {
        playerX = true;  // X starts first
        winner = -1;
        gameDone = false;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = 0;  // Reset the board
            }
        }
        jButton.setVisible(false);  // Hide the "New Game" button initially
    }

    public void paintComponent(Graphics page) {
        super.paintComponent(page);
        drawBoard(page);
        drawUI(page);
        drawGame(page);
    }

    public void drawBoard(Graphics page) {
        setBackground(turtle);
        page.setColor(darkgray);
        page.fillRoundRect(x, y, lineLength, lineWidth, 5, 30);
        page.fillRoundRect(x, y + offset, lineLength, lineWidth, 5, 30);
        page.fillRoundRect(y, x, lineWidth, lineLength, 30, 5);
        page.fillRoundRect(y + offset, x, lineWidth, lineLength, 30, 5);
    }

    public void drawUI(Graphics page) {
        // Set color and font
        page.setColor(pink);
        page.fillRect(300, 0, 120, 300);
        Font font = new Font("Helvetica", Font.PLAIN, 20);
        page.setFont(font);

        // Set win counter
        page.setColor(offwhite);
        page.drawString("Win Count", 310, 30);
        page.drawString(": " + player1wins, 362, 70);
        page.drawString(": " + player2wins, 362, 105);

        // Draw player indicators
        page.drawString("X Wins: " + player1wins, 310, 70);
        page.drawString("O Wins: " + player2wins, 310, 105);

        if (gameDone) {
            if (winner == 1) {
                page.drawString("X Wins!", 325, 180);
            } else if (winner == 2) {
                page.drawString("O Wins!", 325, 180);
            } else {
                page.drawString("It's a Tie!", 325, 180);
            }
        } else {
            if (playerX) {
                page.drawString("X's Turn", 325, 180);
            } else {
                page.drawString("O's Turn", 325, 180);
            }
        }
    }

    public void drawGame(Graphics page) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 1) {  // X is 1
                    ImageIcon xIcon = new ImageIcon("src/main/resources/com/guiyomi/Images/TicTacToe/orangex.png");
                    Image xImg = xIcon.getImage();
                    page.drawImage(xImg, 30 + offset * i, 30 + offset * j, null);
                } else if (board[i][j] == 2) {  // O is 2
                    page.setColor(offwhite);
                    page.fillOval(30 + offset * i, 30 + offset * j, 50, 50);
                    page.setColor(turtle);
                    page.fillOval(40 + offset * i, 40 + offset * j, 30, 30);
                }
            }
        }
    }

 

    @Override
    public void actionPerformed(ActionEvent e) {
        // Reset the game only when the New Game button is pressed
        resetGame();
        repaint();
    }
    
    // Add listener to handle moves
    private class XOListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (gameDone || user == null) return;  // Prevent moves if game is done or user is not authenticated
    
            int row = (e.getX() - 30) / offset;
            int col = (e.getY() - 30) / offset;
    
            if (board[row][col] != 0) return; // If already filled, don't do anything
    
            if (playerX) {
                board[row][col] = 1;  // Player X
            } else {
                board[row][col] = 2;  // Player O
            }
    
            playerX = !playerX; // Switch turns
            checkWinner();
            repaint();
    
            // Pass the gameId and idToken to updateGameStateInFirebase
            String gameId = "your_game_id";  // Replace with actual game ID
            String idToken = user.getTokenID();  // Get the ID token from the authenticated user
            updateGameStateInFirebase(gameId, idToken);  // Call the method with required arguments
        }
    }
    
    
}