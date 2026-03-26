package com.checkers.ai;

import com.checkers.model.*;
import com.checkers.controller.MoveController;
import java.util.List;
import java.util.Random;

public class AlphaBetaPruning {
    private static final int NEGATIVE_INFINITY = -1000000;
    private static final int POSITIVE_INFINITY = 1000000;
    private final Random random = new Random();

    /**
     * Hàm lấy nước đi tốt nhất với cấu hình động.
     * @param config Đối tượng chứa Mode và Depth riêng biệt cho từng máy/gợi ý.
     */
    public Move getBestMove(Board board, Types.PlayerColor aiColor, AIConfig config) {
        List<Move> successors = MoveController.getAllLegalMoves(board, aiColor);
        if (successors.isEmpty()) return null;

        Move bestAction = null;
        int v = NEGATIVE_INFINITY;
        int alpha = NEGATIVE_INFINITY;
        int beta = POSITIVE_INFINITY;

        // Sử dụng độ sâu từ đối tượng config truyền vào
        int depth = config.getSearchDepth();

        for (Move action : successors) {
            Board nextState = board.copy();
            nextState.applyMove(action); 
            
            int val;
            // Xử lý nhảy liên hoàn: giữ nguyên độ sâu vì vẫn trong cùng 1 lượt
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                val = maxValue(nextState, alpha, beta, depth, aiColor, config);
            } else {
                val = minValue(nextState, alpha, beta, depth - 1, aiColor, config);
            }
            
            if (val > v) {
                v = val;
                bestAction = action;
            } else if (val == v && random.nextBoolean()) {
                bestAction = action;
            }
            alpha = Math.max(alpha, v);
        }
        return bestAction;
    }

    private int maxValue(Board state, int alpha, int beta, int depth, Types.PlayerColor aiColor, AIConfig config) {
        if (terminalTest(state, depth, aiColor)) return utility(state, aiColor, config);

        int v = NEGATIVE_INFINITY;
        for (Move action : MoveController.getAllLegalMoves(state, aiColor)) {
            Board nextState = state.copy();
            nextState.applyMove(action);
            
            int res;
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                res = maxValue(nextState, alpha, beta, depth, aiColor, config);
            } else {
                res = minValue(nextState, alpha, beta, depth - 1, aiColor, config);
            }
            v = Math.max(v, res);
            if (v >= beta) return v;
            alpha = Math.max(alpha, v);
        }
        return v;
    }

    private int minValue(Board state, int alpha, int beta, int depth, Types.PlayerColor aiColor, AIConfig config) {
        Types.PlayerColor opp = (aiColor == Types.PlayerColor.WHITE) ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;
        if (terminalTest(state, depth, opp)) return utility(state, aiColor, config);

        int v = POSITIVE_INFINITY;
        for (Move action : MoveController.getAllLegalMoves(state, opp)) {
            Board nextState = state.copy();
            nextState.applyMove(action);
            
            int res;
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                res = minValue(nextState, alpha, beta, depth, aiColor, config);
            } else {
                res = maxValue(nextState, alpha, beta, depth - 1, aiColor, config);
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

    private int utility(Board state, Types.PlayerColor aiColor, AIConfig config) {
        Types.PlayerColor opp = (aiColor == Types.PlayerColor.WHITE) ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;
        
        if (MoveController.getAllLegalMoves(state, opp).isEmpty()) return 800000;
        if (MoveController.getAllLegalMoves(state, aiColor).isEmpty()) return -800000;

        double score = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null) continue;

                // Truyền config vào hàm đánh giá quân cờ đơn lẻ
                double pieceScore = evaluateSinglePiece(p, r, c, state, config);

                if (p.getColor() == aiColor) {
                    score += pieceScore;
                } else {
                    score -= pieceScore;
                }
            }
        }
        return (int) score;
    }

    private double evaluateSinglePiece(Piece p, int r, int c, Board state, AIConfig config) {
        double score = 0;

        // 1. VẬT CHẤT (Material)
        double material = p.isKing() ? 190 : 100;
        score += material * config.materialWeight;

        // 2. VỊ TRÍ (Position)
        double positionScore = 0;
        if (!p.isKing()) {
            int progress = (p.getColor() == Types.PlayerColor.WHITE) ? (7 - r) : r;
            positionScore += progress * 10;
            if (c == 0 || c == 7) positionScore += 15; 
        } else {
            positionScore += getKingCentrality(r, c);
        }
        score += positionScore * config.positionWeight;

        // 3. CẤU TRÚC (Structure)
        double structureScore = 0;
        if (isPartOfBridge(r, c, p.getColor())) structureScore += 40;
        if (isProtected(r, c, p.getColor(), state)) structureScore += 15;
        score += structureScore * config.structureWeight;

        // 4. CƠ ĐỘNG (Mobility)
        int moveCount = MoveController.getMovesForPiece(state, r, c).size();
        score += (moveCount * 5) * config.mobilityWeight;

        return score;
    }

    // --- CÁC HÀM TRỢ GIÚP (Giữ nguyên logic tọa độ) ---

    private double getKingCentrality(int r, int c) {
        if ((r == 3 || r == 4) && (c == 3 || c == 4)) return 30.0;
        if (r >= 2 && r <= 5 && c >= 2 && c <= 5) return 15.0;
        return 0.0;
    }

    private boolean isPartOfBridge(int r, int c, Types.PlayerColor color) {
        if (color == Types.PlayerColor.WHITE) return r == 7 && (c == 1 || c == 3);
        else return r == 0 && (c == 4 || c == 6);
    }

    private boolean isProtected(int r, int c, Types.PlayerColor color, Board state) {
        int backRow = (color == Types.PlayerColor.WHITE) ? r + 1 : r - 1;
        if (backRow < 0 || backRow > 7) return true;
        boolean protectedLeft = (c - 1 >= 0) && hasAllyAt(backRow, c - 1, color, state);
        boolean protectedRight = (c + 1 <= 7) && hasAllyAt(backRow, c + 1, color, state);
        return protectedLeft || protectedRight;
    }

    private boolean hasAllyAt(int r, int c, Types.PlayerColor color, Board state) {
        Piece p = state.getPiece(r, c);
        return p != null && p.getColor() == color;
    }
}