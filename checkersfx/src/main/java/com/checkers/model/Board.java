package com.checkers.model;

/**
 * Lớp quản lý dữ liệu bàn cờ (Model).
 * Đã được điều chỉnh sang màu XANH (BLUE) và TRẮNG (WHITE).
 */
public class Board {
    public static final int SIZE = 8;
    private Piece[][] grid;

    public Board() {
        grid = new Piece[SIZE][SIZE];
        initializeBoard();
    }

    /**
     * Khởi tạo quân cờ ở các vị trí mặc định ban đầu.
     */
    private void initializeBoard() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = null; // Đảm bảo ô trống ban đầu
                
                // Quân cờ chỉ nằm ở các ô tối (dark squares)
                if ((r + c) % 2 != 0) {
                    if (r < 3) {
                        // Đổi BLACK thành BLUE cho khớp với giao diện Xanh - Trắng
                        grid[r][c] = new Piece(Types.PlayerColor.BLUE);
                    } else if (r > 4) {
                        grid[r][c] = new Piece(Types.PlayerColor.WHITE);
                    }
                }
            }
        }
    }

    public Piece getPiece(int row, int col) {
        if (isValidPos(row, col)) return grid[row][col];
        return null;
    }

    public void setPiece(int row, int col, Piece piece) {
        if (isValidPos(row, col)) grid[row][col] = piece;
    }

    public boolean isValidPos(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    /**
     * Thực hiện di chuyển quân cờ và cập nhật logic ăn quân, phong vua.
     */
    public void applyMove(Move move) {
        Piece p = grid[move.getStartRow()][move.getStartCol()];
        if (p == null) return;

        // Di chuyển quân cờ đến vị trí mới
        grid[move.getEndRow()][move.getEndCol()] = p;
        grid[move.getStartRow()][move.getStartCol()] = null;

        // Nếu là nước ăn quân (Jump), xóa quân bị nhảy qua ở giữa
        if (move.isJump()) {
            int midRow = (move.getStartRow() + move.getEndRow()) / 2;
            int midCol = (move.getStartCol() + move.getEndCol()) / 2;
            grid[midRow][midCol] = null;
        }

        // Logic Phong Vua (Promote to King)
        if (!p.isKing()) {
            // Quân Trắng đi lên (về hàng 0) thì thành Vua
            if (p.getColor() == Types.PlayerColor.WHITE && move.getEndRow() == 0) {
                p.promote();
            } 
            // Quân Xanh đi xuống (về hàng SIZE-1) thì thành Vua
            else if (p.getColor() == Types.PlayerColor.BLUE && move.getEndRow() == SIZE - 1) {
                p.promote();
            }
        }
    }

    /**
     * Tạo một bản sao của bàn cờ (dùng cho AI tính toán hoặc Undo).
     */
    public Board copy() {
        Board newBoard = new Board();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Piece p = this.grid[r][c];
                newBoard.grid[r][c] = (p != null) ? p.copy() : null;
            }
        }
        return newBoard;
    }

    /**
     * Xóa toàn bộ quân cờ (dùng khi bắt đầu ván mới).
     */
    public void clear() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = null;
            }
        }
    }
}