module com.checkers { // Thay 'com.checkers' bằng tên package của bạn
    requires javafx.controls;
    requires javafx.fxml;
    
    // Thêm dòng 'transitive' này để sửa lỗi bạn đang gặp
    requires transitive javafx.graphics; 

    // Cho phép FXML truy cập vào các Controller
    opens com.checkers.controller to javafx.fxml;
    
    // Xuất package để các module khác có thể sử dụng
    exports com.checkers;
    exports com.checkers.controller;
}