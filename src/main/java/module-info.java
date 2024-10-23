module com.guiyomi {
    requires javafx.controls;
    requires transitive javafx.fxml;
    requires java.net.http;
    requires transitive org.json;
    requires transitive javafx.graphics;
    
    opens com.guiyomi to javafx.fxml;
    exports com.guiyomi;
}
