package com.checkers.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

public class SettingsController {
    // Khai báo các ID khớp với fx:id trong FXML
    @FXML private StackPane toggleSound;
    @FXML private Circle dotSound;
    
    @FXML private StackPane toggleMusic;
    @FXML private Circle dotMusic;
    
    @FXML private StackPane toggleVibration;
    @FXML private Circle dotVibration;

    @FXML private VBox rootBox;;

    private boolean isSoundOn = false;
    private boolean isMusicOn = false;
    private boolean isVibrationOn = false;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // Ghi nhận vị trí chuột khi vừa bấm vào cửa sổ
        rootBox.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        // Di chuyển cửa sổ theo vị trí chuột khi kéo
        rootBox.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootBox.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    private void handleToggleSound() {
        isSoundOn = !isSoundOn;
        animateToggle(isSoundOn, toggleSound, dotSound);
    }

    // BỔ SUNG PHƯƠNG THỨC NÀY ĐỂ HẾT LỖI DÒNG 33
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

    private void animateToggle(boolean isOn, StackPane track, Circle dot) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(200), dot);
        if (isOn) {
            transition.setToX(12);
            track.getStyleClass().add("toggle-track-on");
        } else {
            transition.setToX(-12);
            track.getStyleClass().remove("toggle-track-on");
        }
        transition.play();
    }

    @FXML
    private void onClose() {
        // Lấy cửa sổ hiện tại (dựa vào một Node bất kỳ, ví dụ toggleSound) và đóng nó
        if (toggleSound != null && toggleSound.getScene() != null) {
            Stage stage = (Stage) toggleSound.getScene().getWindow();
            stage.close();
        }
    }
}