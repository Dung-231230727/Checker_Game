package com.checkers.ai;

import com.checkers.model.*;
import com.checkers.controller.MoveController; // Đổi từ MoveLogic sang MoveController 

import java.util.List;
import java.util.Random;

public class AlphaBetaPruning {
    private static final int MAX_DEPTH = 5; 
    private static final int NEGATIVE_INFINITY = -1000000;
    private static final int POSITIVE_INFINITY = 1000000;
    private final Random random = new Random();

    public Move getBestMove(Board board, Types.PlayerColor aiColor) {
        return getBestMove(board, aiColor, false, MAX_DEPTH);
    }

    public Move getBestMove(Board board, Types.PlayerColor aiColor, boolean deterministic, int depth) {
        // Sử dụng MoveController để lấy danh sách nước đi hợp lệ 
        List<Move> successors = MoveController.getAllLegalMoves(board, aiColor);
        if (successors.isEmpty()) return null;

        Move bestAction = null;
        int v = NEGATIVE_INFINITY;
        int alpha = NEGATIVE_INFINITY;
        int beta = POSITIVE_INFINITY;

        for (Move action : successors) {
            Board nextState = board.copy();
            nextState.applyMove(action); // Piece đã được promote trong applyMove nếu thỏa điều kiện 
            
            int val;
            // Kiểm tra luật nhảy liên hoàn (Multi-jump) 
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                val = maxValue(nextState, alpha, beta, depth, aiColor);
            } else {
                val = minValue(nextState, alpha, beta, depth - 1, aiColor);
            }
            
            if (val > v) {
                v = val;
                bestAction = action;
            } else if (val == v && !deterministic && random.nextBoolean()) {
                bestAction = action;
            }
            alpha = Math.max(alpha, v);
        }
        return bestAction;
    }

    private int maxValue(Board state, int alpha, int beta, int depth, Types.PlayerColor aiColor) {
        if (terminalTest(state, depth, aiColor)) return utility(state, aiColor);

        int v = NEGATIVE_INFINITY;
        for (Move action : MoveController.getAllLegalMoves(state, aiColor)) {
            Board nextState = state.copy();
            nextState.applyMove(action);
            
            int res;
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                res = maxValue(nextState, alpha, beta, depth, aiColor);
            } else {
                res = minValue(nextState, alpha, beta, depth - 1, aiColor);
            }
            v = Math.max(v, res);
            if (v >= beta) return v;
            alpha = Math.max(alpha, v);
        }
        return v;
    }

    private int minValue(Board state, int alpha, int beta, int depth, Types.PlayerColor aiColor) {
        Types.PlayerColor opp = (aiColor == Types.PlayerColor.WHITE) ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;
        if (terminalTest(state, depth, opp)) return utility(state, aiColor);

        int v = POSITIVE_INFINITY;
        for (Move action : MoveController.getAllLegalMoves(state, opp)) {
            Board nextState = state.copy();
            nextState.applyMove(action);
            
            int res;
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                res = minValue(nextState, alpha, beta, depth, aiColor);
            } else {
                res = maxValue(nextState, alpha, beta, depth - 1, aiColor);
            }
            v = Math.min(v, res);
            if (v <= alpha) return v;
            beta = Math.min(beta, v);
        }
        return v;
    }

    private boolean terminalTest(Board state, int depth, Types.PlayerColor currentColor) {
        return depth <= 0 || MoveController.getAllLegalMoves(state, currentColor).isEmpty();
    }

    private int utility(Board state, Types.PlayerColor aiColor) {
        // Đồng bộ màu đối thủ là BLUE 
        Types.PlayerColor opp = (aiColor == Types.PlayerColor.WHITE) ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;
        
        if (MoveController.getAllLegalMoves(state, opp).isEmpty()) return 800000;
        if (MoveController.getAllLegalMoves(state, aiColor).isEmpty()) return -800000;

        int score = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null) continue;

                int pieceValue = p.isKing() ? 50 : 20;

                // Thưởng vị trí tiến gần hàng cuối 
                if (p.getColor() == Types.PlayerColor.WHITE) {
                    pieceValue += (7 - r); 
                    if (r == 7) pieceValue += 10; // Phòng thủ hàng đáy
                } else {
                    pieceValue += r;
                    if (r == 0) pieceValue += 10; // Phòng thủ hàng đáy cho BLUE 
                }

                // Thưởng an toàn biên
                if (c == 0 || c == 7) pieceValue += 5;

                if (p.getColor() == aiColor) score += pieceValue;
                else score -= pieceValue;
            }
        }
        return score;
    }

    public Move getBestMoveForPiece(Board board, int row, int col, Types.PlayerColor color) {
        List<Move> successors = MoveController.getMovesForPiece(board, row, col);
        if (successors.isEmpty()) return null;

        for (Move m : successors) {
            if (m.isJump()) return m;
        }
        return successors.get(0);
    }
}