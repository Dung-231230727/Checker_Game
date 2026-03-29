package com.checkers.controller;

import com.checkers.model.*;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BoardController {

    @FXML
    private GridPane boardGrid; 

    private GameController gameCtrl;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<Move> validMovesForSelected = new ArrayList<>();
    // Biến để khóa quân cờ khi nhảy liên hoàn
    private int forcedRow = -1;
    private int forcedCol = -1;
    private boolean isHintMode = false;
    private ScaleTransition pulseAnimation;

    public void setForcedPiece(int r, int c) {
        this.forcedRow = r;
        this.forcedCol = c;
        
        // SỬA Ở ĐÂY: Chỉ vẽ chấm tròn và vòng chọn quân nếu đang là lượt của NGƯỜI CHƠI
        if (gameCtrl.getGameState().getCurrentPlayer().getType() == Types.PlayerType.HUMAN) {
            this.selectedRow = r;
            this.selectedCol = c;
            
            // Chỉ cho phép các nước nhảy của chính quân này
            this.validMovesForSelected = MoveController.getMovesForPiece(gameCtrl.getGameState().getBoard(), r, c)
                .stream().filter(Move::isJump).collect(Collectors.toList());
        } else {
            // Nếu là MÁY ĐÁNH, xóa trắng lựa chọn để không hiện chấm tròn
            this.selectedRow = -1;
            this.selectedCol = -1;
            this.validMovesForSelected.clear();
        }
            
        refreshBoard(gameCtrl.getGameState().getBoard());
    }

    public void clearForcedPiece() {
        this.forcedRow = -1;
        this.forcedCol = -1;
        clearSelection();
    }

    public void setGameController(GameController gameCtrl) {
        this.gameCtrl = gameCtrl;
    }

    public GridPane getBoardGrid() {
        return boardGrid;
    }

    @FXML
    public void initialize() {
        setupConstraints();
    }

    /**
     * Thiết lập tỷ lệ 12.5% cho mỗi ô để bàn cờ tự co giãn.
     */
    private void setupConstraints() {
        boardGrid.getColumnConstraints().clear();
        boardGrid.getRowConstraints().clear();
        for (int i = 0; i < 8; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(12.5);
            boardGrid.getColumnConstraints().add(col);

            RowConstraints row = new RowConstraints();
            row.setPercentHeight(12.5);
            boardGrid.getRowConstraints().add(row);
        }
    }

    /**
     * Vẽ lại toàn bộ bàn cờ dựa trên dữ liệu từ Model.
     */
    public void refreshBoard(Board board) {
        boardGrid.getChildren().clear();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                StackPane square = new StackPane();
                square.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                
                // Thiết lập màu nền ô cờ
                String styleClass = (row + col) % 2 == 0 ? "light-square" : "dark-square";
                square.getStyleClass().add(styleClass);

                // Thêm hiệu ứng Highlight nếu ô này nằm trong danh sách nước đi hợp lệ
                if (isMoveTarget(row, col)) {
                    square.getStyleClass().add("square-highlight");
                }

                // Lấy quân cờ từ Model để vẽ
                Piece piece = board.getPiece(row, col);
                if (piece != null) {
                    String colorClass = (piece.getColor() == Types.PlayerColor.WHITE) ? "white-piece" : "blue-piece";
                    
                    // SỬA CHỖ NÀY: Đổi Circle thành StackPane
                    StackPane pieceView = createPiece(colorClass, square, piece.isKing());
                    
                    // KIỂM TRA ĐỂ ĐỔI MÀU VIỀN
                    if (row == selectedRow && col == selectedCol) {
                        if (!pieceView.getChildren().isEmpty()) {
                            javafx.scene.Node innerPiece = pieceView.getChildren().get(0);
                            if (isHintMode) {
                                innerPiece.getStyleClass().add("piece-hinted");
                            } else {
                                innerPiece.getStyleClass().add("piece-selected");
                            }
                            // ===== PULSE ANIMATION =====
                            if (!isHintMode) {
                                startPulse(pieceView);
                            }
                        }
                    }
                    
                    square.getChildren().add(pieceView);
                } else {
                    // NẾU Ô TRỐNG: Kiểm tra xem có phải là đích đến của nước đi hợp lệ không
                    if (isMoveTarget(row, col)) {
                        Circle dot = new Circle();
                        dot.getStyleClass().add("suggest-dot");
                        
                        // Ràng buộc bán kính nhỏ (khoảng 15-20% ô vuông)
                        dot.radiusProperty().bind(Bindings.min(
                            square.widthProperty().divide(6), 
                            square.heightProperty().divide(6)
                        ));
                        square.getChildren().add(dot);
                        square.setStyle("-fx-cursor: hand;");
                    }
                }

                // Xử lý sự kiện click
                final int r = row;
                final int c = col;
                square.setOnMouseClicked(e -> handleSquareClick(r, c));

                boardGrid.add(square, col, row);
            }
        }
    }

    private StackPane createPiece(String colorClass, StackPane container, boolean isKing) {
        StackPane pieceWrapper = new StackPane();
        
        javafx.scene.layout.Region piece = new javafx.scene.layout.Region();
        piece.getStyleClass().addAll("piece", colorClass);

        // Ép hình TRÒN TUYỆT ĐỐI — Round Geometric (Clean Tactical Pastel)
        javafx.beans.binding.NumberBinding size = Bindings.min(
            container.widthProperty().multiply(0.68),
            container.heightProperty().multiply(0.68)
        );
        piece.maxWidthProperty().bind(size);
        piece.maxHeightProperty().bind(size);
        piece.minWidthProperty().bind(size);
        piece.minHeightProperty().bind(size);
        // CSS -fx-background-radius: 1000px sẽ ép tròn, nhưng thêm inline để chắc chắn
        piece.setStyle("-fx-background-radius: 1000px; -fx-border-radius: 1000px;");
        
        pieceWrapper.getChildren().add(piece);

        if (isKing) {
            javafx.scene.control.Label crown = new javafx.scene.control.Label("♔");
            crown.getStyleClass().add("crown-label");
            pieceWrapper.getChildren().add(crown);
            pieceWrapper.getStyleClass().add("is-king");
        }

        return pieceWrapper;
    }

    private void handleSquareClick(int row, int col) {
        if (gameCtrl == null || gameCtrl.getCurrentPhase() != GameController.GamePhase.HUMAN_TURN) {
                    return;
                }
        GameState state = gameCtrl.getGameState();
        if (state.isGameOver()) return;

        // --- CHẶN KHI ĐANG NHẢY LIÊN HOÀN ---
        if (forcedRow != -1 && forcedCol != -1) {
            if (isMoveTarget(row, col)) {
                Move move = findMoveInList(row, col);
                if (move != null) {
                    clearSelection();
                    gameCtrl.applyMove(move);
                }
            } else {
                // Nếu click sai chỗ khi đang nhảy liên hoàn, vẽ lại để nhắc nhở (vẫn giữ trạng thái chọn)
                refreshBoard(state.getBoard());
            }
            return; // Thoát ngay
        }
        
        Board board = state.getBoard();
        Types.PlayerColor color = state.getCurrentPlayerColor();

        // Lấy TẤT CẢ các nước đi hợp lệ của cả phe (đã bao gồm luật bắt buộc ăn)
        List<Move> allLegalMoves = MoveController.getAllLegalMoves(board, color);
        boolean mustJump = allLegalMoves.stream().anyMatch(Move::isJump);

        Piece clickedPiece = board.getPiece(row, col);

        // GIAI ĐOẠN 1: CHỌN QUÂN
        if (clickedPiece != null && clickedPiece.getColor() == color) {
            List<Move> pieceMoves = MoveController.getMovesForPiece(board, row, col);
            
            validMovesForSelected = pieceMoves.stream()
                .filter(m -> !mustJump || m.isJump())
                .filter(allLegalMoves::contains)
                .collect(Collectors.toList());

            if (!validMovesForSelected.isEmpty()) {
                selectedRow = row;
                selectedCol = col;
                isHintMode = false; // ĐẢM BẢO TẮT CỜ GỢI Ý KHI NGƯỜI CHƠI TỰ BẤM
            } else {
                clearSelection();
            }
            refreshBoard(board);
        }
        // GIAI ĐOẠN 2: THỰC HIỆN ĐI
        else if (selectedRow != -1 && isMoveTarget(row, col)) {
            Move move = findMoveInList(row, col);
            if (move != null) {
                clearSelection();
                gameCtrl.applyMove(move); // GameController sẽ xử lý nhảy liên hoàn
            }
        } else {
            // Nếu click ra ngoài hoặc chọn quân không hợp lệ
            clearSelection();
            refreshBoard(state.getBoard());
        }
    }

    private boolean isMoveTarget(int r, int c) {
        return validMovesForSelected.stream().anyMatch(m -> m.getEndRow() == r && m.getEndCol() == c);
    }

    private Move findMoveInList(int r, int c) {
        return validMovesForSelected.stream()
                .filter(m -> m.getEndRow() == r && m.getEndCol() == c)
                .findFirst().orElse(null);
    }

    public void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        validMovesForSelected.clear();
        isHintMode = false;
        stopPulse();
    }

    /** Bắt đầu hiệu ứng nhịp thở (Pulse) cho quân cờ đang được chọn */
    private void startPulse(StackPane target) {
        stopPulse();
        pulseAnimation = new ScaleTransition(Duration.millis(500), target);
        pulseAnimation.setFromX(1.0);
        pulseAnimation.setFromY(1.0);
        pulseAnimation.setToX(1.10);
        pulseAnimation.setToY(1.10);
        pulseAnimation.setAutoReverse(true);
        pulseAnimation.setCycleCount(ScaleTransition.INDEFINITE);
        pulseAnimation.play();
    }

    /** Dừng hiệu ứng Pulse */
    private void stopPulse() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }
    }

    public StackPane getSquareAt(int row, int col) {
        for (javafx.scene.Node node : boardGrid.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {
                return (StackPane) node;
            }
        }
        return null;
    }

    public void setSelectedPieceForHint(int r, int c, List<Move> moves) {
        this.selectedRow = r;
        this.selectedCol = c;
        this.validMovesForSelected = moves;
        this.isHintMode = true; // BẬT CỜ GỢI Ý LÊN
        
        refreshBoard(gameCtrl.getGameState().getBoard());
    }
}