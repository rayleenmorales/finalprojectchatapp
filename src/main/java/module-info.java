module com.guiyomi {
    requires javafx.controls;
    requires transitive javafx.fxml;
    requires java.net.http;
    requires transitive org.json;
    requires transitive com.google.gson;
    requires transitive javafx.graphics;
    requires java.base;
    requires com.google.auth.oauth2;
    requires com.google.auth;
    
    opens com.guiyomi to javafx.fxml;
    exports com.guiyomi;
}
