package com.guiyomi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.google.gson.JsonObject;
import javafx.animation.AnimationTimer; // JavaFX for AnimationTimer

// Ensure that classes like User and Firebase are properly imported or defined
// import com.guiyomi.User;       // Custom User class, if applicable
// import com.guiyomi.Firebase;   // Custom Firebase class, if applicable


public class Game2 extends JPanel implements ActionListener {

    // core logic variables
    boolean playerX; // true if player X's turn, false if player O's turn
    boolean gameDone = false; // true if game is over
    int winner = -1; // 0 if X wins, 1 if O wins, -1 if no winner yet
    int player1wins = 0, player2wins = 0; // number of wins for each player
    int[][] board = new int[3][3]; // 0 if empty, 1 if X, 2 if O

    // paint variables
    int lineWidth = 5; // width of the lines
    int lineLength = 270; // length of the lines
    int x = 15, y = 100; // location of first line
    int offset = 95; // square width
    int a = 0; // used for drawing the X's and O's
    int b = 5; // used for drawing the X's and O's
    int selX = 0; // selected square x
    int selY = 0; // selected square y

    // COLORS
    Color turtle = new Color(152, 109, 142);
    Color orange = new Color(255, 165, 0);
    Color offwhite = new Color(0xf7f7f7);
    Color darkgray = new Color(239, 227, 208);
    Color pink = new Color(130, 92, 121);

    // COMPONENTS
    JButton jButton;

    // FOR FIREBASE VARIABLES
    private User user;
    private User selectedUser;
    private boolean isPlayerX;
    private AnimationTimer moveUpdateLoop;
    private JsonObject updateMoves;
    static String gameId;

    // CONSTRUCTOR
    public Game2(User user, User selectedUser) {
        this.user = user;
        this.selectedUser = selectedUser;

        // Determine player roles based on user IDs
        isPlayerX = user.getLocalID().charAt(0) < selectedUser.getLocalID().charAt(0);
        playerX = true; // X starts the game

        setPreferredSize(new Dimension(420, 300));
        addMouseListener(new XOListener());

        jButton = new JButton("New Game");
        jButton.addActionListener(this); 
        jButton.setBounds(315, 210, 100, 30); 
        add(jButton);

        startOrResumeGame();
        startMoveUpdateLoop();
    }

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

    public void resetGame() {
        playerX = true;
        winner = -1;
        gameDone = false;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = 0; // all spots are empty
            }
        }
        jButton.setVisible(false); // Hide the button
        repaint();

        // Reset Firebase to initial game state
        JsonObject gameState = compileGameState();
        Firebase.sendUpdatedGameState(gameId, gameState, user.getTokenID());
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
        // SET COLOR AND FONT
        page.setColor(pink);
        page.fillRect(300, 0, 120, 300);
        Font font = new Font("Helvetica", Font.PLAIN, 20);
        page.setFont(font);

        // SET WIN COUNTER
        page.setColor(offwhite);
        page.drawString("Win Count", 310, 30);
        page.drawString(": " + player1wins, 362, 70);
        page.drawString(": " + player2wins, 362, 105);

        // DRAW score X
        ImageIcon xIcon = new ImageIcon("src\\main\\resources\\com\\guiyomi\\Images\\TicTacToe\\orangex.png");
        Image xImg = xIcon.getImage();
        Image newXImg = xImg.getScaledInstance(27, 27, java.awt.Image.SCALE_SMOOTH);
        ImageIcon newXIcon = new ImageIcon(newXImg);
        page.drawImage(newXIcon.getImage(), 44 + offset * 1 + 190, 47 + offset * 0, null);

        // DRAW score O
        page.setColor(offwhite);
        page.fillOval(43 + 190 + offset, 80, 30, 30);
        page.setColor(darkgray);
        page.fillOval(49 + 190 + offset, 85, 19, 19);

        // Show the player symbol on the UI
        page.setColor(offwhite);
        String playerSymbol = isPlayerX ? "You are X" : "You are O";
        page.drawString(playerSymbol, 310, 45);

        // Show current turn or winner
        // Font turnFont = new Font("Serif", Font.ITALIC, 18);
        // page.setFont(turnFont);
        // if (gameDone) {
        //     page.drawString(winner == 1 ? "X Wins!" : (winner == 2 ? "O Wins!" : "It's a Tie!"), 325, 180);
        // } else {
        //     page.drawString(playerX ? "X's Turn" : "O's Turn", 325, 180);
        // }
        
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
                    ImageIcon xIcon = new ImageIcon("src\\main\\resources\\com\\guiyomi\\Images\\TicTacToe\\orangex.png");
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

 // Updates game state on Firebase and checks winner
 private void checkWinner() {
    if (gameDone) return;

    int temp = -1;
    if (board[0][0] == board[0][1] && board[0][1] == board[0][2] && board[0][0] != 0) temp = board[0][0];
    else if (board[1][0] == board[1][1] && board[1][1] == board[1][2] && board[1][0] != 0) temp = board[1][1];
    else if (board[2][0] == board[2][1] && board[2][1] == board[2][2] && board[2][0] != 0) temp = board[2][1];
    else if (board[0][0] == board[1][0] && board[1][0] == board[2][0] && board[0][0] != 0) temp = board[0][0];
    else if (board[0][1] == board[1][1] && board[1][1] == board[2][1] && board[0][1] != 0) temp = board[0][1];
    else if (board[0][2] == board[1][2] && board[1][2] == board[2][2] && board[0][2] != 0) temp = board[0][2];
    else if (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0] != 0) temp = board[0][0];
    else if (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2] != 0) temp = board[0][2];
    else {
        boolean isDraw = true;
        for (int[] row : board) for (int cell : row) if (cell == 0) isDraw = false;
        if (isDraw) temp = 3;
    }

    if (temp > 0) {
        winner = temp;
        gameDone = true;
        jButton.setVisible(true); // Show "New Game" button
        if (winner == 1) player1wins++;
        if (winner == 2) player2wins++;

        // Update Firebase for the win counts
        JsonObject gameState = compileGameState();
        Firebase.sendUpdatedGameState(gameId, gameState, user.getTokenID());
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

    public void main(String[] args) {
        JFrame frame = new JFrame("Tic Tac Toe");
        frame.getContentPane();

        Game2 gamePanel = new Game2(user, selectedUser);
        frame.add(gamePanel);

        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
            
            }

            public void windowClosing(WindowEvent e) {
                gamePanel.stopMoveUpdateLoop();
            }
        });
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

    // XOListener enforces turn-taking logic
    private class XOListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            if (gameDone) return;

            int selX = -1, selY = -1;
            int a = event.getX(), b = event.getY();

            if (a > 12 && a < 99) selX = 0;
            else if (a > 103 && a < 195) selX = 1;
            else if (a > 200 && a < 287) selX = 2;

            if (b > 12 && b < 99) selY = 0;
            else if (b > 103 && b < 195) selY = 1;
            else if (b > 200 && b < 287) selY = 2;

            if (selX != -1 && selY != -1 && board[selX][selY] == 0) {
                if ((isPlayerX && playerX) || (!isPlayerX && !playerX)) {
                    board[selX][selY] = playerX ? 1 : 2;
                    playerX = !playerX;
                    JsonObject gameState = compileGameState();
                    Firebase.sendUpdatedGameState(gameId, gameState, user.getTokenID());
                    checkWinner();
                    repaint();
                }
            }
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        resetGame();
    }

    // Compile game state to send to Firebase
    private JsonObject compileGameState() {
        JsonObject gameState = new JsonObject();
        JsonObject boardData = new JsonObject();

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

    private void startOrResumeGame() {
        gameId = generateUniqueGameId(); 
        
        if (Firebase.doesGameExist(gameId, user.getTokenID())) {
            JsonObject gameState = Firebase.loadGameStateFromFirebase(gameId, user.getTokenID());
            updateLocalGameState(gameState); 
        } else {
            Firebase.initializeNewGameInFirebase(gameId, user.getTokenID()); 
        }
    }

    private void updateLocalGameState(JsonObject gameState) {
        if (gameState.has("board")) {
            JsonObject boardData = gameState.getAsJsonObject("board");
            for (String key : boardData.keySet()) {
                String[] indices = key.split("_");
                int row = Integer.parseInt(indices[0]);
                int col = Integer.parseInt(indices[1]);
                board[row][col] = boardData.get(key).getAsInt();
            }
        }

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

        repaint(); 
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

}