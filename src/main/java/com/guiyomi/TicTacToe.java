package com.guiyomi;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import com.google.gson.JsonObject;

public class TicTacToe extends JPanel implements ActionListener {

    // Core logic variables
    boolean playerX; // true if player X's turn, false if player O's turn
    boolean gameDone = false; // true if game is over
    int winner = -1; // 0 if X wins, 1 if O wins, -1 if no winner yet
    int player1wins = 0, player2wins = 0; // number of wins for each player
    int[][] board = new int[3][3]; // 0 if empty, 1 if X, 2 if O

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
    JButton jButton;

    // Constructor
    public TicTacToe() {
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
    }

    public void resetGame() {
        playerX = true;
        winner = -1;
        gameDone = false;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = 0;
            }
        }
        getJButton().setVisible(false);
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
                if (board[i][j] == 1) {
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
    }

    public void checkWinner() {
        if (gameDone) return;
        int temp = -1;

        if ((board[0][0] == board[0][1] && board[0][1] == board[0][2] && board[0][0] != 0) ||
            (board[1][0] == board[1][1] && board[1][1] == board[1][2] && board[1][0] != 0) ||
            (board[2][0] == board[2][1] && board[2][1] == board[2][2] && board[2][0] != 0) ||
            (board[0][0] == board[1][0] && board[1][0] == board[2][0] && board[0][0] != 0) ||
            (board[0][1] == board[1][1] && board[1][1] == board[2][1] && board[0][1] != 0) ||
            (board[0][2] == board[1][2] && board[1][2] == board[2][2] && board[0][2] != 0) ||
            (board[0][0] == board[1][1] && board[1][1] == board[2][2] && board[0][0] != 0) ||
            (board[0][2] == board[1][1] && board[1][1] == board[2][0] && board[0][2] != 0)) {
            temp = playerX ? 1 : 2;
        } else {
            boolean notDone = false;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == 0) {
                        notDone = true;
                        break;
                    }
                }
            }
            if (!notDone) {
                temp = 3;
            }
        }

        if (temp > 0) {
            winner = temp;
            if (winner == 1) player1wins++;
            else if (winner == 2) player2wins++;
            gameDone = true;
            getJButton().setVisible(true);
            saveScoreToFirebase(player1wins, player2wins);
        }
    }

    public JButton getJButton() {
        return jButton;
    }

    public void actionPerformed(ActionEvent e) {
        resetGame();    
    }

        // Firebase methods
        private void saveScoreToFirebase(int xWins, int oWins) {
            try {
                URL url = URI.create(DATABASE_URL + "/tictactoe/score.json").toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Using Gson's JsonObject
                JsonObject json = new JsonObject();
                json.addProperty("xWins", xWins);
                json.addProperty("oWins", oWins);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.toString().getBytes());
                    os.flush();
                }
                System.out.println("Scores saved to Firebase");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    public static void main(String[] args) {
        JFrame frame = new JFrame("Tic Tac Toe");
        frame.add(new TicTacToe());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

    // Mouse Listener class for XO game
    private class XOListener extends MouseAdapter {
        public void mousePressed(MouseEvent event) {
            if (gameDone) return;
            int row = event.getX() / offset;
            int col = (event.getY() - 50) / offset;
            if (row < 3 && col < 3 && board[row][col] == 0) {
                board[row][col] = playerX ? 1 : 2;
                playerX = !playerX;
                repaint();
                checkWinner();
            }
        }
    }
}
