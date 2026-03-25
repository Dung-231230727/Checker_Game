package com.checkers.controller;

import java.util.ArrayList;
import java.util.List;
import com.checkers.model.Board;
import com.checkers.model.Move;
import com.checkers.model.Piece;
import com.checkers.model.Types;

public class MoveController {
    
    /**
     * Lấy tất cả các nước đi hợp lệ cho một phe.
     * Ưu tiên nước nhảy (Jump) vì đây là luật bắt buộc trong Checker.
     */
    public static List<Move> getAllLegalMoves(Board board, Types.PlayerColor color) {
        List<Move> allMoves = new ArrayList<>();
        List<Move> jumpMoves = new ArrayList<>();

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece piece = board.getPiece(r, c);
                if (piece != null && piece.getColor() == color) {
                    List<Move> pieceMoves = getMovesForPiece(board, r, c);
                    for (Move m : pieceMoves) {
                        if (m.isJump()) {
                            jumpMoves.add(m);
                        } else {
                            allMoves.add(m);
                        }
                    }
                }
            }
        }
        
        // Nếu có ít nhất một nước nhảy, chỉ trả về danh sách các nước nhảy
        return !jumpMoves.isEmpty() ? jumpMoves : allMoves;
    }

    /**
     * Lấy danh sách nước đi cho một quân cờ cụ thể.
     */
    public static List<Move> getMovesForPiece(Board board, int row, int col) {
        List<Move> moves = new ArrayList<>();
        Piece piece = board.getPiece(row, col);
        if (piece == null) return moves;

        int[] dRow = {-1, -1, 1, 1};
        int[] dCol = {-1, 1, -1, 1};

        for (int i = 0; i < 4; i++) {
            // Kiểm tra hướng đi cho quân thường
            if (!piece.isKing()) {
                // WHITE đi từ row lớn về 0 (giảm row)
                if (piece.getColor() == Types.PlayerColor.WHITE && dRow[i] == 1) continue;
                // BLUE đi từ 0 về row lớn (tăng row)
                if (piece.getColor() == Types.PlayerColor.BLUE && dRow[i] == -1) continue;
            }

            int nextR = row + dRow[i];
            int nextC = col + dCol[i];

            if (board.isValidPos(nextR, nextC)) {
                Piece target = board.getPiece(nextR, nextC);
                
                // Nước đi thường
                if (target == null) {
                    moves.add(new Move(row, col, nextR, nextC));
                } 
                // Nước nhảy ăn quân đối phương
                else if (target.getColor() != piece.getColor()) {
                    int jumpR = nextR + dRow[i];
                    int jumpC = nextC + dCol[i];
                    if (board.isValidPos(jumpR, jumpC) && board.getPiece(jumpR, jumpC) == null) {
                        moves.add(new Move(row, col, jumpR, jumpC));
                    }
                }
            }
        }
        return moves;
    }
    
    /**
     * Dùng để kiểm tra xem quân cờ vừa di chuyển có thể ăn tiếp hay không.
     */
    public static boolean canJumpAgain(Board board, int row, int col) {
        return getMovesForPiece(board, row, col).stream().anyMatch(Move::isJump);
    }
}