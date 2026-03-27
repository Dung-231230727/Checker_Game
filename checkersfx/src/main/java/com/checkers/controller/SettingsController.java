package com.checkers.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.util.Duration;

import java.io.IOException;
import java.util.prefs.Preferences; // Import thư viện lưu trữ

public class SettingsController {
    @FXML private StackPane toggleSound, toggleMusic, toggleVibration;
    @FXML private Circle dotSound, dotMusic, dotVibration;
    @FXML private VBox rootBox;

    private boolean isSoundOn, isMusicOn, isVibrationOn;
    private double xOffset = 0, yOffset = 0;
    
    // Khởi tạo Preferences cho class này
    private Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);

    @FXML
    public void initialize() {
        // 1. Tải các giá trị đã lưu (mặc định là true nếu chưa có)
        isSoundOn = prefs.getBoolean("sound", true);
        isMusicOn = prefs.getBoolean("music", true);
        isVibrationOn = prefs.getBoolean("vibration", true);

        // 2. Cập nhật giao diện nút gạt theo trạng thái đã lưu
        updateToggleUI(isSoundOn, toggleSound, dotSound);
        updateToggleUI(isMusicOn, toggleMusic, dotMusic);
        updateToggleUI(isVibrationOn, toggleVibration, dotVibration);

        // 3. Logic kéo thả cửa sổ
        rootBox.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        rootBox.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootBox.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    // Hàm cập nhật UI không dùng hiệu ứng (dùng khi mới mở dialog)
    private void updateToggleUI(boolean isOn, StackPane track, Circle dot) {
        if (isOn) {
            dot.setTranslateX(12);
            track.getStyleClass().add("toggle-track-on");
        } else {
            dot.setTranslateX(-12);
            track.getStyleClass().remove("toggle-track-on");
        }
    }

    @FXML
    private void handleToggleSound() {
        isSoundOn = !isSoundOn;
        animateToggle(isSoundOn, toggleSound, dotSound);
    }

    @FXML
    private void handleToggleMusic() {
        isMusicOn = !isMusicOn;
        animateToggle(isMusicOn, toggleMusic, dotMusic);
    }

    @FXML
    private void handleToggleVibration() {
        isVibrationOn = !isVibrationOn;
        animateToggle(isVibrationOn, toggleVibration, dotVibration);
    }

    @FXML
    private void handleApply() {
        // Chỉ lưu cài đặt khi người chơi ấn "Áp dụng"
        prefs.putBoolean("sound", isSoundOn);
        prefs.putBoolean("music", isMusicOn);
        prefs.putBoolean("vibration", isVibrationOn);
        
        System.out.println("Đã lưu cài đặt!");
        onClose(); // Lưu xong thì tự động đóng hộp thoại
    }
    
    private void animateToggle(boolean isOn, StackPane track, Circle dot) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(200), dot);
        if (isOn) {
            transition.setToX(12);
            if (!track.getStyleClass().contains("toggle-track-on"))
                track.getStyleClass().add("toggle-track-on");
        } else {
            transition.setToX(-12);
            track.getStyleClass().remove("toggle-track-on");
        }
        transition.play();
    }

    @FXML
    private void onClose() {
        if (rootBox.getScene() != null) {
            ((Stage) rootBox.getScene().getWindow()).close();
        }
    }

    @FXML
    private void handleExitToMenu(ActionEvent event) {
        // 1. Đóng cửa sổ Setting (Stage) ngay lập tức
        if (rootBox != null && rootBox.getScene() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) rootBox.getScene().getWindow();
            stage.close();
        }

        // 2. Dừng âm thanh (để chắc chắn không bị lọt tiếng)
        SoundManager.stopAllSounds();

        // 3. Chuyển Root về Menu chính
        try {
            com.checkers.App.setRoot("controller/main_menu");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}