package com.zyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    // Create a new EventDispatcher instance
    public static final EventDispatcher dispatcher = new EventDispatcher();
    // Create a new IPCCommunication instance
    public static final IPCCommunication ipc = new IPCCommunication(dispatcher);
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static JSONArray historyData;

    /**
     * Connection class to store the connection status and socket
     */
    private static class Connection {
        public static boolean connected = false;
        public static Socket socket;
        public static PrintWriter toServer;
        public static BufferedReader fromServer;
        public static String address;
        public static String port;

        public static void setConnection(Socket socket, PrintWriter toServer, BufferedReader fromServer, String address, String port) {
            Connection.socket = socket;
            Connection.toServer = toServer;
            Connection.fromServer = fromServer;
            Connection.address = address;
            Connection.port = port;
        }
    }

    /**
     * Send request to server
     *
     * @param api:  the api to send to server
     * @param data: the data to send to server
     */
    private static void sendRequestToServer(String api, JSONObject data) {
        try {
            if (!Connection.connected) {
                logger.error("Not connected to server, failed to send request to server.");
                throw new ConnectException();
            }
            if (api.equals("close")) {
                logger.info("Close connection to server");
            }
            PrintWriter out = Connection.toServer;
            JSONObject json = new JSONObject();
            json.put("api", api);
            json.put("data", data);
            out.println(json);
        } catch (Exception e) {
            logger.error("Failed to send request to server: " + e.getMessage());
            Connection.connected = false;
            Render.ConnectionLost();
        }
    }

    /**
     * Handle the data from server
     */
    private static class handleFromServer implements Runnable {
        private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        public void startHandling() {
            logger.info("Start handling request from server...");

            // Test if the scheduler has been shut down
            if (scheduler.isShutdown() || scheduler.isTerminated()) {
                scheduler = Executors.newScheduledThreadPool(1);
            }

            // Try to submit the task to the scheduler
            try {
                scheduler.execute(this);
            } catch (RejectedExecutionException e) {
                logger.error("Failed to submit task to scheduler: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            while (Connection.connected) {
                try {
                    String data = Connection.fromServer.readLine();
                    if (data != null) {
                        dispatcher.dispatchEvent(new Event("server-request", data));
                    }
                } catch (Exception e) {
                    logger.error("Failed to read data from server: " + e.getMessage());
                    Connection.connected = false;
                    if(dispatcher.hasListener("login-success"))
                        dispatcher.dispatchEvent(new Event("login-success", "err server"));
                    Render.ConnectionLost();
                    if (Connection.socket.isClosed() || !Connection.connected) {
                        scheduler.shutdown(); // Stop the scheduler
                    }
                }
            }
        }
    }

    /**
     * Add history message to historyData
     *
     * @param user:    the user who send the message
     * @param message: the message
     * @param time:    the time the message was sent
     * @param count:   the count of the message
     */
    public static void addHistoryMsg(String user, String message, String time, int count) {
        JSONObject json = new JSONObject();
        json.put("user", user);
        json.put("message", message);
        json.put("time", time);
        json.put("count", count);
        historyData.put(json);
    }

    /**
     * Clear history data
     */
    public static void clearHistoryData() {
        historyData = new JSONArray();
        CreatingServer.HistoryData.saveHistoryToFile(historyData);
        historyData = CreatingServer.HistoryData.loadHistoryFromFile();
        logger.info("history data cleared");
    }

    /* ----------------- Event Listeners ----------------- */

    /**
     * Create server event listener
     */
    public static class createServer implements EventListener {
        @Override
        public void onEvent(Event event) {
            dispatcher.registerListener("server-send-msg", new serverSendMsg());
            if ("create-server".equals(event.getType())) {
                JSONObject json = new JSONObject(event.getData().toString());
                logger.info("creating server: " + json);
                String port = json.get("port").toString();
                String username = json.get("user").toString();
                new Thread(new CreatingServer(port, username)).start();
                logger.info("server thread created");
                clearHistoryData();
                dispatcher.registerListener("create-server-success", new EventListener() {
                    @Override
                    public void onEvent(Event event1) {
                        if (event1.getType().equals("create-server-success")) {
                            event.getCallback().call("success");
                        }
                        dispatcher.unregisterListener("create-server-success", this);
                    }
                });
            }
        }
    }

    /**
     * Get history data event listener
     */
    public static class getHistoryData implements EventListener {
        @Override
        public void onEvent(Event event) {
            if ("get-history-data".equals(event.getType())) {
                logger.info("get history data from server");
                sendRequestToServer("getMsgHistory", new JSONObject());
                dispatcher.registerListener("history-data", new EventListener() {
                    @Override
                    public void onEvent(Event event1) {
                        if (event1.getType().equals("history-data")) {
                            historyData = new JSONArray(event1.getData().toString());
                            event.getCallback().call(event1.getData().toString());
                        }
                        dispatcher.unregisterListener("history-data", this);
                    }
                });
            }
        }
    }

    /**
     * Get server info event listener
     */
    public static class getServerInfo implements EventListener {
        @Override
        public void onEvent(Event event) {
            logger.info("get server info");
            if ("get-server-info".equals(event.getType())) {
                if (Render.isServerMode()) {
                    if (!CreatingServer.isRunning())
                        event.getCallback().call(new JSONObject().put("userNumber", -1).toString());
                    event.getCallback().call(
                            new JSONObject().put("userNumber", CreatingServer.UserData.getUserNumber()).toString()
                    );
                    return;
                }
                if (!Connection.connected) {
                    event.getCallback().call(new JSONObject().put("userNumber", -1).toString());
                    return;
                }
                sendRequestToServer("getServerInfo", new JSONObject());
                if (dispatcher.hasListener("server-info")) return;
                dispatcher.registerListener("server-info", event1 -> {
                    if (event1.getType().equals("server-info")) {
                        event.getCallback().call(event1.getData().toString());
                    }
                });
            }
        }
    }

    /**
     * Leave room event listener
     */
    public static class leaveRoom implements EventListener {
        @Override
        public void onEvent(Event event) {
            if(!Connection.connected && !Render.isServerMode())
                return;
            if ("leave-room".equals(event.getType())) {
                try {
                    onClose.shutdownConnection();
                    if (Render.isServerMode()) {
                        CreatingServer.HistoryData.saveHistoryToFile();
                        dispatcher.dispatchEvent(new Event("server-close", "close"));
                    } else {
                        CreatingServer.HistoryData.saveHistoryToFile(historyData);
                    }
                } catch (IOException e) {
                    logger.error("failed to close socket: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Server send message event listener
     */
    public static class serverSendMsg implements EventListener {
        @Override
        public void onEvent(Event event) {
            if ("server-send-msg".equals(event.getType())) {
                if (!Render.isServerMode()) {
                    logger.error("not server mode, but server send msg");
                }
                logger.info("server send msg: " + event.getData().toString());
                JSONObject json = new JSONObject(event.getData().toString());
                String user = json.get("user").toString();
                String message = json.get("message").toString();
                CreatingServer.serverSendMsg(message, user);
            }
        }
    }

    /**
     * Load local chat record event listener
     */
    public static class loadLocalChatRecord implements EventListener {
        @Override
        public void onEvent(Event event) {
            if ("load-local-chat-record".equals(event.getType())) {
                // read local chat record
                logger.info("load local chat record");
                historyData = CreatingServer.HistoryData.loadHistoryFromFile();
                event.getCallback().call(historyData.toString());
            }
        }
    }

    /**
     * Send message event listener
     */
    private static class sendMessage implements EventListener {  // 创建事件监听器并注册事件监听器
        @Override
        public void onEvent(Event event) {
            if ("send-message".equals(event.getType())) {
                // 处理事件
                logger.info("send-message, data: " + event.getData().toString());
                sendRequestToServer("sendMsg", new JSONObject(event.getData().toString()));
                addHistoryMsg(Render.user, new JSONObject(event.getData().toString()).get("message").toString(), LocalTime.now().toString(), ChatPage.messageCount);
            }
        }
    }

    public static class onClose implements EventListener {
        @Override
        public void onEvent(Event event) {
            if ("on-close".equals(event.getType())) {
                try {
                    shutdownConnection();
                } catch (IOException e) {
                    logger.error("failed to close socket: " + e.getMessage());
                }
            }
        }

        private static void shutdownConnection() throws IOException {
            if (Connection.socket != null) {
                sendRequestToServer("close", new JSONObject());
                Connection.socket.shutdownInput();
                Connection.socket.shutdownOutput();
                Connection.socket.close();
                Connection.connected = false;
                logger.info("socket closed");
            }
        }
    }

    private static class login implements EventListener {
        @Override
        public void onEvent(Event event) {
            if ("login".equals(event.getType())) {
                // 处理事件
                // 可以通过回调回复消息
                logger.info("login");
                JSONObject json = new JSONObject(event.getData().toString());
                String address = json.get("address").toString();
                String port = json.get("port").toString();
                String user = json.get("user").toString();
                try {
                    logger.info("connecting");
                    Socket socket = new Socket(address, Integer.parseInt(port));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    try {
                        Main.Connection.setConnection(socket, out, in, address, port);
                        Connection.connected = true;
                        logger.info("connected");
                    } catch (Exception e) {
                        logger.error("Error: " + e.getMessage());
                        event.getCallback().call("connection failed, try again. Error: " + e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    new handleFromServer().startHandling();
                    dispatcher.registerListener("login-success", new EventListener() {
                        @Override
                        public void onEvent(Event event1) {
                            if (event1.getType().equals("login-success") && event1.getData().toString().equals("success")) {
                                clearHistoryData();
                                Render.setUser(user);
                                Render.getHistoryData();
                                event.getCallback().call("success");
                            } else {
                                event.getCallback().call(event1.getData().toString());
                            }
                            dispatcher.unregisterListener("login-success", this);
                        }
                    });
                    sendRequestToServer("login", new JSONObject().put("user", user));

                } catch (Exception e) {
                    logger.error("failed to login: " + e.getMessage());
                    event.getCallback().call("login failed, duplicate user name. Error: " + e);
                }
            }
        }
    }

    private static class handleServerRequest implements EventListener {
        @Override
        public void onEvent(Event event) {
            if ("server-request".equals(event.getType())) {
                JSONObject json = new JSONObject(event.getData().toString());
                String api;
                try {
                    api = json.get("api").toString();
                    logger.info("Server request api: " + api);
                } catch (JSONException e) {
                    logger.error("Unknown api: " + json + ", " + e.getMessage());
                    api = "unknown";
                }
                switch (api) {
                    case "receiveMsg": {
                        JSONObject data = new JSONObject(json.get("data").toString());
                        logger.info(data.toString());
                        Render.receiveMsg(data.get("message").toString(), data.get("user").toString(), data.get("time").toString(), Integer.parseInt(data.get("count").toString()));
                        addHistoryMsg(data.get("user").toString(), data.get("message").toString(), data.get("time").toString(), Integer.parseInt(data.get("count").toString()));
                        break;
                    }
                    case "loginResult": {
                        if (json.get("status").equals("success")) {
                            dispatcher.dispatchEvent(new Event("login-success", "success"));
                        } else {
                            dispatcher.dispatchEvent(new Event("login-success", json.get("status")));
                        }
                        break;
                    }
                    case "historyData": {
                        if (json.get("status").equals("success")) {
                            JSONArray data = json.getJSONArray("data");
                            dispatcher.dispatchEvent(new Event("history-data", data));
                        } else {
                            logger.error("failed to get history data: " + json.get("status"));
                        }
                        break;
                    }
                    case "serverInfo": {
                        if (json.get("status").equals("success")) {
                            JSONObject data = json.getJSONObject("data");
                            dispatcher.dispatchEvent(new Event("server-info", data));
                        } else {
                            logger.error("failed to get server info: " + json.get("status"));
                        }
                        break;
                    }
                    case "serverClose": {
                        Render.leaveRoom();
                        break;
                    }
                    default: {
                        logger.error("Unknown api: " + json);
                        break;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (Render.isServerMode()) {
                CreatingServer.HistoryData.saveHistoryToFile();
            } else {
                CreatingServer.HistoryData.saveHistoryToFile(historyData);
            }
        }));
        historyData = CreatingServer.HistoryData.loadHistoryFromFile();
        dispatcher.registerListener("load-local-chat-record", new loadLocalChatRecord());
        dispatcher.registerListener("leave-room", new leaveRoom());
        dispatcher.registerListener("get-server-info", new getServerInfo());
        dispatcher.registerListener("get-history-data", new getHistoryData());
        dispatcher.registerListener("on-close", new onClose());
        dispatcher.registerListener("send-message", new sendMessage());
        dispatcher.registerListener("server-request", new handleServerRequest());
        dispatcher.registerListener("login", new login());
        dispatcher.registerListener("create-server", new createServer());
        // Initialize the renderer
        logger.info("Render started");
        Render.setIPC(ipc);
        Render.launch(Render.class, args);

    }

}
