module top.oneyi.jdktool {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens top.oneyi.jdktool to javafx.fxml;
    exports top.oneyi.jdktool;
    opens top.oneyi.jdktool.controller to javafx.fxml;


}
