package com.checkers.controller;

import java.util.Stack;
import com.checkers.model.*;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.event.ActionEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.layout.Pane;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class GameController {
    @FXML private StackPane centerContainer; 
    @FXML private BoardController boardPanelController;

    private GameState gameState;
    private AIController aiController;
    private Timeline gameLoop;
    private Pane animationOverlay; 

    @FXML private Circle player1ColorCircle;
    @FXML private Circle player2ColorCircle;
    @FXML private Label player1NameLabel;
    @FXML private Label player2NameLabel;
    @FXML private Label player1TimerLabel;
    @FXML private Label player2TimerLabel;
    @FXML private Label totalTimerLabel;

    private int turnTimeLimit = 60; 
    private int player1TurnTime;
    private int player2TurnTime;
    private int totalMatchTime = 600; 

    private Stack<Board> boardHistory = new Stack<>();
    private Stack<Types.PlayerColor> turnHistory = new Stack<>();
    
    @FXML
    public void initialize() {
        SoundManager.loadSounds();
        this.aiController = new AIController(this);
        
        animationOverlay = new Pane(); 
        animationOverlay.setMouseTransparent(true);
        centerContainer.getChildren().add(animationOverlay);
        
        if (boardPanelController != null) {
            boardPanelController.setGameController(this);
            NumberBinding size = Bindings.min(
                centerContainer.widthProperty(), 
                centerContainer.heightProperty()
            );
            
            boardPanelController.getBoardGrid().prefWidthProperty().bind(size);
            boardPanelController.getBoardGrid().prefHeightProperty().bind(size);
            boardPanelController.getBoardGrid().maxWidthProperty().bind(size);
            boardPanelController.getBoardGrid().maxHeightProperty().bind(size);
        }
    }

    public void applyMove(Move move) {
        if (gameState.isGameOver()) return;

        // 1. Lưu lịch sử ván đấu
        boardHistory.push(gameState.getBoard().copy());
        turnHistory.push(gameState.getCurrentPlayerColor());

        StackPane startSquare = boardPanelController.getSquareAt(move.getStartRow(), move.getStartCol());
        StackPane endSquare = boardPanelController.getSquareAt(move.getEndRow(), move.getEndCol());

        javafx.scene.Node tempPiece = startSquare.getChildren().stream()
                .filter(node -> node instanceof StackPane) 
                .findFirst().orElse(null);

        if (tempPiece == null) {
            finalizeMove(move);
            return;
        }

        final javafx.scene.Node finalPieceView = tempPiece;
        boardPanelController.clearSelection();
        finalPieceView.getStyleClass().add("piece-moving");

        // 2. Tính toán tọa độ và kích thước chuẩn
        Bounds startBounds = startSquare.localToScene(startSquare.getBoundsInLocal());
        Bounds endBounds = endSquare.localToScene(endSquare.getBoundsInLocal());

        Point2D startTopLeft = animationOverlay.sceneToLocal(startBounds.getMinX(), startBounds.getMinY());
        Point2D endTopLeft = animationOverlay.sceneToLocal(endBounds.getMinX(), endBounds.getMinY());

        ((StackPane) finalPieceView).setPrefSize(startBounds.getWidth(), startBounds.getHeight());

        startSquare.getChildren().remove(finalPieceView);
        finalPieceView.setLayoutX(startTopLeft.getX());
        finalPieceView.setLayoutY(startTopLeft.getY());
        animationOverlay.getChildren().add(finalPieceView);

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), finalPieceView);
        slide.setToX(endTopLeft.getX() - startTopLeft.getX());
        slide.setToY(endTopLeft.getY() - startTopLeft.getY());

        // 3. Phát âm thanh khi bắt đầu trượt
        if (gameLoop != null && gameLoop.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            if (move.isJump()) {
                SoundManager.playCaptureSound(); 
            } else {
                SoundManager.playMoveSound();
            }
        }

        slide.play();

        // 4. Xử lý khi kết thúc trượt
        slide.setOnFinished(e -> {
            // Xóa quân cờ khỏi lớp hiệu ứng ngay khi trượt xong
            animationOverlay.getChildren().remove(finalPieceView);
            
            // VẼ LẠI BÀN CỜ NGAY LẬP TỨC: Quân cờ xuất hiện ở ô đích ngay, không bị "mất tích"
            boolean wasPromotion = Move.isPromotionMove(gameState.getBoard(), move);
            gameState.getBoard().applyMove(move);
            boardPanelController.refreshBoard(gameState.getBoard());

            // 5. KHOẢNG NGHỊ 500MS: Vừa để người chơi nhìn, vừa để âm thanh phát hết hoàn toàn
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(500));
            pause.setOnFinished(event -> {
                // Ngắt âm thanh sau khi đã nghỉ xong
                SoundManager.stopAllSounds(); 

                // Chỉ xử lý logic tiếp theo nếu ván đấu chưa bị Reset
                if (gameLoop != null && gameLoop.getStatus() == javafx.animation.Animation.Status.RUNNING) {
                    handleNextTurnLogic(move, wasPromotion); 
                }
            });
            pause.play();
        });
    }

    // Hàm phụ để xử lý chuyển lượt hoặc nhảy tiếp
    private void handleNextTurnLogic(Move move, boolean wasPromotion) {
        boolean canJumpAgain = move.isJump() && !wasPromotion && 
                            MoveController.canJumpAgain(gameState.getBoard(), move.getEndRow(), move.getEndCol());

        if (canJumpAgain) {
            boardPanelController.setForcedPiece(move.getEndRow(), move.getEndCol());
            if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) {
                aiController.processAITurn(gameState);
            }
        } else {
            boardPanelController.clearForcedPiece();
            player1TurnTime = turnTimeLimit;
            player2TurnTime = turnTimeLimit;
            player1TimerLabel.setText(player1TurnTime + "s");
            player2TimerLabel.setText(player2TurnTime + "s");

            gameState.switchTurn();
            if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) {
                aiController.processAITurn(gameState);
            }
        }
    }

    private void finalizeMove(Move move) {
        boolean wasPromotion = Move.isPromotionMove(gameState.getBoard(), move);
        gameState.getBoard().applyMove(move);
        boardPanelController.refreshBoard(gameState.getBoard());

        boolean canJumpAgain = move.isJump() && !wasPromotion && 
                               MoveController.canJumpAgain(gameState.getBoard(), move.getEndRow(), move.getEndCol());

        if (canJumpAgain) {
            boardPanelController.setForcedPiece(move.getEndRow(), move.getEndCol());
            if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) {
                aiController.processAITurn(gameState);
            }
        } else {
            boardPanelController.clearForcedPiece();
            
            // RESET THỜI GIAN KHI ĐỔI LƯỢT
            player1TurnTime = turnTimeLimit;
            player2TurnTime = turnTimeLimit;
            player1TimerLabel.setText(player1TurnTime + "s");
            player2TimerLabel.setText(player2TurnTime + "s");

            gameState.switchTurn();
            if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) {
                aiController.processAITurn(gameState);
            }
        }
    }

    public void startGame(int mode) {
        // mode 0: Người vs Người | mode 1: Người vs Máy | mode 2: Máy vs Máy
        Player p1 = (mode == 2) ? new Player("AI Trắng", Types.PlayerColor.WHITE, Types.PlayerType.AI) : 
                                  new Player("Player 1", Types.PlayerColor.WHITE, Types.PlayerType.HUMAN);
        Player p2 = (mode == 0) ? new Player("Player 2", Types.PlayerColor.BLUE, Types.PlayerType.HUMAN) : 
                                  new Player("AI Xanh", Types.PlayerColor.BLUE, Types.PlayerType.AI);
        
        this.gameState = new GameState(p1, p2);
        
        // Làm sạch lịch sử khi bắt đầu ván mới
        boardHistory.clear();
        turnHistory.clear();

        boardPanelController.refreshBoard(gameState.getBoard());
        
        player1NameLabel.setText(p1.getName());
        player2NameLabel.setText(p2.getName());
        player1ColorCircle.setFill(Color.WHITE);
        player2ColorCircle.setFill(Color.web("#3498db"));
        
        player1TurnTime = turnTimeLimit;
        player2TurnTime = turnTimeLimit;
        totalMatchTime = 600; 
        
        player1TimerLabel.setText(player1TurnTime + "s");
        player2TimerLabel.setText(player2TurnTime + "s");
        totalTimerLabel.setText(formatTime(totalMatchTime));

        if (gameLoop != null) gameLoop.stop();
        gameLoop = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimers()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();

        // Nếu Player 1 là AI, mồi lượt đánh đầu tiên
        if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) {
            aiController.processAITurn(gameState);
        }
    }

    private void updateTimers() {
        if (gameState.isGameOver()) return;

        if (totalMatchTime > 0) {
            totalMatchTime--;
            totalTimerLabel.setText(formatTime(totalMatchTime));
        } else {
            handleTimeout("Trận đấu hòa do hết giờ tổng!");
            return;
        }

        if (gameState.getCurrentPlayerColor() == Types.PlayerColor.WHITE) {
            if (player1TurnTime > 0) {
                player1TurnTime--;
                player1TimerLabel.setText(player1TurnTime + "s");
            } else {
                handleTimeout(player2NameLabel.getText() + " THẮNG do đối phương hết giờ!");
            }
        } else {
            if (player2TurnTime > 0) {
                player2TurnTime--;
                player2TimerLabel.setText(player2TurnTime + "s");
            } else {
                handleTimeout(player1NameLabel.getText() + " THẮNG do đối phương hết giờ!");
            }
        }
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void handleTimeout(String message) {
        if (gameLoop != null) gameLoop.stop();
        gameState.setGameOver(true);
        
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Trận đấu kết thúc");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleResetGame(ActionEvent event) {
        try {
            // 1. Dừng AI ngay lập tức để nó không tính toán và gọi lệnh Move nữa
            if (aiController != null) {
                aiController.stop(); 
            }

            // 2. Dừng mọi âm thanh đang phát
            SoundManager.stopAllSounds();

            // 3. Quan trọng: Dừng bộ đếm thời gian (Timeline)
            if (gameLoop != null) {
                gameLoop.stop();
            }

            // 4. Quay về màn hình chính
            com.checkers.App.setRoot("controller/main_menu");
            
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void handleGameOver() { if(gameLoop != null) gameLoop.stop(); }
    public GameState getGameState() { return gameState; }

    public void updateViewSelection(int r, int c, java.util.List<Move> moves) {
        if (boardPanelController != null) {
            boardPanelController.setSelectedPieceForHint(r, c, moves);
        }
    }

    @FXML
    private void handleShowHint() {
        if (aiController != null && gameState != null) {
            aiController.showHint(gameState); 
        }
    }

    @FXML
    private void handleSurrender() {
        if (gameState.isGameOver()) return;
        
        if (gameLoop != null) gameLoop.stop();
        if (aiController != null) aiController.stop();
        
        gameState.setGameOver(true);
        
        String loser = (gameState.getCurrentPlayerColor() == Types.PlayerColor.WHITE) ? player1NameLabel.getText() : player2NameLabel.getText();
        String winner = (gameState.getCurrentPlayerColor() == Types.PlayerColor.WHITE) ? player2NameLabel.getText() : player1NameLabel.getText();
        
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Đầu hàng");
        alert.setHeaderText(loser + " đã giương cờ trắng!");
        alert.setContentText("Chúc mừng " + winner + " giành chiến thắng.");
        alert.showAndWait();
    }

    @FXML
    private void handleUndoMove() {
        if (boardHistory.isEmpty() || gameState.isGameOver()) return;

        if (aiController != null) aiController.stop();

        // Lùi 2 bước nếu đánh với máy (để về đúng lượt của người)
        boolean isPvE = (gameState.getPlayer1().getType() != gameState.getPlayer2().getType());
        int stepsToUndo = isPvE ? 2 : 1;

        // Chỉ lùi nếu có đủ số bước trong lịch sử
        if (boardHistory.size() >= stepsToUndo) {
            for (int i = 0; i < stepsToUndo; i++) {
                gameState.setBoard(boardHistory.pop()); 
                gameState.setCurrentPlayerColor(turnHistory.pop()); 
            }
            
            boardPanelController.clearSelection();
            boardPanelController.clearForcedPiece();
            boardPanelController.refreshBoard(gameState.getBoard());
            
            // Reset thời gian lượt đi
            player1TurnTime = turnTimeLimit;
            player2TurnTime = turnTimeLimit;
            player1TimerLabel.setText(turnTimeLimit + "s");
            player2TimerLabel.setText(turnTimeLimit + "s");
            
            // Khởi động lại AI nếu sau khi lùi lại là lượt của AI (trường hợp lùi 1 bước trong PvE)
            if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) {
                aiController.processAITurn(gameState);
            }
        }
    }

    @FXML
    private void handleOpenSettings() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/checkers/controller/settings_dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage settingsStage = new javafx.stage.Stage();
            settingsStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            // 1. Thay UNDECORATED bằng TRANSPARENT
            settingsStage.initStyle(javafx.stage.StageStyle.TRANSPARENT); 
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            
            // 2. Làm nền Scene trong suốt
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT); 
            
            settingsStage.setScene(scene);
            settingsStage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}