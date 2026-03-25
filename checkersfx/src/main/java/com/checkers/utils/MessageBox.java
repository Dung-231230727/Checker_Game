package com.checkers.utils;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.checkers.controller.MessageBoxController;

public class MessageBox {

    private static double xOffset = 0;
    private static double yOffset = 0;

    public enum MessageButtons {
        OK,
        YES_NO
    }

    /**
     * 1. Hiển thị thông báo chữ đơn giản
     */
    public static boolean show(String title, String message, MessageButtons buttonType) {
        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: #8E1451; -fx-font-size: 18px; -fx-font-weight: bold;");
        lbl.setAlignment(Pos.CENTER);
        return showCustom(title, lbl, buttonType);
    }

    /**
     * 2. Hiển thị giao diện tùy chỉnh (Giữ nguyên 3 tham số như cũ)
     */
    public static boolean showCustom(String title, Node content, MessageButtons buttonType) {
        try {
            FXMLLoader loader = new FXMLLoader(MessageBox.class.getResource("/com/checkers/controller/message_box.fxml"));
            Parent root = loader.load();
            MessageBoxController controller = loader.getController();
            
            // Thiết lập tiêu đề và phần ruột
            controller.setContent(title, content);

            // Logic xử lý Stage
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            // --- MỚI: LOGIC KÉO THẢ CỬA SỔ ---
            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });

            root.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });

            final boolean[] result = {false};

            // 3. Tự động hóa việc tạo nút bấm dựa trên Enum (Giữ nguyên logic cũ)
            controller.getFooterArea().getChildren().clear();

            if (buttonType == MessageButtons.OK) {
                Button btnOk = new Button("Xác nhận");
                btnOk.getStyleClass().add("apply-button");
                btnOk.setMinWidth(100);
                btnOk.setOnAction(e -> { result[0] = true; stage.close(); });
                controller.getFooterArea().getChildren().add(btnOk);
            } 
            else if (buttonType == MessageButtons.YES_NO) {
                Button btnYes = new Button("Đồng ý");
                btnYes.getStyleClass().add("apply-button");
                btnYes.setMinWidth(100);
                btnYes.setOnAction(e -> { result[0] = true; stage.close(); });

                Button btnNo = new Button("Hủy bỏ");
                btnNo.getStyleClass().add("close-button");
                btnNo.setMinWidth(100);
                btnNo.setOnAction(e -> { result[0] = false; stage.close(); });

                controller.getFooterArea().getChildren().addAll(btnYes, btnNo);
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            // --- MỚI: LOGIC HIỆN RA GIỮA MÀN HÌNH ---
            // Sử dụng setOnShown để đảm bảo stage đã tính toán xong kích thước thực tế
            stage.setOnShown(event -> {
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
                stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
            });

            stage.showAndWait();

            return result[0];
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi nạp MessageBox FXML");
            return false;
        }
    }
}