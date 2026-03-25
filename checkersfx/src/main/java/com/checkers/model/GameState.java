package com.checkers.model;

import java.util.Stack;

public class GameState {
    private Board board;
    private Player player1;
    private Player player2;
    private Types.PlayerColor currentPlayerColor;
    private boolean isGameOver;
    
    // Lịch sử (có thể dùng nội bộ hoặc kết hợp với GameController)
    private Stack<Board> boardHistory = new Stack<>();
    private Stack<Types.PlayerColor> playerHistory = new Stack<>();

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

    public Types.PlayerColor getCurrentPlayerColor() { return currentPlayerColor; }

    public void updateTime() {
        if (!isGameOver) getCurrentPlayer().incrementTime();
    }

    public void switchTurn() {
        currentPlayerColor = (currentPlayerColor == Types.PlayerColor.WHITE) 
                           ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;
    }

    public void saveState() {
        boardHistory.push(board.copy());
        playerHistory.push(currentPlayerColor);
    }

    public boolean undo() {
        if (!boardHistory.isEmpty()) {
            this.board = boardHistory.pop();
            this.currentPlayerColor = playerHistory.pop();
            return true;
        }
        return false;
    }

    // --- CÁC PHƯƠNG THỨC HỖ TRỢ CHỨC NĂNG ĐI LẠI (UNDO) ---
    
    public void setBoard(Board board) {
        this.board = board;
    }

    public void setCurrentPlayerColor(Types.PlayerColor color) {
        // SỬA LỖI: Gán trực tiếp vào biến currentPlayerColor
        this.currentPlayerColor = color; 
    }

    // THÊM: Các hàm Getter để GameController kiểm tra chế độ chơi (PvP hay PvE)
    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }
}