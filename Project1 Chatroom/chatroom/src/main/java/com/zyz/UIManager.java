package com.zyz;

import javafx.application.Platform;
import javafx.stage.Stage;

public class UIManager {
    private final Stage primaryStage;
    public static String currentPage;

    /**
     * Constructor for UIManager
     * @param primaryStage the primary stage
     */
    public UIManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Switch to a new page
     * @param page the page to switch to
     */
    public void switchToPage(PageInterface page) {
        if (Platform.isFxApplicationThread()) {
            page.render(primaryStage);
        } else {
            Platform.runLater(() -> page.render(primaryStage));
        }
        currentPage = page.getClass().getSimpleName();
    }
}


