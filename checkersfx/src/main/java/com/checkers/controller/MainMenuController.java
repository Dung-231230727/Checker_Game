package com.checkers.controller;

import com.checkers.App;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private void handlePvP(ActionEvent event) throws IOException {
        loadGameMode(event, 0); // 0 là chế độ 2 người chơi
    }

    @FXML
    private void handlePvE(ActionEvent event) throws IOException {
        loadGameMode(event, 1); // 1 là chế độ đánh với máy
    }

    @FXML
    private void handleEvE(ActionEvent event) throws IOException {
        loadGameMode(event, 2); // 2 là chế độ máy đánh với máy
    }

    private void loadGameMode(ActionEvent event, int mode) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("controller/game_window.fxml"));
        Parent root = loader.load();
        
        // Lấy controller và truyền chế độ chơi vào
        GameController gameCtrl = loader.getController();
        gameCtrl.startGame(mode);
        
        // Lấy Stage hiện tại và đổi cảnh (Yêu cầu nút bấm phải có ActionEvent để lấy nguồn)
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    private void handleSettings(ActionEvent event) { // Ở GameController thì tên là handleOpenSettings
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("controller/settings_dialog.fxml"));
            Parent root = loader.load();
            
            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            
            // 1. CHUYỂN THÀNH TRONG SUỐT HOÀN TOÀN thay vì UNDECORATED
            settingsStage.initStyle(StageStyle.TRANSPARENT); 
            
            Scene scene = new Scene(root);
            // 2. TÔ MÀU NỀN CỦA SCENE THÀNH TRONG SUỐT (Để lộ góc bo tròn của CSS)
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT); 
            
            settingsStage.setScene(scene);
            settingsStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onExit(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleInstructions(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Hướng dẫn chơi");
        alert.setHeaderText("Luật chơi Checkers cơ bản");
        alert.setContentText(
            "1. Quân cờ chỉ di chuyển chéo về phía trước 1 ô.\n" +
            "2. Có thể nhảy qua quân đối phương để ăn.\n" +
            "3. Bắt buộc phải ăn quân nếu có cơ hội (Bao gồm nhảy liên hoàn).\n" +
            "4. Khi đi đến hàng cuối cùng của đối phương, quân cờ sẽ phong Vua (King).\n" +
            "5. Vua có thể đi lùi và tiến theo đường chéo.\n\n" +
            "Chúc bạn chơi game vui vẻ!"
        );
        alert.showAndWait();
    }
}