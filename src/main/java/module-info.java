module com.guiyomi {
    requires javafx.controls;
    requires transitive javafx.fxml;
    requires java.net.http;
    requires transitive javafx.graphics;
    requires firebase.admin;
    requires java.prefs;
    requires java.logging; 
    requires transitive javafx.media;
    requires jdk.compiler;
    requires com.google.auth.oauth2;
    requires com.google.api.client;
    requires transitive com.google.gson;
    requires org.checkerframework.checker.qual;
    requires transitive java.desktop;
	requires javafx.base;

    opens com.guiyomi to javafx.fxml;
    exports com.guiyomi;
}