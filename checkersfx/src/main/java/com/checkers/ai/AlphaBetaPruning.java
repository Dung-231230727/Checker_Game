package com.checkers.ai;

import com.checkers.model.*;
import com.checkers.controller.MoveController;
import java.util.List;
import java.util.Random;

public class AlphaBetaPruning {
    private static final int NEGATIVE_INFINITY = -1000000;
    private static final int POSITIVE_INFINITY = 1000000;
    private final Random random = new Random();

    // --- Zobrist Hashing & Transposition Table ---
    private static final long[][][] zobristTable = new long[8][8][4]; // [Row][Col][PieceType]
    private static final long turnHash;
    private static final int TT_SIZE = 1 << 20; // Khoảng 1 triệu Entry
    private final TTEntry[] transpositionTable = new TTEntry[TT_SIZE];

    static {
        Random rnd = new Random(12345); // Seed cố định để nhất quán
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                for (int p = 0; p < 4; p++) {
                    zobristTable[r][c][p] = rnd.nextLong();
                }
            }
        }
        turnHash = rnd.nextLong();
    }

    private int getPieceIndex(Piece p) {
        if (p == null) return -1;
        if (p.getColor() == Types.PlayerColor.WHITE) {
            return p.isKing() ? 1 : 0;
        } else {
            return p.isKing() ? 3 : 2;
        }
    }
    // ----------------------------------------------

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

        int depth = config.getSearchDepth();
        long initialHash = calculateInitialHash(board, aiColor);

        for (Move action : successors) {
            Board nextState = board.copy();
            nextState.applyMove(action);
            
            // Cập nhật mã Hash cho nước đi tiếp theo (lượt của đối phương)
            long nextHash = updateHash(initialHash, action, board);
            
            int val;
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                val = maxValue(nextState, alpha, beta, depth, aiColor, config, nextHash);
            } else {
                val = minValue(nextState, alpha, beta, depth - 1, aiColor, config, nextHash);
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

    private int maxValue(Board state, int alpha, int beta, int depth, Types.PlayerColor aiColor, AIConfig config, long hash) {
        // 1. Kiểm tra Transposition Table (Lookup)
        int ttIndex = (int) (Math.abs(hash) % TT_SIZE);
        TTEntry entry = transpositionTable[ttIndex];
        if (entry != null && entry.key == hash && entry.depth >= depth) {
            if (entry.type == TTEntry.EXACT) return entry.score;
            if (entry.type == TTEntry.LOWER_BOUND) alpha = Math.max(alpha, entry.score);
            if (entry.type == TTEntry.UPPER_BOUND) beta = Math.min(beta, entry.score);
            if (alpha >= beta) return entry.score;
        }

        List<Move> moves = MoveController.getAllLegalMoves(state, aiColor);
        if (moves.isEmpty()) return -800000;
        if (depth <= 0) return utility(state, aiColor, config);

        int originalAlpha = alpha;
        int v = NEGATIVE_INFINITY;
        for (Move action : moves) {
            Board nextState = state.copy();
            nextState.applyMove(action);
            long nextHash = updateHash(hash, action, state);
            
            int res;
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                res = maxValue(nextState, alpha, beta, depth, aiColor, config, nextHash);
            } else {
                res = minValue(nextState, alpha, beta, depth - 1, aiColor, config, nextHash);
            }
            v = Math.max(v, res);
            alpha = Math.max(alpha, v);
            if (v >= beta) break; // Beta cutoff
        }

        // 2. Lưu kết quả vào Transposition Table (Store - Replacement Strategy: Depth-Preferred)
        int type = (v <= originalAlpha) ? TTEntry.UPPER_BOUND : (v >= beta) ? TTEntry.LOWER_BOUND : TTEntry.EXACT;
        if (entry == null || depth > entry.depth) {
            transpositionTable[ttIndex] = new TTEntry(hash, v, depth, type);
        }

        return v;
    }

    private int minValue(Board state, int alpha, int beta, int depth, Types.PlayerColor aiColor, AIConfig config, long hash) {
        Types.PlayerColor opp = (aiColor == Types.PlayerColor.WHITE) ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;

        // 1. Kiểm tra Transposition Table (Lookup)
        int ttIndex = (int) (Math.abs(hash) % TT_SIZE);
        TTEntry entry = transpositionTable[ttIndex];
        if (entry != null && entry.key == hash && entry.depth >= depth) {
            if (entry.type == TTEntry.EXACT) return entry.score;
            if (entry.type == TTEntry.LOWER_BOUND) alpha = Math.max(alpha, entry.score);
            if (entry.type == TTEntry.UPPER_BOUND) beta = Math.min(beta, entry.score);
            if (alpha >= beta) return entry.score;
        }

        List<Move> moves = MoveController.getAllLegalMoves(state, opp);
        if (moves.isEmpty()) return 800000;
        if (depth <= 0) return utility(state, aiColor, config);

        int originalAlpha = alpha;
        int v = POSITIVE_INFINITY;
        for (Move action : moves) {
            Board nextState = state.copy();
            nextState.applyMove(action);
            long nextHash = updateHash(hash, action, state);
            
            int res;
            if (action.isJump() && MoveController.canJumpAgain(nextState, action.getEndRow(), action.getEndCol())) {
                res = minValue(nextState, alpha, beta, depth, aiColor, config, nextHash);
            } else {
                res = maxValue(nextState, alpha, beta, depth - 1, aiColor, config, nextHash);
            }
            v = Math.min(v, res);
            beta = Math.min(beta, v);
            if (v <= alpha) break; // Alpha cutoff
        }

        // 2. Lưu kết quả vào Transposition Table (Store)
        int type = (v <= originalAlpha) ? TTEntry.UPPER_BOUND : (v >= beta) ? TTEntry.LOWER_BOUND : TTEntry.EXACT;
        if (entry == null || depth > entry.depth) {
            transpositionTable[ttIndex] = new TTEntry(hash, v, depth, type);
        }

        return v;
    }

    private int utility(Board state, Types.PlayerColor aiColor, AIConfig config) {
        Types.PlayerColor opp = (aiColor == Types.PlayerColor.WHITE) ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;
        double score = 0;

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null) continue;

                double pieceScore = evaluateSinglePiece(p, r, c, state, config);
                if (p.getColor() == aiColor) score += pieceScore;
                else score -= pieceScore;
            }
        }
        
        // Bonus Mobility: Thưởng cho việc có nhiều nước đi an toàn hơn
        int aiMoves = MoveController.getAllLegalMoves(state, aiColor).size();
        int oppMoves = MoveController.getAllLegalMoves(state, opp).size();
        score += (aiMoves - oppMoves) * 5 * config.mobilityWeight;

        return (int) score;
    }

    private double evaluateSinglePiece(Piece p, int r, int c, Board state, AIConfig config) {
        double score = 0;

        // 1. VẬT CHẤT (Material)
        // Trong Checker, quân Vua cực kỳ mạnh, đáng giá gấp đôi quân thường
        double material = p.isKing() ? 200 : 100;
        score += material * config.materialWeight;

        // 2. VỊ TRÍ & CHIẾN THUẬT (Positioning)
        double positionScore = 0;
        if (!p.isKing()) {
            // Thưởng cho quân tiến gần hàng phong Vua
            int progress = (p.getColor() == Types.PlayerColor.WHITE) ? (7 - r) : r;
            positionScore += progress * 15; // Tăng trọng số tiến quân

            // Bonus đặc biệt nếu quân ở hàng cuối (giữ vị trí để chặn đối phương thành Vua)
            if (isBackRowProtector(r, c, p.getColor())) positionScore += 40;
            
            // Nếu quân cách đích 1 ô (sắp thành Vua)
            if (progress == 6) positionScore += 50;
        } else {
            // Vua mạnh nhất khi ở trung tâm (kiểm soát nhiều đường chéo hơn)
            positionScore += getKingCentrality(r, c) * 1.5;
        }
        
        // Ưu tiên quân ở biên (không bị nhảy từ bên ngoài) nhưng giảm cơ động
        if (c == 0 || c == Board.SIZE - 1) positionScore += 10;

        score += positionScore * config.positionWeight;

        // 3. CƠ CẤU & BẢO VỆ (Structure)
        double structureScore = 0;
        // Quân đứng sau bảo vệ quân trước (diagonal protection)
        if (isProtected(r, c, p.getColor(), state)) structureScore += 20;
        
        // Phạt nặng nếu quân đang ở vị trí bị đối phương có thể ăn (hàn đơn giản)
        if (isThreatened(r, c, p.getColor(), state)) structureScore -= 80;

        score += structureScore * config.structureWeight;

        return score;
    }

    // --- CÁC HÀM TRỢ GIÚP (Giữ nguyên logic tọa độ) ---

    private double getKingCentrality(int r, int c) {
        if ((r == 3 || r == 4) && (c == 3 || c == 4)) return 30.0;
        if (r >= 2 && r <= 5 && c >= 2 && c <= 5) return 15.0;
        return 0.0;
    }

    private boolean isBackRowProtector(int r, int c, Types.PlayerColor color) {
        // BLUE bắt đầu từ row 0, WHITE bắt đầu từ row 7
        if (color == Types.PlayerColor.WHITE) return r == 7;
        else return r == 0;
    }

    private boolean isThreatened(int r, int c, Types.PlayerColor color, Board state) {
        // Kiểm tra xem quân cờ tại r, c có thể bị đối phương ăn ở nước đi tiếp theo không
        // Logic đơn giản: Check 4 đường chéo xem có quân địch kề bên và ô nhảy trống không
        int[] dR = {-1, -1, 1, 1};
        int[] dC = {-1, 1, -1, 1};
        Types.PlayerColor oppColor = (color == Types.PlayerColor.WHITE) ? Types.PlayerColor.BLUE : Types.PlayerColor.WHITE;

        for (int i = 0; i < 4; i++) {
            int oppR = r + dR[i];
            int oppC = c + dC[i];
            if (state.isValidPos(oppR, oppC)) {
                Piece opp = state.getPiece(oppR, oppC);
                if (opp != null && opp.getColor() == oppColor) {
                    // Nếu là quân thường của địch, nó chỉ nhảy được theo hướng của nó
                    if (!opp.isKing()) {
                        if (oppColor == Types.PlayerColor.WHITE && dR[i] == 1) continue;
                        if (oppColor == Types.PlayerColor.BLUE && dR[i] == -1) continue;
                    }
                    // Check ô nhảy (phía sau r, c theo hướng dR[i], dC[i])
                    int jumpR = r - dR[i];
                    int jumpC = c - dC[i];
                    if (state.isValidPos(jumpR, jumpC) && state.getPiece(jumpR, jumpC) == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isProtected(int r, int c, Types.PlayerColor color, Board state) {
        int backRow = (color == Types.PlayerColor.WHITE) ? r + 1 : r - 1;
        if (backRow < 0 || backRow > 7) return true;
        boolean protectedLeft = (c - 1 >= 0) && hasAllyAt(backRow, c - 1, color, state);
        boolean protectedRight = (c + 1 <= 7) && hasAllyAt(backRow, c + 1, color, state);
        return protectedLeft || protectedRight;
    }

    // --- CÁC HÀM HASHING (Incremental Update) ---

    private long calculateInitialHash(Board board, Types.PlayerColor turn) {
        long h = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                int pIdx = getPieceIndex(p);
                if (pIdx != -1) h ^= zobristTable[r][c][pIdx];
            }
        }
        if (turn == Types.PlayerColor.BLUE) h ^= turnHash;
        return h;
    }

    private long updateHash(long currentHash, Move move, Board context) {
        long h = currentHash;
        Piece mover = context.getPiece(move.getStartRow(), move.getStartCol());
        int pIdx = getPieceIndex(mover);

        // 1. XOR quân cờ rời khỏi vị trí cũ
        h ^= zobristTable[move.getStartRow()][move.getStartCol()][pIdx];

        // 2. XOR quân cờ vào vị trí mới (Xử lý cả phong Vua)
        boolean isPromoted = !mover.isKing() && 
            ((mover.getColor() == Types.PlayerColor.WHITE && move.getEndRow() == 0) ||
             (mover.getColor() == Types.PlayerColor.BLUE && move.getEndRow() == 7));
        
        int newIdx = isPromoted ? (pIdx + 1) : pIdx;
        h ^= zobristTable[move.getEndRow()][move.getEndCol()][newIdx];

        // 3. Nếu là nước nhảy, XOR quân bị ăn mất
        if (move.isJump()) {
            int midR = (move.getStartRow() + move.getEndRow()) / 2;
            int midC = (move.getStartCol() + move.getEndCol()) / 2;
            Piece captured = context.getPiece(midR, midC);
            int capIdx = getPieceIndex(captured);
            if (capIdx != -1) h ^= zobristTable[midR][midC][capIdx];
        }

        // 4. Flip lượt
        h ^= turnHash;

        return h;
    }

    private boolean hasAllyAt(int r, int c, Types.PlayerColor color, Board state) {
        Piece p = state.getPiece(r, c);
        return p != null && p.getColor() == color;
    }
}