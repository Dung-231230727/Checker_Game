package com.checkers.controller;

import com.checkers.ai.AlphaBetaPruning;
import com.checkers.ai.AIConfig;
import com.checkers.model.*;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class AIController {
    private final GameController gameCtrl;
    private final AlphaBetaPruning ai = new AlphaBetaPruning();
    private boolean isThinking = false;
    private boolean isHinting = false;

    public AIController(GameController gameCtrl) {
        this.gameCtrl = gameCtrl;
    }

    /**
     * Xử lý lượt đi của AI với cấu hình động lấy từ GameController.
     */
    public void processAITurn(GameState state) {
        if (state == null || state.isGameOver() || 
            state.getCurrentPlayer().getType() != Types.PlayerType.AI || isThinking) {
            return;
        }

        isThinking = true;
        
        final Board snapshot = state.getBoard().copy();
        final Types.PlayerColor aiColor = state.getCurrentPlayer().getColor();

        // --- LẤY CẤU HÌNH ĐỘNG TỪ VIEW THÔNG QUA GAMECONTROLLER ---
        // Phân biệt máy 1 (Thường là Trắng) và máy 2 (Thường là Xanh) để lấy đúng Mode/Depth
        final AIConfig currentConfig = (aiColor == Types.PlayerColor.WHITE) ? 
                                        gameCtrl.getAi1Config() : gameCtrl.getAi2Config();

        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> {
            Task<Move> aiTask = new Task<>() {
                @Override
                protected Move call() {
                    // Truyền thêm currentConfig vào hàm getBestMove
                    return ai.getBestMove(snapshot, aiColor, currentConfig);
                }
            };

            aiTask.setOnSucceeded(event -> {
                isThinking = false;
                Move bestMove = aiTask.getValue();
                
                if (bestMove != null) {
                    Platform.runLater(() -> gameCtrl.applyMove(bestMove));
                } else {
                    Platform.runLater(() -> gameCtrl.handleGameOver());
                }
            });

            aiTask.setOnFailed(event -> {
                isThinking = false;
                aiTask.getException().printStackTrace();
            });

            new Thread(aiTask).start();
        });
        pause.play();
    }

    /**
     * Chức năng gợi ý sử dụng cấu hình riêng cho Hint.
     */
    public void showHint(GameState state) {
        if (state == null || state.isGameOver() || isThinking || isHinting) return;

        if (state.getCurrentPlayer().getType() != Types.PlayerType.HUMAN) return;

        isHinting = true;

        final Board snapshot = state.getBoard().copy();
        final Types.PlayerColor humanColor = state.getCurrentPlayer().getColor();
        
        // --- LẤY CẤU HÌNH RIÊNG CHO GỢI Ý ---
        final AIConfig hConfig = gameCtrl.getHintConfig();

        Task<Move> hintTask = new Task<>() {
            @Override
            protected Move call() {
                // Truyền cấu hình gợi ý (thường có độ sâu cao hơn) vào AI
                return ai.getBestMove(snapshot, humanColor, hConfig);
            }
        };

        hintTask.setOnSucceeded(e -> {
            Move hint = hintTask.getValue();
            if (hint != null) {
                gameCtrl.updateViewSelection(hint.getStartRow(), hint.getStartCol(), java.util.List.of(hint));
            }
        });

        hintTask.setOnFailed(e -> {
            isHinting = false; // Mở khóa nếu bị lỗi
            hintTask.getException().printStackTrace();
        });

        new Thread(hintTask).start();
    }

    public void stop() {
        isThinking = false;
    }
}