package org.zyz;

import javafx.application.Platform;
import javafx.stage.Stage;

public class UIManager {
    private final Stage primaryStage;
    public static String currentPage;

    public UIManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void switchToPage(PageInterface page) {
        if (Platform.isFxApplicationThread()) {
            page.render(primaryStage);
        } else {
            Platform.runLater(() -> page.render(primaryStage));
        }
        currentPage = page.getClass().getSimpleName();
    }
}


