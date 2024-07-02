package org.zyz;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Render extends Application {
    private static final Logger logger = LogManager.getLogger(Render.class);
    public static IPCCommunication ipc;

    public static void setIPC(IPCCommunication i) {
        ipc = i;
    }

    private static UIManager uiManager;

    @Override
    public void start(Stage primaryStage){
        // 设置窗口关闭事件处理器
        primaryStage.setOnCloseRequest((WindowEvent we) -> {
            System.exit(0);
        });
        logger.info("Render started");
        uiManager = new UIManager(primaryStage);
        uiManager.switchToPage(new PlayPage());
        primaryStage.show();
    }
}
