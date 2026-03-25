package com.checkers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class App extends Application {
    private static Scene scene;
    private static Stage window;

    @Override
    public void start(Stage stage) throws IOException {
        window = stage;
        // Đặt tên tiêu đề cửa sổ
        window.setTitle("Checker Game"); 
        
        // Tải màn hình chính (Menu) đầu tiên
        scene = new Scene(loadFXML("controller/main_menu"), 1024, 768);
        window.setScene(scene);
        window.show();
    }

    // Hàm dùng chung để chuyển cảnh (Ví dụ: từ Menu -> Game)
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // Hàm tải file FXML
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}