package com.checkers.controller;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.prefs.Preferences;

public class SoundManager {
    private static AudioClip moveSound;
    private static AudioClip captureSound;
    private static MediaPlayer bgmPlayer;

    // Cầu chì tổng
    private static boolean isMutedTemporarily = false;
    private static boolean isBgmPlayingState = false;
    private static boolean isLoaded = false;

    public static void loadSounds() {
        if (isLoaded)
            return;
        isLoaded = true;

        try {
            moveSound = new AudioClip(
                    SoundManager.class.getResource("/com/checkers/assets/sounds/duckmove.mp3").toExternalForm());
            captureSound = new AudioClip(
                    SoundManager.class.getResource("/com/checkers/assets/sounds/duckcapture.mp3").toExternalForm());

            // Tải nhạc nền
            String musicPath = SoundManager.class
                    .getResource("/com/checkers/assets/music/Đừng làm trái tim anh đau.mp3").toExternalForm();
            Media media = new Media(musicPath);
            bgmPlayer = new MediaPlayer(media);
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Lặp vô hạn

            bgmPlayer.setOnReady(() -> {
                updateVolumes();
                updateMusicStatus();
            });

        } catch (Exception e) {
            System.err.println("Cảnh báo: Không tìm thấy file âm thanh hoặc nhạc nền.");
            e.printStackTrace();
        }
    }

    public static void updateVolumes() {
        Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
        double soundVol = prefs.getDouble("soundVolume", 1.0);
        double musicVol = prefs.getDouble("musicVolume", 1.0);

        if (moveSound != null)
            moveSound.setVolume(soundVol);
        if (captureSound != null)
            captureSound.setVolume(soundVol);
        if (bgmPlayer != null)
            bgmPlayer.setVolume(musicVol);
    }

    public static void updateMusicStatus() {
        if (bgmPlayer == null)
            return;

        boolean isEnabled = Preferences.userNodeForPackage(SettingsController.class).getBoolean("music", true);
        if (isEnabled && !isMutedTemporarily) {
            if (!isBgmPlayingState) {
                // Kiểm soát luồng ngặt nghèo để tránh gọi play() nhiều lần gây crash
                // MediaPlayer
                bgmPlayer.play();
                isBgmPlayingState = true;
            }
        } else {
            if (isBgmPlayingState) {
                bgmPlayer.pause();
                isBgmPlayingState = false;
            }
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
        // Chỉ dừng các hiệu ứng âm thanh ngắn, không dừng nhạc nền trừ khi muốn tắt hẳn
        if (moveSound != null)
            moveSound.stop();
        if (captureSound != null)
            captureSound.stop();
    }

    public static void pauseMusic() {
        if (bgmPlayer != null)
            bgmPlayer.pause();
    }

    public static void resumeMusic() {
        updateMusicStatus();
    }

    public static void forceUnmute() {
        isMutedTemporarily = false;
        updateMusicStatus();
    }
}