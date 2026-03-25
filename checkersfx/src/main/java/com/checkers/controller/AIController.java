package com.checkers.controller;

import com.checkers.ai.AlphaBetaPruning;
import com.checkers.model.*;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class AIController {
    private final GameController gameCtrl;
    private final AlphaBetaPruning ai = new AlphaBetaPruning();
    private boolean isThinking = false;

    public AIController(GameController gameCtrl) {
        this.gameCtrl = gameCtrl;
    }

    /**
     * Xử lý lượt đi của AI.
     */
    public void processAITurn(GameState state) {
        // Kiểm tra điều kiện: Game chưa kết thúc, đúng lượt AI và AI không đang suy nghĩ
        if (state == null || state.isGameOver() || 
            state.getCurrentPlayer().getType() != Types.PlayerType.AI || isThinking) {
            return;
        }

        isThinking = true;
        
        // Tạo bản sao dữ liệu để AI tính toán độc lập với luồng UI
        final Board snapshot = state.getBoard().copy();
        final Types.PlayerColor aiColor = state.getCurrentPlayer().getColor();

        // Tạo một khoảng dừng nhỏ (500ms) để người chơi kịp nhìn thấy nước đi trước đó
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> {
            // Chạy thuật toán AI trên một luồng phụ (Thread) để không làm đơ giao diện
            Task<Move> aiTask = new Task<>() {
                @Override
                protected Move call() {
                    return ai.getBestMove(snapshot, aiColor);
                }
            };

            // Khi AI tính toán xong
            aiTask.setOnSucceeded(event -> {
                isThinking = false;
                Move bestMove = aiTask.getValue();
                
                if (bestMove != null) {
                    // Thực hiện nước đi thông qua GameController
                    Platform.runLater(() -> gameCtrl.applyMove(bestMove));
                } else {
                    // Nếu không tìm thấy nước đi (AI thua), kết thúc game
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
     * Chức năng gợi ý nước đi cho người chơi.
     */
    public void showHint(GameState state) {
        if (state == null || state.isGameOver() || isThinking) return;

        final Board snapshot = state.getBoard().copy();
        final Types.PlayerColor humanColor = state.getCurrentPlayer().getColor();

        Task<Move> hintTask = new Task<>() {
            @Override
            protected Move call() {
                // Sử dụng AI để tìm nước đi tốt nhất cho người chơi
                return ai.getBestMove(snapshot, humanColor);
            }
        };

        hintTask.setOnSucceeded(e -> {
            Move hint = hintTask.getValue();
            if (hint != null) {
                // Hiển thị gợi ý lên bàn cờ thông qua GameController
                gameCtrl.updateViewSelection(hint.getStartRow(), hint.getStartCol(), java.util.List.of(hint));
            }
        });

        new Thread(hintTask).start();
    }

    public void stop() {
        isThinking = false;
    }
}