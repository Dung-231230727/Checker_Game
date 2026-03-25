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

        // Lưu lịch sử trước khi thực hiện di chuyển
        boardHistory.push(gameState.getBoard().copy());
        turnHistory.push(gameState.getCurrentPlayerColor());

        StackPane startSquare = boardPanelController.getSquareAt(move.getStartRow(), move.getStartCol());
        StackPane endSquare = boardPanelController.getSquareAt(move.getEndRow(), move.getEndCol());

        Circle tempPiece = (Circle) startSquare.getChildren().stream()
                .filter(node -> node instanceof Circle).findFirst().orElse(null);

        if (tempPiece == null) {
            finalizeMove(move);
            return;
        }

        final Circle finalPieceView = tempPiece;
        boardPanelController.clearSelection(); 
        finalPieceView.getStyleClass().add("piece-moving");

        Bounds startBounds = startSquare.localToScene(startSquare.getBoundsInLocal());
        Bounds endBounds = endSquare.localToScene(endSquare.getBoundsInLocal());

        double startCenterX = startBounds.getMinX() + startBounds.getWidth() / 2;
        double startCenterY = startBounds.getMinY() + startBounds.getHeight() / 2;
        double endCenterX = endBounds.getMinX() + endBounds.getWidth() / 2;
        double endCenterY = endBounds.getMinY() + endBounds.getHeight() / 2;

        Point2D startPos = animationOverlay.sceneToLocal(startCenterX, startCenterY);
        Point2D endPos = animationOverlay.sceneToLocal(endCenterX, endCenterY);

        startSquare.getChildren().remove(finalPieceView);
        finalPieceView.setTranslateX(0);
        finalPieceView.setTranslateY(0);
        finalPieceView.setLayoutX(startPos.getX());
        finalPieceView.setLayoutY(startPos.getY());
        
        animationOverlay.getChildren().add(finalPieceView);

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), finalPieceView);
        slide.setToX(endPos.getX() - startPos.getX());
        slide.setToY(endPos.getY() - startPos.getY());

        slide.setOnFinished(e -> {
            animationOverlay.getChildren().remove(finalPieceView);
            finalPieceView.setLayoutX(0);
            finalPieceView.setLayoutY(0);
            finalPieceView.setTranslateX(0);
            finalPieceView.setTranslateY(0);
            finalizeMove(move);
        });
        
        slide.play();
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
            if (gameLoop != null) gameLoop.stop(); 
            if (aiController != null) aiController.stop(); 
            
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

        // Lùi 2 bước nếu đánh với máy
        boolean isPvE = (gameState.getPlayer1().getType() != gameState.getPlayer2().getType());
        int stepsToUndo = isPvE ? 2 : 1;

        for (int i = 0; i < stepsToUndo; i++) {
            if (!boardHistory.isEmpty()) {
                gameState.setBoard(boardHistory.pop()); 
                gameState.setCurrentPlayerColor(turnHistory.pop()); 
            }
        }

        boardPanelController.clearSelection();
        boardPanelController.clearForcedPiece();
        boardPanelController.refreshBoard(gameState.getBoard());
        
        player1TurnTime = turnTimeLimit;
        player2TurnTime = turnTimeLimit;
        player1TimerLabel.setText(turnTimeLimit + "s");
        player2TimerLabel.setText(turnTimeLimit + "s");
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