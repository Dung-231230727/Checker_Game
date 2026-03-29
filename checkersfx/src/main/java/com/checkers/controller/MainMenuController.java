package com.checkers.controller;

import com.checkers.ai.AIConfig;
import com.checkers.utils.MessageBox;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class MainMenuController {
    private AIConfig ai1Config = new AIConfig(AIConfig.Mode.BALANCED, 5);
    private AIConfig ai2Config = new AIConfig(AIConfig.Mode.BALANCED, 5);
    private AIConfig hintConfig = new AIConfig(AIConfig.Mode.BALANCED, 5);
    @FXML private Button btnStartGame;
    @FXML private Button btnSelectMode;
    private int selectedMode = 0; // Mặc định là PvP

    @FXML
    private void showModeMenu(ActionEvent event) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem pvpItem = new MenuItem("Người Vs Người (PVP)");
        pvpItem.setOnAction(e -> updateSelectedMode(0, "PVP"));
        
        MenuItem pveItem = new MenuItem("Người Vs Máy (PVE)");
        pveItem.setOnAction(e -> updateSelectedMode(1, "PVE"));
        
        MenuItem eveItem = new MenuItem("Máy Vs Máy (EVE)");
        eveItem.setOnAction(e -> updateSelectedMode(2, "EVE"));
        
        contextMenu.getItems().addAll(pvpItem, pveItem, eveItem);
        // Hiển thị dính ngay dưới nút Chọn Chế độ
        contextMenu.show(btnSelectMode, Side.BOTTOM, 0, 0);
    }
    
    private void updateSelectedMode(int mode, String modeName) {
        this.selectedMode = mode;
        if (btnStartGame != null) {
            btnStartGame.setText("BẮT ĐẦU: " + modeName);
        }
    }

    @FXML
    private void handleStartSelectedMode(ActionEvent event) throws IOException {
        loadGameMode(event, selectedMode);
    }

    private void loadGameMode(ActionEvent event, int mode) {
        try {
            // Sử dụng getClass() thay vì App.class
            URL fxmlUrl = getClass().getResource("/com/checkers/controller/game_window.fxml");
            
            if (fxmlUrl == null) {
                System.err.println("LỖI NGHIÊM TRỌNG: Không tìm thấy game_window.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            
            GameController gameCtrl = loader.getController();
            
            // Truyền cấu hình AI từ Menu sang Game
            gameCtrl.setInitialConfigs(this.ai1Config, this.ai2Config, this.hintConfig);
            gameCtrl.startGame(mode);
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            
        } catch (Exception | Error e) {
            // In ra TẤT CẢ các lỗi để không bao giờ bị "chết ngầm" nữa
            System.err.println("LỖI KHI NẠP MÀN HÌNH GAME:");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSettings(ActionEvent event) {
       try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/checkers/controller/settings_dialog.fxml"));
            Node settingsContent = loader.load();
            
            // Ẩn nút Thoát khi mở từ menu chính
            SettingsController sc = loader.getController();
            sc.setFromMenu(true);
            
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

    @FXML
    private void handleOpenMatchSettings() {
        try {
            URL fxmlLocation = getClass().getResource("/com/checkers/controller/match_settings.fxml");
            if (fxmlLocation == null) {
                System.err.println("LỖI: Không tìm thấy file match_settings.fxml!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Node root = loader.load();
            
            MatchSettingsController ctrl = loader.getController();
            // Từ menu chính: luôn hiển thị đủ cả 2 AI để cấu hình trước
            ctrl.init(ai1Config, ai2Config, hintConfig, 2);

            MessageBox.showCustom("THIẾT LẬP MÁY", root, MessageBox.MessageButtons.OK);
            
            ctrl.save(ai1Config, ai2Config, hintConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}