package com.checkers.model;

public class GameState {
    private Board board;
    private Player player1;
    private Player player2;
    private Types.PlayerColor currentPlayerColor;
    private boolean isGameOver;

    public GameState(Player player1, Player player2) {
        this.board = new Board();
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayerColor = Types.PlayerColor.WHITE;
        this.isGameOver = false;
    }

    public Board getBoard() { return board; }
    
    public boolean isGameOver() { return isGameOver; }
    
    public void setGameOver(boolean gameOver) { this.isGameOver = gameOver; }

    public Player getCurrentPlayer() {
        return (currentPlayerColor == player1.getColor()) ? player1 : player2;
    }

    // Dùng để GameController kiểm tra xem bên nào là AI
    public Player getPlayerByColor(Types.PlayerColor color) {
        if (player1.getColor() == color) return player1;
        if (player2.getColor() == color) return player2;
        return null;
    }

    public Types.PlayerColor getCurrentPlayerColor() { return currentPlayerColor; }

    public void updateTime() {
        if (!isGameOver) getCurrentPlayer().incrementTime();
    }

    public void switchTurn() {
        currentPlayerColor = (currentPlayerColor == Types.PlayerColor.WHITE) 
                           ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setCurrentPlayerColor(Types.PlayerColor color) {
        this.currentPlayerColor = color; 
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
}