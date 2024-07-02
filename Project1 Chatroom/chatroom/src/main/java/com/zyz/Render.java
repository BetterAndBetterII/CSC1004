package com.zyz;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Render extends Application {
    private static final Logger logger = LogManager.getLogger(Render.class);
    public static IPCCommunication ipc;
    public static void setIPC(IPCCommunication i) {
        ipc = i;
    }
    private static UIManager uiManager;
    public static String user;
    public static boolean serverMode = false;

    /**
     * Returns the isServerMode.
     *
     * @return the isServerMode
     */
    public static boolean isServerMode() {
        return serverMode;
    }

    /**
     * User send out message.
     *
     * @param message the message JSON string
     */
    public static void sendMsg(String message) {
        if (serverMode) {
            JSONObject json = new JSONObject();
            json.put("message", message);
            json.put("user", user);
            ipc.serverSendMsg(json.toString());
            return;
        }
        JSONObject data = new JSONObject();
        data.put("user", user);
        data.put("message", message);
        ipc.sendMessage(data.toString());
    }

    /**
     * User login.
     *
     * @param address the server address
     * @param port the server port
     * @param user the username
     */
    public static void login(String address, int port, String user) {
        JSONObject data = new JSONObject();
        data.put("address", address);
        data.put("port", port);
        data.put("user", user);
        try {
            ConnectPage.showLoading();
            ipc.login(data, res -> {
                if (res.equals("success")) {
                    logger.info("login success");
                    serverMode = false;
                    ConnectPage.hideLoading();
                    ChatPage.historyMode = false;
                    uiManager.switchToPage(new ChatPage());
                } else {
                    logger.error("login failed: " + res);
                    ConnectPage.showError(res);
                }
            });
        } catch (Exception e) {
            logger.error("login failed: " + e.getMessage());
            ConnectPage.showError("login failed");
        }
    }

    /**
     * User create server.
     *
     * @param port the server port
     * @param username the username
     */
    public static void createServer(int port, String username) {
        JSONObject data = new JSONObject();
        data.put("port", port);
        data.put("user", username);
        try {
            ipc.createServer(data, res -> {
                if (res.equals("success")) {
                    ChatPage.historyMode = false;
                    serverMode = true;
                    uiManager.switchToPage(new ChatPage());
                    logger.info("create server success");
                    setUser(username);
                } else {
                    logger.error("create server failed: " + res);
                    ConnectPage.showError(res);
                }
            });
        } catch (Exception e) {
            logger.error("create server failed: " + e.getMessage());
        }
    }

    /**
     * Receive message from server.
     *
     * @param message the message
     * @param user the user
     * @param time the time of the message
     * @param count the message count
     */
    public static void receiveMsg(String message, String user, String time, int count) {
        logger.info("receive message: " + message + " from " + user + " at " + time + " with count " + count);
        ChatPage.receiveMsg(message, user, time, count);
    }

    /**
     * Get history data.
     */
    public static void getHistoryData() {
        try {
            ipc.getHistoryData(null, res -> {
                JSONArray historyData = new JSONArray(res);
                ChatPage.loadHistoryData(historyData);
            });
        } catch (Exception e) {
            logger.error("get history data failed: " + e.getMessage());
        }
    }

    /**
     * Set user.
     *
     * @param user the username
     */
    public static void setUser(String user) {
        ChatPage.messageCount = 1;
        Render.user = user;
        ChatPage.setUser(user);
        updateServerInfo.startUpdating();
    }

    /**
     * Update header.
     */
    public static void updateHeader(){
        final int[] userNumber = {0};
        // 同步服务器信息
        ipc.getServerInfo(res -> {
            JSONObject json = new JSONObject(res);
            int newUserNumber = Integer.parseInt(json.get("userNumber").toString());
            if (newUserNumber == -1) {
                ConnectionLost();
                return;
            }
            if (newUserNumber == userNumber[0]) return;
            else userNumber[0] = newUserNumber;
            String host;
            if (serverMode) {
                host = Render.user;
            } else {
                host = json.get("host").toString();
            }
            String header = "Online: " + newUserNumber + " users";
            ChatPage.updateServerInfo(header, host);
        });
    }

    /**
     * Connection lost, switch to ConnectPage.
     */
    public static void ConnectionLost() {
        updateServerInfo.stopUpdating();
        ipc.leaveRoom(null);
        logger.error("connection lost");
        uiManager.switchToPage(new ConnectPage());
        ConnectPage.showError("connection lost, please try again later");
    }

    /**
     * Leave room and switch to ConnectPage.
     */
    public static void leaveRoom() {
        try {
            ipc.leaveRoom(null);
            logger.info("leave room success, shutdown server");
        } catch (Exception e) {
            logger.error("leave room failed: " + e.getMessage());
        }

        uiManager.switchToPage(new ConnectPage());
    }

    /**
     * Load local chat record.
     */
    public static void loadLocalChatRecord() {
        ipc.loadLocalChatRecord(null, res -> {
            logger.info("load local chat record: " + res);
            if (res.equals("[]")) {
                ConnectPage.showError("no chat record");
                return;
            }
            JSONArray historyData = new JSONArray(res);
            ChatPage.loadHistoryData(historyData);
            ChatPage.historyMode = true;
            uiManager.switchToPage(new ChatPage());
        });
    }

    /**
     * The updateServerInfo Thread to fetch server info every second.
     */
    public static class updateServerInfo implements Runnable {
        private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        public static void startUpdating() {
            // Run the updateServerInfo Thread every second
            if (scheduler.isShutdown() || scheduler.isTerminated()) {
                scheduler = Executors.newScheduledThreadPool(1);
            }
            // Try to submit the task to the scheduler
            try {
                scheduler.scheduleAtFixedRate(new updateServerInfo(), 0, 1, TimeUnit.SECONDS);
            } catch (RejectedExecutionException e) {
                logger.error("Failed to run task to scheduler: " + e.getMessage());
            }
        }

        public static void stopUpdating() {
            scheduler.shutdown();
        }

        @Override
        public void run() {
            try {
                updateHeader();
            } catch (Exception e) {
                logger.error("Update server info failed: " + e.getMessage());
                scheduler.shutdown();
            }
        }
    }

    /**
     * The main method.
     *
     * @param primaryStage the primary stage to render
     */
    @Override
    public void start(Stage primaryStage) {
        // Set the title of the stage
        primaryStage.setOnCloseRequest((WindowEvent we) -> {
            logger.info("try to close socket");
            ipc.closeSocket();
            logger.info("Thread count: " + Thread.activeCount());
            System.exit(0);
        });
        // Create a new ConnectPage by default
        ConnectPage connectPage = new ConnectPage();
        uiManager = new UIManager(primaryStage);
        uiManager.switchToPage(connectPage);
        primaryStage.show();
    }
}
