package com.guiyomi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Toolkit;
import com.google.gson.JsonObject;
import javafx.animation.AnimationTimer;

public class TicTacToe extends JPanel implements ActionListener {

    // Core logic variables
    static boolean playerX; // true if player X's turn, false if player O's turn
    boolean gameDone = false; // true if game is over
    int winner = -1; // 0 if X wins, 1 if O wins, -1 if no winner yet
    int player1wins = 0, player2wins = 0; // number of wins for each player
    static int[][] board = new int[3][3]; // 0 if empty, 1 if X, 2 if O
    static String gameId = "";  // Unique game ID for Firebase

    // For firebase variables
    private User user;
    private User selectedUser;
    JButton jButton;
    private AnimationTimer moveUpdateLoop;
    private JsonObject updateMoves;
            
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

    // Constructor to accept User object
    public TicTacToe(User user, User selectedUser) {
        try {
            this.user = user;
            this.selectedUser = selectedUser;
        } catch (Exception e) {
            e.printStackTrace();
        }

        Dimension size = new Dimension(420, 300); // size of the panel
        setPreferredSize(size);
        setMaximumSize(size);
        setMinimumSize(size);
        jButton = new JButton("New Game");
        jButton.addActionListener(this);
        jButton.setBounds(315, 210, 100, 30); // set button location
        add(jButton);

        startOrResumeGame();
        addMouseListener(new XOListener());
        startMoveUpdateLoop();
        resetGame();
    }

    // For real-time updates
    private void startMoveUpdateLoop() {
        if(moveUpdateLoop == null) {
            moveUpdateLoop = new AnimationTimer() {
                private long lastUpdate = 0;

                @Override
                public void handle(long now) {
                    if (now - lastUpdate >= 1_000_000_000) {
                        lastUpdate = now;
                        Thread moveUpdateThread = new Thread(moveUpdateTask);
                        moveUpdateThread.setDaemon(true);
                        moveUpdateThread.start();
                    }
                }
            };
        }
        moveUpdateLoop.start();
    }

    Runnable moveUpdateTask = () -> {
        updateMoves = Firebase.loadGameStateFromFirebase(gameId, user.getTokenID());
        if (updateMoves != null) {
            updateLocalGameState(updateMoves);
        }
    };

    public void stopMoveUpdateLoop() {
        if (moveUpdateLoop != null) {
            moveUpdateLoop.stop();
        }
    }

    private class XOListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (gameDone || !isPlayerTurn()) return;

            int row = (e.getX() - 30) / offset;
            int col = (e.getY() - 30) / offset;
    
            if (board[row][col] != 0) return; // If already filled, don't do anything
    
            if (playerX) {
                board[row][col] = 1;  // Player X
            } else {
                board[row][col] = 2;  // Player O
            }
    
            board[row][col] = playerX ? 1 : 2; 
            repaint(); 
            switchPlayerTurn(); 
            checkGameStatus(); 

            JsonObject gameState = compileGameState();
            Firebase.sendUpdatedGameState(gameId, gameState, user.getTokenID()); // Send to Firebase
        }
    }

    public void checkWinner() {
        if (gameDone == true) {
            System.out.print("gameDone");
            return;
        }
        // vertical
        int temp = -1;
        if ((board[0][0] == board[0][1])
                && (board[0][1] == board[0][2])
                && (board[0][0] != 0)) {
            temp = board[0][0];
        } else if ((board[1][0] == board[1][1])
                && (board[1][1] == board[1][2])
                && (board[1][0] != 0)) {
            temp = board[1][1];
        } else if ((board[2][0] == board[2][1])
                && (board[2][1] == board[2][2])
                && (board[2][0] != 0)) {
            temp = board[2][1];

            // horizontal
        } else if ((board[0][0] == board[1][0])
                && (board[1][0] == board[2][0])
                && (board[0][0] != 0)) {
            temp = board[0][0];
        } else if ((board[0][1] == board[1][1])
                && (board[1][1] == board[2][1])
                && (board[0][1] != 0)) {
            temp = board[0][1];
        } else if ((board[0][2] == board[1][2])
                && (board[1][2] == board[2][2])
                && (board[0][2] != 0)) {
            temp = board[0][2];

            // diagonal
        } else if ((board[0][0] == board[1][1])
                && (board[1][1] == board[2][2])
                && (board[0][0] != 0)) {
            temp = board[0][0];
        } else if ((board[0][2] == board[1][1])
                && (board[1][1] == board[2][0])
                && (board[0][2] != 0)) {
            temp = board[0][2];
        } else {

            // CHECK FOR A TIE
            boolean notDone = false;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == 0) {
                        notDone = true;
                        break;
                    }
                }
            }
            if (notDone == false) {
                temp = 3;
            }
        }
        if (temp > 0) {
            winner = temp;
            if (winner == 1) {
                player1wins++;
                System.out.println("winner is X");
            } else if (winner == 2) {
                player2wins++;
                System.out.println("winner is O");
            } else if (winner == 3) {
                System.out.println("It's a tie");
            }
            gameDone = true;
            getJButton().setVisible(true);
        }
    }

    public JButton getJButton() {
        return jButton;
    }

    public void setPlayerXWins(int a) {
        player1wins = a;
    }

    public void setPlayerOWins(int a) {
        player2wins = a;
    }
    // Checks if it's the current player's turn
    private boolean isPlayerTurn() {
        return playerX && user.getLocalID().equals("PlayerX_ID") || !playerX && user.getLocalID().equals("PlayerO_ID");
    }
    
    // Method to compile the current game state into JSON format
    private JsonObject compileGameState() {
        JsonObject gameState = new JsonObject();
        JsonObject boardData = new JsonObject();
    
        // Compile board data
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardData.addProperty(i + "_" + j, board[i][j]);
            }
        }
        
        gameState.add("board", boardData);
        gameState.addProperty("currentTurn", playerX ? "X" : "O");
        gameState.addProperty("gameDone", gameDone);
        gameState.addProperty("winner", winner);
        gameState.addProperty("player1wins", player1wins);
        gameState.addProperty("player2wins", player2wins);
        
        return gameState;
    }
    
    // Toggle player turns
    private void switchPlayerTurn() {
        playerX = !playerX;
    }
    
            
    private String generateUniqueGameId() {
        if (user == null || selectedUser == null) {
            System.out.println("Cannot generate game ID, user or selectedUser is null.");
            return ""; // Return empty string if user or selectedUser is null
        }
    
        String userId1 = user.getLocalID();
        String userId2 = selectedUser.getLocalID();
        String gameId = (userId1.compareTo(userId2) < 0) ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
        System.out.println("Generated conversation ID: " + gameId);

        return gameId;
    }

    public void startOrResumeGame() {
        gameId = generateUniqueGameId();  // Generate a unique game ID
        
        if (Firebase.doesGameExist(gameId, user.getTokenID())) {
            System.out.println("Game already exists. Loading existing game data...");
            JsonObject gameState = Firebase.loadGameStateFromFirebase(gameId, user.getTokenID());
            updateLocalGameState(gameState); 
        } else {
            System.out.println("Starting a new game...");
            Firebase.initializeNewGameInFirebase(gameId, user.getTokenID()); 
            startGameSession();
        }

    }

    private void updateLocalGameState(JsonObject gameState) {
        // Update board state
        if (gameState.has("board")) {
            JsonObject boardData = gameState.getAsJsonObject("board");
            for (String key : boardData.keySet()) {
                String[] indices = key.split("_");
                int row = Integer.parseInt(indices[0]);
                int col = Integer.parseInt(indices[1]);
                board[row][col] = boardData.get(key).getAsInt();
            }
        }
    
        // Update other variables
        if (gameState.has("currentTurn")) {
            playerX = gameState.get("currentTurn").getAsString().equals("X");
        }
        if (gameState.has("gameDone")) {
            gameDone = gameState.get("gameDone").getAsBoolean();
        }
        if (gameState.has("winner")) {
            winner = gameState.get("winner").getAsInt();
        }
        if (gameState.has("player1wins")) {
            player1wins = gameState.get("player1wins").getAsInt();
        }
        if (gameState.has("player2wins")) {
            player2wins = gameState.get("player2wins").getAsInt();
        }
    
        repaint(); // Update the UI
    }
    
    public void startGameSession() {
        playerX = true; // The current user (inviter) is always player X (first player)
        player1wins = 0;  // Reset player wins, if needed
        player2wins = 0;

        jButton = new JButton("New Game");
        jButton.addActionListener(this);
        jButton.setBounds(315, 210, 100, 30);
        add(jButton);
        jButton.setVisible(false);
        repaint();
        resetGame();
    }

    // Method to check if there is a winner or if the game is a draw
    private void checkGameStatus() {
        // Check rows and columns for a win
        for (int i = 0; i < 3; i++) {
            // Check rows
            if (board[i][0] != 0 && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                winner = board[i][0]; // 1 for X, 2 for O
                gameDone = true;
                updateWinCount(winner);
                return;
            }
            // Check columns
            if (board[0][i] != 0 && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                winner = board[0][i];
                gameDone = true;
                updateWinCount(winner);
                return;
            }
        }

        // Check diagonals for a win
        if (board[0][0] != 0 && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            winner = board[0][0];
            gameDone = true;
            updateWinCount(winner);
            return;
        }
        if (board[0][2] != 0 && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            winner = board[0][2];
            gameDone = true;
            updateWinCount(winner);
            return;
        }

        // Check for a draw (if board is full and no winner)
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
            winner = 0; // 0 indicates a draw
            gameDone = true;
            System.out.println("The game is a draw.");
        }
    }

    // Method to update the win count based on the winner
    private void updateWinCount(int winner) {
        if (winner == 1) {
            player1wins++; // Player X wins
            System.out.println("Player X wins!");
        } else if (winner == 2) {
            player2wins++; // Player O wins
            System.out.println("Player O wins!");
        }
}


    // Make Game static to avoid non-static inner class error
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

    public void resetGame() {
        playerX = true;  // X starts first
        winner = -1;
        gameDone = false;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = 0;  // Clear the board for a new game
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

        // DRAW score X
        ImageIcon xIcon = new ImageIcon("orangex.png");
        Image xImg = xIcon.getImage();
        Image newXImg = xImg.getScaledInstance(27, 27, java.awt.Image.SCALE_SMOOTH);
        ImageIcon newXIcon = new ImageIcon(newXImg);
        page.drawImage(newXIcon.getImage(), 44 + offset * 1 + 190, 47 + offset * 0, null);

        // DRAW score O
        page.setColor(offwhite);
        page.fillOval(43 + 190 + offset, 80, 30, 30);
        page.setColor(darkgray);
        page.fillOval(49 + 190 + offset, 85, 19, 19);

        // DRAW WHOS TURN or WINNER
        page.setColor(offwhite);
        Font font1 = new Font("Serif", Font.ITALIC, 18);
        page.setFont(font1);

        if (gameDone) {
            if (winner == 1) { // x
                page.drawString("The winner is", 310, 150);
                page.drawImage(xImg, 335, 160, null);
            } else if (winner == 2) { // o
                page.drawString("The winner is", 310, 150);
                page.setColor(offwhite);
                page.fillOval(332, 160, 50, 50);
                page.setColor(darkgray);
                page.fillOval(342, 170, 30, 30);
            } else if (winner == 3) { // tie
                page.drawString("It's a tie", 330, 178);
            }
        } else {
            Font font2 = new Font("Serif", Font.ITALIC, 20);
            page.setFont(font2);
            page.drawString("", 350, 160);
            if (playerX) {
                page.drawString("X 's Turn", 325, 180);
            } else {
                page.drawString("O 's Turn", 325, 180);
            }
        }
        // DRAW LOGO
        Image cookie = Toolkit.getDefaultToolkit().getImage("logo.png");
        page.drawImage(cookie, 345, 235, 30, 30, this);
        Font c = new Font("Courier", Font.BOLD + Font.CENTER_BASELINE, 13);
        page.setFont(c);
        page.drawString("Tic Tac Toe", 310, 280);
    }

    public void drawGame(Graphics page) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 0) {

                } else if (board[i][j] == 1) {
                    ImageIcon xIcon = new ImageIcon("orangex.png");
                    Image xImg = xIcon.getImage();
                    page.drawImage(xImg, 30 + offset * i, 30 + offset * j, null);
                } else if (board[i][j] == 2) {
                    page.setColor(offwhite);
                    page.fillOval(30 + offset * i, 30 + offset * j, 50, 50);
                    page.setColor(turtle);
                    page.fillOval(40 + offset * i, 40 + offset * j, 30, 30);
                }
            }
        }
        repaint();
    }
    

 

    @Override
    public void actionPerformed(ActionEvent e) {
        // Reset the game only when the New Game button is pressed
        resetGame();
        repaint();
    }
    
}