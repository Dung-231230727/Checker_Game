module com.checkers {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.media;
    requires java.prefs;

    opens com.checkers.controller to javafx.fxml;
    
    exports com.checkers;
    exports com.checkers.controller;
    exports com.checkers.model;
    exports com.checkers.ai;
}