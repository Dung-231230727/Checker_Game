package com.checkers.controller;

import java.io.IOException;
import java.net.URL;
import java.util.Stack;
import java.util.List;

import com.checkers.ai.AIConfig;
import com.checkers.model.*;
import com.checkers.utils.MessageBox;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;

public class GameController {

    @FXML private StackPane centerContainer; 
    private BoardController boardPanelController;
    private GameState gameState;
    private AIController aiController;
    private Timeline gameLoop;
    private Pane animationOverlay; 
    private Label pauseLabel; 
    private TranslateTransition currentTransition;

    @FXML private Circle player1ColorCircle, player2ColorCircle;
    @FXML private Label player1NameLabel, player2NameLabel;
    @FXML private Label player1TimerLabel, player2TimerLabel, totalTimerLabel;

    @FXML private VBox controlsP1, controlsP2;
    @FXML private HBox mainHBox;
    @FXML private Button btnPauseP1, btnUndoP1, btnHintP1, btnSurrenderP1;
    @FXML private Button btnPauseP2, btnUndoP2, btnHintP2, btnSurrenderP2;
    @FXML private Button btnStart, btnExit, btnMatchSettings, btnPauseSystem;

    private int turnTimeLimit = 60; 
    private int player1TurnTime, player2TurnTime;
    private int totalMatchTime = 600; 
    private boolean isGameStarted = false; 

    private Stack<Board> boardHistory = new Stack<>();
    private Stack<Types.PlayerColor> turnHistory = new Stack<>();
    
    private AIConfig ai1Config = new AIConfig(AIConfig.Mode.BALANCED, 5); 
    private AIConfig ai2Config = new AIConfig(AIConfig.Mode.BALANCED, 5); 
    private AIConfig hintConfig = new AIConfig(AIConfig.Mode.BALANCED, 5);
    
    private int currentGameMode = 1; 

    public enum GamePhase { STANDBY, HUMAN_TURN, AI_TURN, ANIMATING, PAUSED, GAME_OVER, ANIMATING_IN_PAUSE }
    private GamePhase currentPhase = GamePhase.STANDBY;
    private GamePhase phaseBeforePause = GamePhase.STANDBY;

    public GamePhase getCurrentPhase() { return currentPhase; }
    public GameState getGameState() { return gameState; }
    public AIConfig getHintConfig() { return hintConfig; }
    public AIConfig getAi1Config() { return ai1Config; }
    public AIConfig getAi2Config() { return ai2Config; }
    public void handleGameOver() { if(gameLoop != null) gameLoop.stop(); }

    public void setPhase(GamePhase newPhase) {
        this.currentPhase = newPhase;
        if (gameLoop != null && isGameStarted) {
            if (newPhase == GamePhase.HUMAN_TURN || newPhase == GamePhase.AI_TURN || newPhase == GamePhase.ANIMATING) gameLoop.play();
            else gameLoop.pause();
        }
        centerContainer.setMouseTransparent(newPhase != GamePhase.HUMAN_TURN);
        togglePauseOverlay(newPhase == GamePhase.PAUSED);

        switch (newPhase) {
            case STANDBY:
            case GAME_OVER:
                cleanup();
                if (controlsP1 != null) controlsP1.setVisible(false);
                if (controlsP2 != null) controlsP2.setVisible(false);
                btnPauseSystem.setVisible(false);
                btnPauseSystem.setManaged(false);
                btnStart.setVisible(true); btnStart.setManaged(true); btnStart.setDisable(false);
                btnExit.setVisible(true); btnExit.setManaged(true);
                btnStart.setText(newPhase == GamePhase.STANDBY ? "Bắt đầu" : "Chơi lại");
                btnMatchSettings.setDisable(false);
                break;
            case HUMAN_TURN:
            case AI_TURN:
            case ANIMATING:
            case ANIMATING_IN_PAUSE:
                if (currentTransition != null && currentTransition.getStatus() == javafx.animation.Animation.Status.PAUSED) {
                    currentTransition.play();
                }
                btnStart.setVisible(false); btnStart.setManaged(false);
                btnExit.setVisible(false); btnExit.setManaged(false);
                btnMatchSettings.setDisable(false);
                updatePauseButtonText("Tạm dừng");
                if (currentGameMode == 2) {
                    btnPauseSystem.setVisible(true);
                    btnPauseSystem.setManaged(true);
                    btnPauseSystem.setDisable(false);
                    if (controlsP1 != null) controlsP1.setVisible(false);
                    if (controlsP2 != null) controlsP2.setVisible(false);
                } else {
                    btnPauseSystem.setVisible(false);
                    btnPauseSystem.setManaged(false);
                    boolean isP1AI = (gameState.getPlayerByColor(Types.PlayerColor.WHITE).getType() == Types.PlayerType.AI);
                    boolean isP2AI = (gameState.getPlayerByColor(Types.PlayerColor.BLUE).getType() == Types.PlayerType.AI);
                    if (controlsP1 != null) controlsP1.setVisible(!isP1AI);
                    if (controlsP2 != null) controlsP2.setVisible(!isP2AI);
                    updatePlayerControls(controlsP1, Types.PlayerColor.WHITE);
                    updatePlayerControls(controlsP2, Types.PlayerColor.BLUE);
                }
                break;
            case PAUSED:
                if (currentTransition != null) currentTransition.pause();
                updatePauseButtonText("Tiếp tục");
                if (btnPauseSystem != null) btnPauseSystem.setDisable(false);
                if (controlsP1 != null) controlsP1.setDisable(false); 
                if (controlsP2 != null) controlsP2.setDisable(false);
                break;
        }
    }

    private void updatePlayerControls(VBox controls, Types.PlayerColor color) {
        if (gameState == null || controls == null) return;
        boolean isMyTurn = (gameState.getCurrentPlayerColor() == color);
        Button btnPause = (color == Types.PlayerColor.WHITE) ? btnPauseP1 : btnPauseP2;
        Button btnUndo = (color == Types.PlayerColor.WHITE) ? btnUndoP1 : btnUndoP2;
        Button btnHint = (color == Types.PlayerColor.WHITE) ? btnHintP1 : btnHintP2;
        Button btnSurrender = (color == Types.PlayerColor.WHITE) ? btnSurrenderP1 : btnSurrenderP2;
        btnPause.setDisable(false);
        btnPause.setOpacity(1.0);
        btnSurrender.setDisable(false);
        btnSurrender.setOpacity(1.0);
        boolean canPlay = isMyTurn && currentPhase != GamePhase.ANIMATING;
        btnHint.setDisable(!canPlay);
        btnHint.setOpacity(canPlay ? 1.0 : 0.4);
        int req = (currentGameMode == 1) ? 2 : 1;
        boolean canUndo = canPlay && boardHistory.size() >= req;
        btnUndo.setDisable(!canUndo);
        btnUndo.setOpacity(canUndo ? 1.0 : 0.4);
        controls.setDisable(false);
        controls.setOpacity(1.0);
    }

    private void togglePauseOverlay(boolean show) {
        if (show) {
            if (pauseLabel == null) {
                pauseLabel = new Label("GAME PAUSED");
                pauseLabel.setStyle("-fx-font-size: 45px; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.7); -fx-padding: 40px; -fx-background-radius: 20px; -fx-border-color: white; -fx-border-width: 2px; -fx-border-radius: 20px;");
                pauseLabel.setAlignment(Pos.CENTER);
                StackPane.setAlignment(pauseLabel, Pos.CENTER);
            }
            if (!centerContainer.getChildren().contains(pauseLabel)) centerContainer.getChildren().add(pauseLabel);
        } else {
            if (pauseLabel != null) centerContainer.getChildren().remove(pauseLabel);
        }
    }

    @FXML
    public void initialize() {
        SoundManager.loadSounds();
        this.aiController = new AIController(this);
        animationOverlay = new Pane(); 
        animationOverlay.setMouseTransparent(true);
        centerContainer.getChildren().add(animationOverlay);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/checkers/controller/board_panel.fxml"));
            GridPane boardGrid = loader.load();
            this.boardPanelController = loader.getController();
            centerContainer.getChildren().add(0, boardGrid);
            this.boardPanelController.setGameController(this);
            NumberBinding size = Bindings.min(mainHBox.widthProperty().subtract(360), mainHBox.heightProperty()).subtract(20);
            boardGrid.prefWidthProperty().bind(size); 
            boardGrid.prefHeightProperty().bind(size);
            boardGrid.maxWidthProperty().bind(size); 
            boardGrid.maxHeightProperty().bind(size);
        } catch (IOException e) { e.printStackTrace(); }
        totalTimerLabel.setText("10:00");
        player1TimerLabel.setText("60s");
        player2TimerLabel.setText("60s");
        setPhase(GamePhase.STANDBY);
    }

    public void startGame(int mode) {
        this.currentGameMode = mode;
        this.isGameStarted = true;
        SoundManager.forceUnmute(); // Mở khóa âm thanh khi bắt đầu
        Player p1 = (mode == 2) ? new Player("AI Trắng", Types.PlayerColor.WHITE, Types.PlayerType.AI) : new Player("Player 1", Types.PlayerColor.WHITE, Types.PlayerType.HUMAN);
        Player p2 = (mode == 0) ? new Player("Player 2", Types.PlayerColor.BLUE, Types.PlayerType.HUMAN) : new Player("AI Xanh", Types.PlayerColor.BLUE, Types.PlayerType.AI);
        this.gameState = new GameState(p1, p2);
        boardHistory.clear(); 
        turnHistory.clear();
        boardPanelController.refreshBoard(gameState.getBoard());
        if (player1NameLabel != null) player1NameLabel.setText(p1.getName()); 
        if (player2NameLabel != null) player2NameLabel.setText(p2.getName());
        if (player1ColorCircle != null) player1ColorCircle.setFill(Color.WHITE);
        if (player2ColorCircle != null) player2ColorCircle.setFill(Color.web("#3498db"));
        totalMatchTime = 600; 
        resetTurnTimers();
        totalTimerLabel.setText(formatTime(totalMatchTime));
        if (gameLoop != null) gameLoop.stop();
        gameLoop = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimers()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
    }

    private void updateTimers() {
        if (gameState.isGameOver() || !isGameStarted) return;
        if (totalMatchTime > 0) { 
            totalMatchTime--; 
            totalTimerLabel.setText(formatTime(totalMatchTime)); 
        } else { 
            handleTimeout("Trận đấu hòa do hết giờ tổng!"); 
            return; 
        }
        if (gameState.getCurrentPlayerColor() == Types.PlayerColor.WHITE) {
            if (player1TurnTime > 0) { player1TurnTime--; player1TimerLabel.setText(player1TurnTime + "s"); } 
            else handleTimeout(player2NameLabel.getText() + " THẮNG!");
        } else {
            if (player2TurnTime > 0) { player2TurnTime--; player2TimerLabel.setText(player2TurnTime + "s"); } 
            else handleTimeout(player1NameLabel.getText() + " THẮNG!");
        }
    }

    private void resetTurnTimers() { 
        player1TurnTime = turnTimeLimit; 
        player2TurnTime = turnTimeLimit; 
        if (player1TimerLabel != null) player1TimerLabel.setText(player1TurnTime + "s"); 
        if (player2TimerLabel != null) player2TimerLabel.setText(player2TurnTime + "s"); 
    }
    
    private String formatTime(int s) { return String.format("%02d:%02d", s / 60, s % 60); }

    private void handleTimeout(String msg) {
        if (gameLoop != null) gameLoop.stop();
        gameState.setGameOver(true); 
        this.isGameStarted = false;
        setPhase(GamePhase.GAME_OVER);
        Alert a = new Alert(AlertType.INFORMATION); 
        a.setTitle("Hết giờ"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML private void handleStartGame() {
        startGame(currentGameMode);
        if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) { 
            setPhase(GamePhase.AI_TURN); 
            if (aiController != null) aiController.processAITurn(gameState); 
        } else setPhase(GamePhase.HUMAN_TURN);
    }

    @FXML private void handlePause() {
        if (!isGameStarted) return; 
        if (currentPhase == GamePhase.PAUSED) resumeGame();
        else { this.phaseBeforePause = currentPhase; setPhase(GamePhase.PAUSED); updatePauseButtonText("Tiếp tục"); }
    }

    private void updatePauseButtonText(String text) {
        if (btnPauseP1 != null) btnPauseP1.setText(text);
        if (btnPauseP2 != null) btnPauseP2.setText(text);
        if (btnPauseSystem != null) btnPauseSystem.setText(text);
    }

    private void handleSurrenderAfterConfirm() {
        if (gameLoop != null) gameLoop.stop(); 
        if (aiController != null) aiController.stop();
        this.isGameStarted = false; 
        setPhase(GamePhase.STANDBY);
        if (totalTimerLabel != null) totalTimerLabel.setText("10:00");
        resetTurnTimers();
    }

    @FXML private void handleSurrender() {
        this.phaseBeforePause = currentPhase; 
        setPhase(GamePhase.PAUSED);
        if (MessageBox.show("ĐẦU HÀNG", "Bạn có chắc chắn muốn đầu hàng không?", MessageBox.MessageButtons.YES_NO)) handleSurrenderAfterConfirm();
        else setPhase(this.phaseBeforePause);
    }

    @FXML private void handleShowHint() {
        if (currentPhase != GamePhase.HUMAN_TURN) return;
        if (gameState.getCurrentPlayerColor() == Types.PlayerColor.WHITE) btnHintP1.setText("Đang tính...");
        else btnHintP2.setText("Đang tính...");
        setPhase(GamePhase.ANIMATING); 
        if (aiController != null) aiController.showHint(gameState);
    }

    public void resetHintButton() { if (currentPhase == GamePhase.ANIMATING && !gameState.isGameOver()) setPhase(GamePhase.HUMAN_TURN); }

    @FXML private void handleUndoMove() {
        if (currentPhase != GamePhase.HUMAN_TURN) return;
        int steps = (currentGameMode == 1) ? 2 : 1;
        if (boardHistory.size() >= steps) {
            for (int i = 0; i < steps; i++) { gameState.setBoard(boardHistory.pop()); gameState.setCurrentPlayerColor(turnHistory.pop()); }
            boardPanelController.clearSelection(); boardPanelController.refreshBoard(gameState.getBoard());
            resetTurnTimers(); if(aiController != null) aiController.stop(); setPhase(GamePhase.HUMAN_TURN);
        }
    }

    // --- PHẦN SỬA LỖI ÂM THANH ---
    public void applyMove(Move move) {
        if (gameState.isGameOver()) return;
        setPhase(GamePhase.ANIMATING);
        SoundManager.forceUnmute(); // Đảm bảo âm thanh luôn được mở trước khi phát

        boardHistory.push(gameState.getBoard().copy()); 
        turnHistory.push(gameState.getCurrentPlayerColor());

        StackPane startSquare = boardPanelController.getSquareAt(move.getStartRow(), move.getStartCol());
        StackPane endSquare = boardPanelController.getSquareAt(move.getEndRow(), move.getEndCol());
        Node tempPiece = startSquare.getChildren().stream().filter(node -> node instanceof StackPane).findFirst().orElse(null);

        if (tempPiece == null) { finalizeMove(move); return; }

        final Node finalPieceView = tempPiece;
        boardPanelController.clearSelection();
        finalPieceView.getStyleClass().add("piece-moving");
        Bounds startBounds = startSquare.localToScene(startSquare.getBoundsInLocal());
        Bounds endBounds = endSquare.localToScene(endSquare.getBoundsInLocal());
        Point2D startTopLeft = animationOverlay.sceneToLocal(startBounds.getMinX(), startBounds.getMinY());
        Point2D endTopLeft = animationOverlay.sceneToLocal(endBounds.getMinX(), endBounds.getMinY());
        ((StackPane) finalPieceView).setPrefSize(startBounds.getWidth(), startBounds.getHeight());

        startSquare.getChildren().remove(finalPieceView);
        finalPieceView.setLayoutX(startTopLeft.getX());
        finalPieceView.setLayoutY(startTopLeft.getY());
        animationOverlay.getChildren().add(finalPieceView);

        currentTransition = new TranslateTransition(Duration.millis(300), finalPieceView);
        currentTransition.setToX(endTopLeft.getX() - startTopLeft.getX());
        currentTransition.setToY(endTopLeft.getY() - startTopLeft.getY());

        // Phát âm thanh
        if (move.isJump()) SoundManager.playCaptureSound(); 
        else SoundManager.playMoveSound();

        currentTransition.setOnFinished(e -> {
            currentTransition = null; 
            animationOverlay.getChildren().remove(finalPieceView);
            boolean wasPromotion = Move.isPromotionMove(gameState.getBoard(), move);
            gameState.getBoard().applyMove(move);
            boardPanelController.refreshBoard(gameState.getBoard());

            // KHÔNG gọi stopAllSounds ở đây nữa để tránh làm mất tiếng các nước đi liên tục
            handleNextTurnLogic(move, wasPromotion); 
        });
        currentTransition.play();
    }

    private void finalizeMove(Move move) {
        if (move.isJump()) {
            int midR = (move.getStartRow() + move.getEndRow()) / 2;
            int midC = (move.getStartCol() + move.getEndCol()) / 2;
            gameState.getBoard().setPiece(midR, midC, null);
        }
        boolean wasPromotion = Move.isPromotionMove(gameState.getBoard(), move);
        gameState.getBoard().applyMove(move);
        boardPanelController.refreshBoard(gameState.getBoard());
        handleNextTurnLogic(move, wasPromotion);
    }

    private void handleNextTurnLogic(Move move, boolean prom) {
        if (move.isJump() && !prom && MoveController.canJumpAgain(gameState.getBoard(), move.getEndRow(), move.getEndCol())) {
            boardPanelController.setForcedPiece(move.getEndRow(), move.getEndCol());
            setPhase(GamePhase.HUMAN_TURN);
        } else {
            boardPanelController.clearForcedPiece();
            resetTurnTimers(); 
            gameState.switchTurn();
            if (gameState.isGameOver()) setPhase(GamePhase.GAME_OVER);
            else {
                if (currentPhase == GamePhase.PAUSED || phaseBeforePause == GamePhase.PAUSED) {
                    this.phaseBeforePause = (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) ? GamePhase.AI_TURN : GamePhase.HUMAN_TURN;
                    return; 
                }
                if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) { 
                    setPhase(GamePhase.AI_TURN); 
                    if(aiController != null) aiController.processAITurn(gameState); 
                } else setPhase(GamePhase.HUMAN_TURN);
            }
        }
    }

    public void setInitialConfigs(AIConfig ai1, AIConfig ai2, AIConfig hint) {
        this.ai1Config.setMode(ai1.getCurrentMode()); this.ai1Config.setSearchDepth(ai1.getSearchDepth());
        this.ai2Config.setMode(ai2.getCurrentMode()); this.ai2Config.setSearchDepth(ai2.getSearchDepth());
        this.hintConfig.setMode(hint.getCurrentMode()); this.hintConfig.setSearchDepth(hint.getSearchDepth());
    }

    @FXML private void handleOpenSettings() {
        try {
            if (isGameStarted && currentPhase != GamePhase.PAUSED) { this.phaseBeforePause = currentPhase; setPhase(GamePhase.PAUSED); }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/checkers/controller/settings_dialog.fxml"));
            Node root = loader.load();
            MessageBox.showCustom("CÀI ĐẶT", root, MessageBox.MessageButtons.OK);
            resumeGame();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleOpenMatchSettings(ActionEvent event) {
        try {
            if (isGameStarted && currentPhase != GamePhase.PAUSED) { this.phaseBeforePause = currentPhase; setPhase(GamePhase.PAUSED); }
            URL fxmlLocation = getClass().getResource("/com/checkers/controller/match_settings.fxml");
            if (fxmlLocation == null) return;
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Node root = loader.load();
            MatchSettingsController ctrl = loader.getController();
            ctrl.init(ai1Config, ai2Config, hintConfig, currentGameMode);
            MessageBox.showCustom("THIẾT LẬP MÁY", root, MessageBox.MessageButtons.OK);
            ctrl.save(ai1Config, ai2Config, hintConfig);
            resumeGame();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML public void handleExitToMenu() {
        cleanup();
        try { isGameStarted = false; com.checkers.App.setRoot("controller/main_menu"); } catch (IOException e) { e.printStackTrace(); }
    }

    public void updateViewSelection(int r, int c, List<Move> moves) { if (boardPanelController != null) boardPanelController.setSelectedPieceForHint(r, c, moves); }

    public void cleanup() {
        SoundManager.stopAllSounds();
        if (currentTransition != null) { currentTransition.stop(); currentTransition.setOnFinished(null); currentTransition = null; }
        if (aiController != null) aiController.stop();
        if (gameLoop != null) gameLoop.stop();
    }

    private void resumeGame() {
        if (!isGameStarted) return;
        SoundManager.forceUnmute();
        setPhase(phaseBeforePause);
        updatePauseButtonText("Tạm dừng");
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
        delay.setOnFinished(e -> { if (phaseBeforePause == GamePhase.AI_TURN && aiController != null) aiController.processAITurn(gameState); });
        delay.play();
    }
}