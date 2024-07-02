package com.zyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;
import java.util.*;
import java.time.*;

public class CreatingServer extends Thread {
    private static final Logger logger = LogManager.getLogger(CreatingServer.class);
    private static int port;
    private static volatile boolean running = true; // 添加一个控制变量
    private static ExecutorService threadPool;
    private static ServerSocket serverSocket;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private static final CopyOnWriteArrayList<PrintWriter> printWriters = new CopyOnWriteArrayList<>();  // Store all client output streams

    /**
     * Create a server
     *
     * @param p        port
     * @param username username
     */
    public CreatingServer(String p, String username) {
        port = Integer.parseInt(p);
        RoomControl.createdRoom(username);
    }

    public static boolean isRunning() {
        return running;
    }

    /**
     * Thread to run the server
     */
    public void run() {
        // Clear history data
        HistoryData.clearHistoryData();
        // Save history data to file when server is closed
        Runtime.getRuntime().addShutdownHook(new Thread(HistoryData::saveHistoryToFile));
        // Load history data from file to clear history data
        HistoryData.loadHistoryFromFile();
        threadPool = Executors.newFixedThreadPool(10); // Create a thread pool
        try {
            serverSocket = new ServerSocket(port);
            Render.serverMode = true;
            Main.dispatcher.dispatchEvent(new Event("create-server-success", "success"));
            if (!Main.dispatcher.hasListener("server-close"))
                Main.dispatcher.registerListener("server-close", event -> onClose());
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Incoming client connected.");
                    // Use thread pool to handle client requests
                    threadPool.execute(new ClientHandler(clientSocket));
                } catch (SocketException e) {
                    logger.info("Server socket closed");
                }
            }
        } catch (Exception e) {
            logger.error("Creating server error, port occupied or other error.");
            ConnectPage.showError("Creating server error, port occupied or other error.");
            onClose(); // Close thread pool
        }
    }

    /**
     * Send message to all clients, used by server mode
     *
     * @param message  message
     * @param username username
     */
    public static void serverSendMsg(String message, String username) {
        JSONObject singleData = new JSONObject();
        singleData.put("user", username);
        singleData.put("message", message);
        singleData.put("count", UserData.getMsgCount(username));
        singleData.put("time", LocalTime.now().toString());
        HistoryData.addHistoryData(singleData);
        RoomControl.newMsgFrom(username, message);
        UserData.addMsgCount(username);
    }

    /**
     * Close the whole server
     */
    public static void onClose() {
        logger.warn("Server closing, the number of client: " + printWriters.size());
        running = false;
        // Close all client connections
        for (PrintWriter pw : printWriters) {
            pw.println("{\"api\":\"serverClose\"}");
            pw.close();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }
        if (threadPool != null) {
            threadPool.shutdown(); // Try to close the thread pool
        }
        logger.warn("Server closed");
    }

    /**
     * ClientHandler Thread
     */
    class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private Thread handlingThread;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        /**
         * Check if the client is still connected
         *
         * @param out output stream
         */
        private void checkClientConnection(PrintWriter out) {
            // Check if the client is still connected
            if (clientSocket.isClosed() || !clientSocket.isConnected() || out.checkError()) {
                // If the client is disconnected, remove the output stream from the collection
                logger.info("Client disconnected, user: " +
                        (UserData.isUserExist(out) ? UserData.getUser(out) : "unknown"));
                if (UserData.isUserExist(out)) {
                    UserData.removeUser(out);
                }
                handlingThread.interrupt();
                // Stop the scheduled task
                scheduler.shutdown();
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                // Add the output stream to the collection
                printWriters.add(out);
                // Start a new thread to handle client requests
                handlingThread = new Thread(new handlingClientRequest(in, out));
                handlingThread.start();
                // Check if the client is still connected
                scheduler.scheduleAtFixedRate(() -> checkClientConnection(out), 0, 1, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error("Client connection lost: " + e.getMessage());
                // Close the client connection
                scheduler.shutdown();
                try {
                    clientSocket.close();
                } catch (IOException e2) {
                    logger.error("Close client connection error: " + e.getMessage());
                }
            }
        }

        static class handlingClientRequest extends Thread {
            BufferedReader in;
            PrintWriter out;

            public handlingClientRequest(BufferedReader in, PrintWriter out) {
                logger.info("Start handling client request");
                this.in = in;
                this.out = out;
            }

            /**
             * Thread to handle client requests
             */
            @Override
            public void run() {
                String inputLine;
                while (running) {
                    try {
                        inputLine = in.readLine();
                    } catch (IOException e) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            logger.error("Thread interrupted: " + ex.getMessage());
                        }
                        logger.error("Client request error: " + e.getMessage());
                        break;
                    }
                    if (inputLine == null) {
                        continue;
                    }
                    handlingClientRequest(inputLine, out);
                }
            }
        }

        /**
         * Handle client requests
         *
         * @param inputLine input line
         * @param out       output stream
         */
        public static void handlingClientRequest(String inputLine, PrintWriter out) {
            JSONObject json = new JSONObject(inputLine);
            String api = (String) json.get("api");
            switch (api) {
                case "sendMsg": {
                    /*
                     * Send message
                     * data: {
                     *     'api': 'sendMsg',
                     *     'data': {
                     *         'message': 'message'
                     *     }
                     * }
                     */
                    JSONObject data = (JSONObject) json.get("data");
                    String msg = (String) data.get("message");
                    String user = UserData.getUser(out);
                    UserData.addMsgCount(user);
                    JSONObject singleData = new JSONObject();
                    singleData.put("user", user);
                    singleData.put("message", msg);
                    int counts = UserData.getMsgCount(user);
                    singleData.put("count", counts);
                    String time = LocalTime.now().toString();
                    singleData.put("time", time);
                    HistoryData.addHistoryData(singleData);
                    RoomControl.newMsgFrom(user, msg);
                    if (Render.isServerMode()) {
                        Render.receiveMsg(msg, user, time, counts);
                    }
                    break;
                }
                case "getMsgHistory": {
                    /*
                     * Get message history
                     * data: {
                     *     'api': 'getMsgHistory'
                     * }
                     */
                    JSONObject response = new JSONObject();
                    response.put("status", "success");
                    response.put("api", "historyData");
                    response.put("data", HistoryData.getHistoryData());
                    out.println(response);
                    logger.info("getMsgHistory: " + response);
                    break;
                }
                case "login": {
                    /*
                     * Login
                     * data: {
                     *     'api': 'login',
                     *     'data': {
                     *         'user': 'username'
                     *     }
                     * }
                     */
                    JSONObject data = (JSONObject) json.get("data");
                    String user = (String) data.get("user");
                    UserData.addUser(out, user);
                    JSONObject response = new JSONObject();
                    response.put("api", "loginResult");
                    response.put("status", "success");
                    out.println(response);
                    logger.info("login: " + response);
                    RoomControl.newUser(user);
                    break;
                }
                case "close": {
                    /*
                     * Close the connection
                     * data: {
                     *     'api': 'close'
                     * }
                     */
                    String user = UserData.getUser(out);
                    logger.info("User quited: " + user);
                    RoomControl.userLeft(user);
                    UserData.removeUser(out);
                    printWriters.remove(out);
                    Thread.currentThread().interrupt();
                    break;
                }
                case "getServerInfo": {
                    /*
                     * Get server info
                     * data: {
                     *     'api': 'getServerInfo'
                     * }
                     */
                    out.println(getResponse());
                    break;
                }
                default: {
                    logger.error("Unknown api: " + api);
                    break;
                }
            }
        }

        /**
         * Get server info
         *
         * @return server info
         */
        private static JSONObject getResponse() {
            JSONObject response = new JSONObject();
            response.put("api", "serverInfo");
            response.put("status", "success");
            JSONObject data = new JSONObject();
            data.put("userNumber", UserData.getUserNumber());
            data.put("host", Render.user);
            response.put("data", data);
            return response;
        }

        /**
         * Broadcast message to all clients except the specified client
         *
         * @param message message
         * @param except  except
         * @throws IOException IOException
         */
        public static void broadcastMessage(String message, String except) throws IOException {
            // Broadcast message to all clients except the specified client
            for (PrintWriter pw : printWriters) {
                if (pw == null || UserData.getUser(pw) == null) {
                    printWriters.remove(pw);
                    UserData.removeUser(pw);
                    continue;
                }
                if (Objects.equals(UserData.getUser(pw), except)) {
                    continue;
                }
                pw.println(message);
                logger.info("BroadcastMessage: " + message);
            }
        }
    }

    /**
     * History data class to manage history data
     */
    static class HistoryData {
        private static JSONArray historyData = new JSONArray();
        private static final Object lock = new Object();

        /**
         * Add history data
         *
         * @param singleData single data
         */
        public static void addHistoryData(JSONObject singleData) {
            synchronized (lock) {
                historyData.put(singleData);
            }
        }

        /**
         * Save history data to file
         */
        public static void saveHistoryToFile() {
            _saveHistoryToFile(historyData);
        }

        /**
         * Save history data to file
         *
         * @param data data
         */
        public static void saveHistoryToFile(JSONArray data) {
            _saveHistoryToFile(data);
        }

        /**
         * Save history data to file (path=./chat_history.json)
         *
         * @param historyData history data
         */
        private static void _saveHistoryToFile(JSONArray historyData) {
            String filePath = "./chat_history.json";
            synchronized (lock) {
                try (FileWriter file = new FileWriter(filePath)) {
                    file.write(historyData.toString(2));
                    file.flush();
                } catch (IOException e) {
                    logger.error("failed to save history data to file: " + e.getMessage());
                }
                logger.info("history data saved to file: " + filePath);
            }
        }

        /**
         * Load history data from file (path=./chat_history.json)
         *
         * @return history data
         */
        public static JSONArray loadHistoryFromFile() {
            String filePath = "./chat_history.json";
            synchronized (lock) {
                JSONArray chat_history;
                try (FileReader file = new FileReader(filePath)) {
                    chat_history = new JSONArray(new JSONTokener(file));
                    historyData = chat_history;
                    return chat_history;
                } catch (IOException e) {
                    logger.error("failed to load history data from file: " + e.getMessage());
                    return new JSONArray();
                } catch (JSONException e) {
                    logger.error("parse history data failed: " + e.getMessage());
                    return new JSONArray();
                }
            }
        }

        /**
         * Get history data
         *
         * @return history data
         */
        public static JSONArray getHistoryData() {
            return historyData;
        }

        /**
         * Clear history data
         */
        public static void clearHistoryData() {
            historyData = new JSONArray();
        }
    }

    /**
     * User data class to manage user data
     */
    static class UserData {
        private static final HashMap<PrintWriter, String> UserMap = new HashMap<>();

        /**
         * Add user
         *
         * @param pw       print writer
         * @param username username
         */
        public static void addUser(PrintWriter pw, String username) {
            UserMap.put(pw, username);
        }

        public static String getUser(PrintWriter pw) {
            if (!UserMap.containsKey(pw)) {
                return null;
            }
            return UserMap.get(pw);
        }

        private static final HashMap<String, Integer> MsgCount = new HashMap<>();

        public static void removeUser(PrintWriter pw) {
            UserMap.remove(pw);
        }

        public static void removeUser(String username) {
            for (PrintWriter pw : UserMap.keySet()) {
                if (UserMap.get(pw).equals(username)) {
                    UserMap.remove(pw);
                    break;
                }
            }
        }

        public static boolean isUserExist(String username) {
            for (PrintWriter pw : UserMap.keySet()) {
                if (UserMap.get(pw).equals(username)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get message count
         *
         * @param username username
         * @return message count
         */
        public static int getMsgCount(String username) {
            if (!MsgCount.containsKey(username)) {
                MsgCount.put(username, 1);
            }
            return MsgCount.get(username);
        }

        /**
         * Add message count
         *
         * @param username username
         */
        public static void addMsgCount(String username) {
            if (MsgCount.containsKey(username)) {
                MsgCount.put(username, MsgCount.get(username) + 1);
            } else {
                MsgCount.put(username, 1);
            }
        }

        public static boolean isUserExist(PrintWriter pw) {
            return UserMap.containsKey(pw);
        }

        public static int getUserNumber() {
            if (Render.isServerMode()) {
                return UserMap.size() + 1;
            }
            return UserMap.size();
        }
    }

    /**
     * Room control class to manage the chatroom
     */
    static class RoomControl {
        public static void newUser(String username) {
            // New user joined
            String time = LocalTime.now().toString();
            JSONObject data = new JSONObject();
            data.put("user", "system");
            data.put("message", username + " joined the room");
            data.put("time", time);
            data.put("count", 0);
            JSONObject response = new JSONObject();
            response.put("api", "receiveMsg");
            response.put("data", data);
            JSONObject singleData = new JSONObject();
            singleData.put("user", "system");
            singleData.put("message", username + " joined the room");
            singleData.put("count", 0);
            singleData.put("time", time);
            HistoryData.addHistoryData(singleData);
            if (Render.isServerMode()) {
                Render.receiveMsg(username + " joined the room", "system", time, 0);
            }
            try {
                ClientHandler.broadcastMessage(response.toString(), username);
            } catch (IOException e) {
                logger.error("Failed to broadcast message to all clients: " + e.getMessage());
            }
        }

        public static void userLeft(String username) {
            // User left
            JSONObject data = new JSONObject();
            data.put("user", "system");
            data.put("message", username + " left the room");
            data.put("time", LocalTime.now().toString());
            data.put("count", 0);
            JSONObject response = new JSONObject();
            response.put("api", "receiveMsg");
            response.put("data", data);
            JSONObject singleData = new JSONObject();
            singleData.put("user", "system");
            singleData.put("message", username + " left the room");
            singleData.put("count", 0);
            String time = LocalTime.now().toString();
            singleData.put("time", time);
            HistoryData.addHistoryData(singleData);
            if (Render.isServerMode()) {
                Render.receiveMsg(username + " left the room", "system", time, 0);
            }
            try {
                ClientHandler.broadcastMessage(response.toString(), username);
            } catch (IOException e) {
                logger.error("failed to borcast message to all clients: " + e.getMessage());
            }
        }

        public static void newMsgFrom(String username, String msg) {
            // New message from user
            JSONObject data = new JSONObject();
            data.put("user", username);
            data.put("message", msg);
            data.put("time", LocalTime.now().toString());
            data.put("count", UserData.getMsgCount(username));
            JSONObject response = new JSONObject();
            response.put("api", "receiveMsg");
            response.put("data", data);
            try {
                ClientHandler.broadcastMessage(response.toString(), username);
            } catch (IOException e) {
                logger.error("broadcast message failed: " + e.getMessage());
            }
        }

        public static void createdRoom(String username) {
            // Room created
            logger.info("room created by: " + username);
            JSONObject singleData = new JSONObject();
            singleData.put("user", "system");
            singleData.put("message", username + " created the room");
            singleData.put("count", 0);
            String time = LocalTime.now().toString();
            singleData.put("time", time);
            HistoryData.addHistoryData(singleData);
            if (Render.isServerMode()) {
                Render.receiveMsg(username + " created the room", "system", time, 0);
            }
        }
    }
}
