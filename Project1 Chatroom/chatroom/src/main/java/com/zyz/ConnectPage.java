package com.zyz;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.kordamp.bootstrapfx.scene.layout.Panel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectPage implements PageInterface {
    private static final Logger logger = LogManager.getLogger(ConnectPage.class);
    private static Label warning;  // Warning Label

    /**
     * Render the connect page
     *
     * @param primaryStage primary stage
     */
    @Override
    public void render(Stage primaryStage) {
        // Create a panel
        Panel panel = new Panel("Login To Chatroom");
        if (!panel.getStyleClass().contains("panel-primary"))
            panel.getStyleClass().add("panel-primary");

        // Create a grid layout
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);  // Center the grid
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().addAll("grid", "grid-padding", "mg-auto");
        panel.getStyleClass().add("panel-primary");

        // Create labels and text fields
        Label addressLabel = new Label("IP Address:");
        TextField addressInput = new TextField();
        addressLabel.getStyleClass().addAll("h4");
        addressInput.setPromptText("Enter IP Address...");
        addressInput.getStyleClass().addAll("form-control", "h4");

        Label portLabel = new Label("Port:");
        TextField portInput = new TextField();
        portLabel.getStyleClass().addAll("h4");
        portInput.setPromptText("Enter Port...");
        portInput.getStyleClass().addAll("form-control", "h4");

        Label userIdLabel = new Label("User ID:");
        TextField userIdInput = new TextField();
        userIdLabel.getStyleClass().addAll("h4");
        userIdInput.setPromptText("Enter User ID...");
        userIdInput.getStyleClass().addAll("form-control", "h4");

        warning = new Label("Please enter all fields");
        warning.getStyleClass().addAll("h4");
        warning.setTextFill(Color.RED);
        warning.setVisible(false);

        // Create connect button
        Button connectButton = new Button("Join Chatroom");
        // Center the button text
        connectButton.setAlignment(Pos.CENTER);
        connectButton.getStyleClass().setAll("btn", "btn-primary", "btn-success", "h4");
        connectButton.setOnAction((ActionEvent event) -> {
            logger.info("Button clicked");
            logger.info("Address: " + addressInput.getText());
            logger.info("Port: " + portInput.getText());
            logger.info("User ID: " + userIdInput.getText());
            try {
                if (userIdInput.getText().isEmpty()) {
                    warning.setText("Please enter your User ID");
                    warning.setVisible(true);
                    return;
                }
                if (portInput.getText().isEmpty()) {
                    warning.setText("Please enter the port");
                    warning.setVisible(true);
                    return;
                }
                if (addressInput.getText().isEmpty()) {
                    warning.setText("Please enter the IP address");
                    warning.setVisible(true);
                    return;
                }
                Render.login(addressInput.getText(), Integer.parseInt(portInput.getText()), userIdInput.getText());
            } catch (Exception e) {
                logger.error("Login failed: " + e.getMessage());
            }
        });

        // Create server button
        Button serverButton = new Button("Create Chatroom");
        // Center the button text
        serverButton.setAlignment(Pos.CENTER);
        serverButton.getStyleClass().setAll("btn", "btn-primary", "btn-success", "h4");
        serverButton.setOnAction((ActionEvent event) -> {
            logger.info("Button clicked");
            if (portInput.getText().isEmpty() && userIdInput.getText().isEmpty()) {
                warning.setText("Please enter port and User ID");
                warning.setVisible(true);
                return;
            }
            if (userIdInput.getText().isEmpty()) {
                warning.setText("Please enter your User ID");
                warning.setVisible(true);
                return;
            }
            if (portInput.getText().isEmpty()) {
                warning.setText("Please enter the port");
                warning.setVisible(true);
                return;
            }
            try {
                Render.createServer(Integer.parseInt(portInput.getText()), userIdInput.getText());
            } catch (Exception e) {
                logger.error("Create server failed: " + e.getMessage());
            }
        });

        // Button to read local chat record
        Button readRecordButton = new Button("Read Local Chat Record");
        readRecordButton.setAlignment(Pos.CENTER);
        readRecordButton.getStyleClass().setAll("btn", "btn-primary", "btn-success", "h4");
        readRecordButton.setOnAction((ActionEvent event) -> {
            logger.info("Button clicked: Read Local Chat Record");
            try {
                Render.loadLocalChatRecord();
            } catch (Exception e) {
                logger.error("Read local chat record failed: " + e.getMessage());
            }
        });

        // Create advanced settings
        TitledPane advancedSettings = new TitledPane();
        advancedSettings.setText("Advanced Settings");
        VBox advancedContent = new VBox(10);
        advancedContent.getChildren().addAll(addressLabel, addressInput, portLabel, portInput);
        advancedSettings.setContent(advancedContent);
        advancedSettings.setExpanded(false); // Fold the advanced settings by default

        // Set default values
        addressInput.setText("localhost");
        portInput.setText("1234");

        // Set the layout
        grid.add(userIdLabel, 0, 0);
        grid.add(userIdInput, 1, 0);
        grid.add(advancedSettings, 0, 1, 2, 1);
        grid.add(warning, 1, 2);
        grid.add(connectButton, 1, 3);
        grid.add(serverButton, 1, 4);
        grid.add(readRecordButton, 1, 5);

        // Add the grid to the panel
        grid.setAlignment(Pos.CENTER);
        grid.setPrefHeight(500);
        grid.setPrefWidth(800);
        panel.setBody(grid);
        // Set the scene
        Scene scene = new Scene(panel, 1024, 720);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Show error message
     *
     * @param message error message
     */
    public static void showError(String message) {
        Platform.runLater(() -> {
            if (warning != null) {
                warning.setText(message);
                warning.setVisible(true);
            } else {
                logger.error("Label warning is null, but show error");
            }
        });

    }

    /**
     * Show loading message
     */
    public static void showLoading() {
        Platform.runLater(() -> {
            if (warning != null) {
                warning.setText("Connecting...");
                warning.setVisible(true);
            } else {
                logger.error("Label warning is null, but show loading");
            }
        });
    }

    /**
     * Hide loading message
     */
    public static void hideLoading() {
        if (warning != null) {
            warning.setVisible(false);
        } else {
            logger.error("Label warning is null, but hide loading");
        }
    }
}
