module top.oneyi.envLauncher {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.xml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens top.oneyi.envLauncher to javafx.fxml;
    exports top.oneyi.envLauncher;
    opens top.oneyi.envLauncher.controller to javafx.fxml;


}
