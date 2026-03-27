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
    private Thread currentAIThread; // Quản lý thread để có thể can thiệp nếu cần

    public AIController(GameController gameCtrl) {
        this.gameCtrl = gameCtrl;
    }

    /**
     * Xử lý lượt đi của AI với cấu hình động.
     */
    public void processAITurn(GameState state) {
        // 1. KIỂM TRA PAUSE: Tuyệt đối không bắt đầu tính toán nếu game đang tạm dừng
        if (gameCtrl.getCurrentPhase() == GameController.GamePhase.PAUSED) {
            return;
        }

        if (state == null || state.isGameOver() || 
            state.getCurrentPlayer().getType() != Types.PlayerType.AI || isThinking) {
            return;
        }

        isThinking = true;
        
        final Board snapshot = state.getBoard().copy();
        final Types.PlayerColor aiColor = state.getCurrentPlayer().getColor();
        final AIConfig currentConfig = (aiColor == Types.PlayerColor.WHITE) ? 
                                        gameCtrl.getAi1Config() : gameCtrl.getAi2Config();

        // Tạo độ trễ nhỏ để người chơi kịp quan sát trước khi máy đi
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(e -> {
            // 2. KIỂM TRA LẠI: Sau khi hết thời gian chờ, nếu người dùng đã nhấn Pause thì dừng ngay
            if (gameCtrl.getCurrentPhase() == GameController.GamePhase.PAUSED) {
                isThinking = false;
                return;
            }

            Task<Move> aiTask = new Task<>() {
                @Override
                protected Move call() {
                    return ai.getBestMove(snapshot, aiColor, currentConfig);
                }
            };

            aiTask.setOnSucceeded(event -> {
                isThinking = false;
                
                // 3. KIỂM TRA HẬU KỲ: Nếu lúc tính xong mà game đang Pause, 
                // cất nước đi đó đi, không apply vào bàn cờ.
                if (gameCtrl.getCurrentPhase() == GameController.GamePhase.PAUSED) {
                    return;
                }

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

            currentAIThread = new Thread(aiTask);
            currentAIThread.setDaemon(true);
            currentAIThread.start();
        });
        pause.play();
    }

    /**
     * Chức năng gợi ý sử dụng cấu hình riêng cho Hint.
     */
    public void showHint(GameState state) {
        // Không gợi ý khi đang Pause
        if (gameCtrl.getCurrentPhase() == GameController.GamePhase.PAUSED) return;

        if (state == null || state.isGameOver() || isThinking || isHinting) return;
        if (state.getCurrentPlayer().getType() != Types.PlayerType.HUMAN) return;

        isHinting = true;

        final Board snapshot = state.getBoard().copy();
        final Types.PlayerColor humanColor = state.getCurrentPlayer().getColor();
        final AIConfig hConfig = gameCtrl.getHintConfig();

        Task<Move> hintTask = new Task<>() {
            @Override
            protected Move call() {
                return ai.getBestMove(snapshot, humanColor, hConfig);
            }
        };

        hintTask.setOnSucceeded(e -> {
            isHinting = false; 
            
            // Sau khi tính gợi ý xong, nếu đang Pause thì chỉ reset nút mà không vẽ gợi ý
            Platform.runLater(() -> gameCtrl.setPhase(GameController.GamePhase.HUMAN_TURN));

            if (gameCtrl.getCurrentPhase() == GameController.GamePhase.PAUSED) {
                return;
            }

            Move hint = hintTask.getValue();
            if (hint != null) {
                gameCtrl.updateViewSelection(hint.getStartRow(), hint.getStartCol(), java.util.List.of(hint));
            }
        });

        hintTask.setOnFailed(e -> {
            isHinting = false; 
            Platform.runLater(() -> gameCtrl.setPhase(GameController.GamePhase.HUMAN_TURN));
            hintTask.getException().printStackTrace();
        });

        Thread hintThread = new Thread(hintTask);
        hintThread.setDaemon(true);
        hintThread.start();
    }

    /**
     * Dừng hoàn toàn các hoạt động của AI.
     */
    public void stop() {
        isThinking = false;
        isHinting = false;
        
        // Nếu có Thread AI đang chạy, hãy ép nó dừng lại
        if (currentAIThread != null && currentAIThread.isAlive()) {
            currentAIThread.interrupt(); 
            currentAIThread = null;
        }
    }
}