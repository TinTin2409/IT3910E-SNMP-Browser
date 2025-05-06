package snmpbrowser;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SNMPBrowserUI extends Application {

    @Override
    public void start(Stage stage) {
        // Input fields
        TextField ipField = new TextField("127.0.0.1");
        TextField communityField = new TextField("public");
        TextField oidField = new TextField("1.3.6.1.2.1.1.1.0"); // sysDescr

        ipField.setPromptText("IP Address");
        communityField.setPromptText("Community");
        oidField.setPromptText("OID");

        // Buttons
        Button getButton = new Button("GET");
        Button walkButton = new Button("WALK");

        // Result area
        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefHeight(300);

        // Layouts
        HBox inputBox = new HBox(10, new Label("IP:"), ipField,
                                      new Label("Community:"), communityField,
                                      new Label("OID:"), oidField);
        HBox buttonBox = new HBox(10, getButton, walkButton);
        VBox root = new VBox(15, inputBox, buttonBox, resultArea);
        root.setStyle("-fx-padding: 20;");

        // Events (mock for now)
        getButton.setOnAction(e -> resultArea.setText("GET result for OID: " + oidField.getText()));
        walkButton.setOnAction(e -> resultArea.setText("WALK results for OID: " + oidField.getText()));

        // Scene setup
        Scene scene = new Scene(root, 800, 400);
        stage.setScene(scene);
        stage.setTitle("SNMP Browser");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
