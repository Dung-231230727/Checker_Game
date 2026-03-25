package com.checkers.model;

public class Move {
    private int startRow, startCol;
    private int endRow, endCol;

    public Move(int startRow, int startCol, int endRow, int endCol) {
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
    }

    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }
    public int getEndRow() { return endRow; }
    public int getEndCol() { return endCol; }

    // Logic kiểm tra xem đây có phải là nước nhảy (ăn quân) không
    public boolean isJump() {
        return Math.abs(startRow - endRow) == 2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Move)) return false;
        Move other = (Move) obj;
        return this.startRow == other.startRow && this.startCol == other.startCol &&
               this.endRow == other.endRow && this.endCol == other.endCol;
    }

    public static boolean isPromotionMove(Board board, Move move) {
        Piece piece = board.getPiece(move.getStartRow(), move.getStartCol());
        if (piece == null || piece.isKing()) return false;

        if (piece.getColor() == Types.PlayerColor.WHITE && move.getEndRow() == 0) return true;
        if (piece.getColor() == Types.PlayerColor.BLUE && move.getEndRow() == Board.SIZE - 1) return true;
        
        return false;
    }
}