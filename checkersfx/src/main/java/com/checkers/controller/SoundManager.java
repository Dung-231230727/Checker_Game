package com.checkers.controller;

import javafx.scene.media.AudioClip;
import java.util.prefs.Preferences;

public class SoundManager {
    private static AudioClip moveSound;
    private static AudioClip captureSound;
    private static Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);

    // Hàm này sẽ tải âm thanh vào bộ nhớ (Gọi 1 lần lúc bật game)
    public static void loadSounds() {
        try {
            moveSound = new AudioClip(SoundManager.class.getResource("/com/checkers/assets/sounds/move.mp3").toExternalForm());
            captureSound = new AudioClip(SoundManager.class.getResource("/com/checkers/assets/sounds/capture.mp3").toExternalForm());
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không tìm thấy file âm thanh trong assets/sounds/");
        }
    }

    public static void playMoveSound() {
        // Chỉ phát nếu cài đặt Sound đang Bật (true) và file không bị lỗi
        if (prefs.getBoolean("sound", true) && moveSound != null) {
            moveSound.play();
        }
    }

    public static void playCaptureSound() {
        if (prefs.getBoolean("sound", true) && captureSound != null) {
            captureSound.play();
        }
    }

    public static void stopAllSounds() {
        if (moveSound != null && moveSound.isPlaying()) {
            moveSound.stop();
        }
        if (captureSound != null && captureSound.isPlaying()) {
            captureSound.stop();
        }
    }
}