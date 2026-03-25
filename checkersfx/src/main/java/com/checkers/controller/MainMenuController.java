package com.checkers.controller;

import com.checkers.App;
import com.checkers.utils.MessageBox;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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

    @FXML
    public void onExit(ActionEvent event) {
        if (MessageBox.show("THOÁT GAME", "Bạn có chắc chắn muốn đóng trò chơi không?", MessageBox.MessageButtons.YES_NO)) {
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    @FXML
    private void handleInstructions(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/checkers/controller/guide_content.fxml"));
            Node guideContent = loader.load();
            
            MessageBox.showCustom("HƯỚNG DẪN", guideContent, MessageBox.MessageButtons.OK);
        } catch (IOException e) {
            MessageBox.show("HƯỚNG DẪN", "Luật chơi: Ăn hết quân đối phương để giành chiến thắng!", MessageBox.MessageButtons.OK);
        }
    }
}