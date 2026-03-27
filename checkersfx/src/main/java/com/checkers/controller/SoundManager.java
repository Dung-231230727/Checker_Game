package com.checkers.controller;

import javafx.scene.media.AudioClip;
import java.util.prefs.Preferences;

public class SoundManager {
    private static AudioClip moveSound;
    private static AudioClip captureSound;
    
    // Cầu chì tổng
    private static boolean isMutedTemporarily = false;

    public static void loadSounds() {
        try {
            moveSound = new AudioClip(SoundManager.class.getResource("/com/checkers/assets/sounds/move1.mp3").toExternalForm());
            captureSound = new AudioClip(SoundManager.class.getResource("/com/checkers/assets/sounds/move.mp3").toExternalForm());
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không tìm thấy file âm thanh.");
        }
    }

    public static void playMoveSound() {
        // LUÔN đọc trực tiếp từ Preferences để lấy giá trị mới nhất
        boolean isEnabled = Preferences.userNodeForPackage(SettingsController.class).getBoolean("sound", true);
        
        if (!isMutedTemporarily && isEnabled && moveSound != null) {
            // Dừng cái cũ trước khi phát cái mới để tránh bị kẹt luồng (lượt sau không kêu)
            moveSound.stop(); 
            moveSound.play();
        }
    }

    public static void playCaptureSound() {
        boolean isEnabled = Preferences.userNodeForPackage(SettingsController.class).getBoolean("sound", true);
        
        if (!isMutedTemporarily && isEnabled && captureSound != null) {
            captureSound.stop();
            captureSound.play();
        }
    }

    public static void stopAllSounds() {
        isMutedTemporarily = true; 
        if (moveSound != null) moveSound.stop();
        if (captureSound != null) captureSound.stop();
    }

    public static void forceUnmute() {
        isMutedTemporarily = false;
    }
}