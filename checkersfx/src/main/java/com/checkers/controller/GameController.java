package com.checkers.controller;

import java.io.IOException;
import java.util.Stack;
import com.checkers.model.*;
import com.checkers.utils.MessageBox;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.event.ActionEvent;
import javafx.animation.Animation;
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
    
    @FXML private Button btnStart, btnExit, btnPause, btnUndo, btnHint, btnSurrender;


    private int currentGameMode = 1; 
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

        setUIPplaying(false);
    
        // Hiển thị mốc thời gian mặc định
        totalTimerLabel.setText("10:00");
        player1TimerLabel.setText("60s");
        player2TimerLabel.setText("60s");
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
        this.currentGameMode = mode;
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
    public void handleStartGame() {
        // 1. Khởi tạo dữ liệu game (quân cờ, người chơi)
        startGame(currentGameMode); 
        isGameStarted = true;

        // 2. Bắt đầu kích hoạt đồng hồ đếm ngược tại đây
        if (gameLoop != null) gameLoop.stop();
        gameLoop = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimers()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play(); // ĐỒNG HỒ CHỈ CHẠY KHI NHẤN NÚT

        // 3. Chuyển đổi giao diện sang trạng thái Playing
        setUIPplaying(true);

        // 4. Nếu lượt đầu là AI thì cho đánh luôn
        if (gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) {
            aiController.processAITurn(gameState);
        }
    }

    @FXML
    public void handleExitToMenu() {
        boolean confirm = MessageBox.show("HỦY VÁN ĐẤU", 
            "Ván đấu hiện tại sẽ không được lưu. Bạn muốn quay về Menu?", 
            MessageBox.MessageButtons.YES_NO);
    
        if (confirm) {
            if (aiController != null) aiController.stop();
            SoundManager.stopAllSounds();
            try {
                com.checkers.App.setRoot("controller/main_menu");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handlePause() {
        // 1. Tạm dừng tất cả các hoạt động hiện tại
        if (gameLoop != null) gameLoop.pause(); 
        if (aiController != null) aiController.stop();

        // 2. Tạo nội dung đếm ngược cho phần "ruột" của thông báo
        VBox pauseBox = new VBox(15);
        pauseBox.setAlignment(Pos.CENTER);
        
        Label timerLabel = new Label("Thời gian tạm dừng còn lại: 05:00");
        timerLabel.setStyle("-fx-text-fill: #8E1451; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label msgLabel = new Label("Chọn 'Đồng ý' để tiếp tục hoặc 'Hủy bỏ' để thoát ván đấu.");
        msgLabel.setStyle("-fx-text-fill: #1a1a1a;"); // Màu đen để nổi trên nền trắng
        msgLabel.setWrapText(true);

        pauseBox.getChildren().addAll(timerLabel, msgLabel);

        // 3. Logic đếm ngược 5 phút trong lúc hiện thông báo
        final int[] secondsLeft = {300}; 
        Timeline pauseTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft[0]--;
            timerLabel.setText(String.format("Thời gian tạm dừng còn lại: %02d:%02d", 
                            secondsLeft[0] / 60, secondsLeft[0] % 60));
            
            if (secondsLeft[0] <= 0) {
                // Hết giờ thì tự đóng thông báo (mặc định là hủy ván)
                if (timerLabel.getScene() != null) {
                    ((Stage)timerLabel.getScene().getWindow()).close();
                }
            }
        }));
        pauseTimeline.setCycleCount(Animation.INDEFINITE);
        pauseTimeline.play();

        // 4. Hiển thị MessageBox với 2 nút YES_NO
        // true = Tiếp tục (Đồng ý), false = Hủy bỏ (Hủy bỏ)
        boolean resume = MessageBox.showCustom("TẠM DỪNG", pauseBox, MessageBox.MessageButtons.YES_NO);
        
        pauseTimeline.stop();

        if (resume) {
            // --- TRƯỜNG HỢP: TIẾP TỤC ---
            if (gameLoop != null) gameLoop.play();
            
            // Nếu là lượt của AI, kích hoạt máy đánh tiếp
            if (gameState != null && !gameState.isGameOver() && 
                gameState.getCurrentPlayer().getType() == Types.PlayerType.AI) {
                aiController.processAITurn(gameState);
            }
        } else {
            // --- TRƯỜNG HỢP: HỦY BỎ VÁN ĐẤU ---
            handleSurrenderAfterConfirm(); 
        }
    }

    // Hàm phụ để reset game ngay lập tức mà không cần hỏi lại lần nữa
    private void handleSurrenderAfterConfirm() {
        if (gameLoop != null) gameLoop.stop();
        if (aiController != null) aiController.stop();
        
        setUIPplaying(false); // Đưa UI về trạng thái chờ
        startGame(this.currentGameMode); // Reset bàn cờ
        if (gameLoop != null) gameLoop.stop(); // Đảm bảo đồng hồ không tự chạy
        
        totalTimerLabel.setText("10:00");
        player1TimerLabel.setText("60s");
        player2TimerLabel.setText("60s");
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
    public void handleSurrender() {
        boolean confirm = MessageBox.show("XÁC NHẬN", "Bạn có thực sự muốn đầu hàng?", MessageBox.MessageButtons.YES_NO);
        
        if (confirm) {
            isGameStarted = false;
            if (gameLoop != null) gameLoop.stop();
            if (aiController != null) aiController.stop();
            
            MessageBox.show("KẾT THÚC", "Bạn đã đầu hàng!", MessageBox.MessageButtons.OK);
            
            // Reset về trạng thái ban đầu
            setUIPplaying(false);
            startGame(this.currentGameMode);

            if (gameLoop != null) gameLoop.stop();
            
            // Reset đồng hồ hiển thị
            totalTimerLabel.setText("10:00");
            player1TimerLabel.setText("60s");
            player2TimerLabel.setText("60s");
        }
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
            // 1. Nạp file FXML chứa các nút gạt (Âm thanh, Nhạc, Rung)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/checkers/controller/settings_dialog.fxml"));
            Node settingsContent = loader.load();
            
            // 2. Gọi MessageBox hiển thị giao diện Cài đặt
            // Dùng nút OK để đóng lại sau khi chỉnh sửa
            MessageBox.showCustom("CÀI ĐẶT", settingsContent, MessageBox.MessageButtons.OK);
            
        } catch (IOException e) {
            e.printStackTrace();
            MessageBox.show("LỖI", "Không thể tải cấu hình cài đặt!", MessageBox.MessageButtons.OK);
        }
    }

    private void setUIPplaying(boolean isPlaying) {
        // Nhóm ban đầu (Start và Exit)
        btnStart.setVisible(!isPlaying);
        btnStart.setManaged(!isPlaying);
        btnExit.setVisible(!isPlaying);
        btnExit.setManaged(!isPlaying);

        // Kiểm tra nếu là chế độ Máy vs Máy (Mode 2)
        boolean isEvE = (currentGameMode == 2);

        // Nút Tạm dừng luôn hiện khi đang chơi
        btnPause.setVisible(isPlaying);
        btnPause.setManaged(isPlaying);

        // Các nút chức năng chỉ hiện khi đang chơi và KHÔNG PHẢI chế độ Máy vs Máy
        btnUndo.setVisible(isPlaying && !isEvE);
        btnUndo.setManaged(isPlaying && !isEvE);
        
        btnHint.setVisible(isPlaying && !isEvE);
        btnHint.setManaged(isPlaying && !isEvE);
        
        btnSurrender.setVisible(isPlaying && !isEvE);
        btnSurrender.setManaged(isPlaying && !isEvE);
    }

    private boolean isGameStarted = false;

    public boolean isGameStarted() {
        return isGameStarted;
    }
}