package com.checkers.controller;

import com.checkers.ai.AIConfig;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;

public class MatchSettingsController {

    @FXML private VBox sectionAI1, sectionAI2;
    
    // Các thành phần cho AI 1 (Trắng)
    @FXML private ComboBox<AIConfig.Mode> cbModeAI1;
    @FXML private Slider sliderDepthAI1;
    @FXML private Label lblDepthAI1;

    // Các thành phần cho AI 2 (Xanh)
    @FXML private ComboBox<AIConfig.Mode> cbModeAI2;
    @FXML private Slider sliderDepthAI2;
    @FXML private Label lblDepthAI2;

    // Các thành phần cho Gợi ý (Hint)
    @FXML private ComboBox<AIConfig.Mode> cbModeHint;
    @FXML private Slider sliderDepthHint;
    @FXML private Label lblDepthHint;

    @FXML
    public void initialize() {
        // Đổ dữ liệu vào các ComboBox Mode
        setupComboBox(cbModeAI1);
        setupComboBox(cbModeAI2);
        setupComboBox(cbModeHint);

        // Lắng nghe thay đổi của Slider để cập nhật Label hiển thị con số
        bindSliderToLabel(sliderDepthAI1, lblDepthAI1);
        bindSliderToLabel(sliderDepthAI2, lblDepthAI2);
        bindSliderToLabel(sliderDepthHint, lblDepthHint);
    }

    private void setupComboBox(ComboBox<AIConfig.Mode> cb) {
        cb.setItems(FXCollections.observableArrayList(AIConfig.Mode.values()));
    }

    private void bindSliderToLabel(Slider slider, Label label) {
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            label.setText(String.valueOf(newVal.intValue()));
        });
    }

    /**
     * Khởi tạo dữ liệu từ các config hiện tại của GameController vào View
     */
    public void init(AIConfig c1, AIConfig c2, AIConfig hint, int gameMode) {
        // Ẩn/Hiện vùng thiết lập tùy theo chế độ chơi
        // mode 0: P v P (Ẩn cả 2 AI), mode 1: P v E (Ẩn AI 2), mode 2: E v E (Hiện cả 2)
        sectionAI1.setVisible(gameMode == 2);
        sectionAI1.setManaged(gameMode == 2);
        
        sectionAI2.setVisible(gameMode != 0);
        sectionAI2.setManaged(gameMode != 0);

        // Gán giá trị AI 1
        cbModeAI1.setValue(c1.getCurrentMode());
        sliderDepthAI1.setValue(c1.getSearchDepth());
        lblDepthAI1.setText(String.valueOf(c1.getSearchDepth()));

        // Gán giá trị AI 2
        cbModeAI2.setValue(c2.getCurrentMode());
        sliderDepthAI2.setValue(c2.getSearchDepth());
        lblDepthAI2.setText(String.valueOf(c2.getSearchDepth()));

        // Gán giá trị Hint
        cbModeHint.setValue(hint.getCurrentMode());
        sliderDepthHint.setValue(hint.getSearchDepth());
        lblDepthHint.setText(String.valueOf(hint.getSearchDepth()));
    }

    /**
     * Lưu dữ liệu từ View ngược lại vào các đối tượng AIConfig
     */
    public void save(AIConfig c1, AIConfig c2, AIConfig hint) {
        // Lưu cho AI 1
        c1.setMode(cbModeAI1.getValue());
        c1.setSearchDepth((int) sliderDepthAI1.getValue());

        // Lưu cho AI 2
        c2.setMode(cbModeAI2.getValue());
        c2.setSearchDepth((int) sliderDepthAI2.getValue());

        // Lưu cho Hint
        hint.setMode(cbModeHint.getValue());
        hint.setSearchDepth((int) sliderDepthHint.getValue());
    }
}