module top.oneyi.jdktool {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens top.oneyi.envLauncher to javafx.fxml;
    exports top.oneyi.envLauncher;
    opens top.oneyi.envLauncher.controller to javafx.fxml;


}
