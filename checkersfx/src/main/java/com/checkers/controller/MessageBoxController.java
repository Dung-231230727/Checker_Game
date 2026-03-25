package com.checkers.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox; // Phải import HBox
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MessageBoxController {

    @FXML private Label titleLabel;
    @FXML private VBox contentArea;
    @FXML private HBox footerArea; 

    public void setContent(String title, Node content) {
        titleLabel.setText(title.toUpperCase());
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    public HBox getFooterArea() {
        return footerArea;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}