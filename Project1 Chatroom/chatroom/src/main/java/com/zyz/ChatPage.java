package com.zyz;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.kordamp.bootstrapfx.scene.layout.Panel;

import java.time.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.*;
import java.util.function.Consumer;


public class ChatPage implements PageInterface {
    private static final Logger logger = LogManager.getLogger(ChatPage.class);
    private static VBox chatHistory;  // VBox to hold all the messages
    private static ScrollPane scrollPane;  // ScrollPane to hold the chat history
    private static TextField chatInput;  // TextField for user input
    private final Panel panel;
    private static final Queue<JSONObject> tempQueue = new LinkedList<>();
    // Temporarily store messages before initialization
    private static final Stack<JSONObject> historyMsg = new Stack<>();  // Store history messages
    public static int messageCount = 1;  // The count of messages sent by the user
    private static String username;  // The username of the current user
    private static Label header;
    private TextField searchInput;
    private static final Lock lock = new ReentrantLock();
    public static boolean historyMode = false;  // Whether the chat page is in history mode (reading local chat record)
    private static final ArrayList<String> colors = new ArrayList<>(Arrays.asList(
            "#FFE5E5", "#AC87C5", "#87556F", "#78C4D4", "#F8E796", "#F9B5D0", "#F99157", "#F95A6A", "#F92672",
            "#B399D4", "#7FB3D5", "#59C3C3", "#36D7B7", "#2EBEB1", "#27AE60", "#2ECC71", "#229954", "#9B59B6",
            "#8E44AD", "#8E44AD", "#F1C40F", "#F39C12", "#E67E22", "#D35400", "#E74C3C", "#C0392B", "#ECF0F1",
            "#95A5A6", "#7F8C8D", "#607D8B", "#546E7A", "#454954", "#34495E", "#2C3E50", "#2980B9", "#2980B9"
    ));  // Store colors for user avatars
    private static final HashMap<String, String> userColor = new HashMap<>();  // Store the color for each user
    private static Label roomInfo;  // Label to show the server info (host and port)

    /**
     * ChatPage constructor
     */
    public ChatPage() {
        panel = new Panel("Chatroom");
        if (!panel.getStyleClass().contains("panel-primary"))
            panel.getStyleClass().add("panel-primary");
        header = new Label("Chatroom");
        header.getStyleClass().addAll("h3");
        panel.setHeading(header);
    }

    /**
     * Receive message from server
     *
     * @param message message content
     * @param user    user name
     * @param time    message time
     * @param count   message count
     */
    public static void receiveMsg(String message, String user, String time, int count) {
        // If chatHistory is not initialized, temporarily store the message
        if (chatHistory == null) {
            logger.info("Received message before initialization");
            JSONObject json = new JSONObject();
            json.put("message", message);
            json.put("user", user);
            json.put("time", time);
            json.put("count", count);
            tempQueue.add(json);
            return;
        }
        try {
            if (Platform.isFxApplicationThread()) {
                toggleReceiveMsg(message, user, time, count);
            } else {
                Platform.runLater(() -> toggleReceiveMsg(message, user, time, count));
            }
        } catch (Exception e) {
            logger.error("Error receiving message: " + e.getMessage());
        }
    }

    /**
     * Toggle the incoming message display (excluding the current user's message)
     *
     * @param message message content
     * @param user    user name
     * @param time    message time
     * @param count   message count
     */
    private static void toggleReceiveMsg(String message, String user, String time, int count) {
        logger.info("Received message from " + user + ": " + message);
        if ("system".equals(user))
            chatHistory.getChildren().add(systemBubble(message, time));
        else {
            chatHistory.getChildren().add(chatBubbleOthers(message, user, time, count));
        }
        scrollToBottom();
        chatInput.requestFocus(); // 输入框获取焦点
    }

    /**
     * Method to create a chat bubble for the other users
     *
     * @param message message content
     * @param user    user name
     * @param time    message time
     * @param count   message count
     * @return chat bubble for the other users
     */
    public static VBox chatBubbleOthers(String message, String user, String time, int count) {
        Label chatBubbleText = new Label(message);
        chatBubbleText.setWrapText(true);  // Wrap text within the label
        chatBubbleText.setMaxWidth(300);  // Maximum width for the chat bubble

        // Set user label and chat bubble styles
        Label chatBubbleUser = new Label(String.valueOf(user.charAt(0)));
        chatBubbleUser.setAlignment(Pos.CENTER);
        chatBubbleUser.getStyleClass().addAll("chat-bubble-user", "h4");
        chatBubbleUser.setPrefHeight(30);
        chatBubbleUser.setPrefWidth(30);
        chatBubbleUser.setTextFill(Color.WHITE);

        // Set user avatar color
        Label chatBubbleUsername = new Label(user);
        chatBubbleUsername.setMaxWidth(0);
        chatBubbleUsername.setMinWidth(0);
        chatBubbleUsername.setPrefWidth(0);
        chatBubbleUsername.setVisible(false);

        if (colors.isEmpty()) {
            colors.addAll(Arrays.asList(
                    "#FFE5E5", "#AC87C5", "#87556F", "#78C4D4", "#F8E796", "#F9B5D0", "#F99157", "#F95A6A", "#F92672",
                    "#B399D4", "#7FB3D5", "#59C3C3", "#36D7B7", "#2EBEB1", "#27AE60", "#2ECC71", "#229954", "#9B59B6",
                    "#8E44AD", "#8E44AD", "#F1C40F", "#F39C12", "#E67E22", "#D35400", "#E74C3C", "#C0392B", "#ECF0F1",
                    "#95A5A6", "#7F8C8D", "#607D8B", "#546E7A", "#454954", "#34495E", "#2C3E50", "#2980B9", "#2980B9"
            ));
        }
        int random = (int) (Math.random() * 1000) % colors.size();
        String color;
        if (userColor.containsKey(user)) {
            color = userColor.get(user);
        } else {
            color = colors.get(random);
            userColor.put(user, color);
            colors.remove(random);
        }

        // Set tooltip for the user avatar to show the full name
        Tooltip tooltip = new Tooltip(user);
        Tooltip.install(chatBubbleUser, tooltip);

        // Set chat bubble styles
        chatBubbleUser.setBackground(new Background(
                new BackgroundFill(
                        Color.web(color),
                        new CornerRadii(15),
                        Insets.EMPTY)
                )
        );
        chatBubbleText.getStyleClass().addAll("chat-bubble-text", "h5");
        chatBubbleText.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(0, 0, 0, 0.1),
                        new CornerRadii(5), Insets.EMPTY)
                )
        );
        chatBubbleText.setPadding(new Insets(10));

        // Align to the left
        HBox chatBubble = new HBox(5, chatBubbleUser, chatBubbleText, chatBubbleUsername);
        // 5 is the spacing between user label and text
        chatBubble.getStyleClass().addAll("chat-bubble", "chat-bubble-left");
        chatBubble.setAlignment(Pos.CENTER_LEFT);

        // Set time label
        Label timeLabel = new Label(!time.contains(".") ? time : time.split("\\.")[0]);
        timeLabel.getStyleClass().addAll("chat-bubble-time", "h6");
        timeLabel.setAlignment(Pos.CENTER_LEFT);
        timeLabel.setTextFill(Color.rgb(0, 0, 0, 0.5));

        // Set count label
        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().addAll("chat-bubble-count", "h6");
        countLabel.setAlignment(Pos.CENTER_LEFT);
        countLabel.setTextFill(Color.rgb(0, 0, 0, 0.5));

        // Align to the left
        VBox chatBubbleVBox = new VBox(5, chatBubble, timeLabel, countLabel);
        chatBubbleVBox.setAlignment(Pos.CENTER_LEFT);
        return chatBubbleVBox;
    }

    /**
     * Load history data
     *
     * @param historyData history data
     */
    public static void loadHistoryData(JSONArray historyData) {
        logger.info("Loading history data: " + historyData.toString());
        for (int i = 0; i < historyData.length(); i++) {
            JSONObject data = historyData.getJSONObject(i);
            if (!historyMsg.contains(data)) {
                historyMsg.add(data);
            }
        }
        Platform.runLater(() -> {
            logger.info("Attempting to load history messages again");
            if (historyMsg.isEmpty()) {
                logger.info("History message is empty");
            }
            while (!historyMsg.isEmpty()) {
                if (chatHistory == null) {
                    break;
                }
                JSONObject json = historyMsg.pop();
                if (json.get("user").toString().equals("system"))
                    chatHistory.getChildren().add(
                            0,
                            systemBubble(json.get("message").toString(), json.get("time").toString())
                    );
                else if (!json.get("user").toString().equals(username))
                    chatHistory.getChildren().add(
                            0,
                            chatBubbleOthers(
                                    json.get("message").toString(),
                                    json.get("user").toString(),
                                    json.get("time").toString(),
                                    Integer.parseInt(json.get("count").toString())
                            )
                    );
                else {
                    chatHistory.getChildren().add(
                            0,
                            chatBubbleSelf(
                                    json.get("message").toString(),
                                    json.get("time").toString(),
                                    Integer.parseInt(json.get("count").toString())
                            )
                    );
                    messageCount++;
                }
            }
        });
    }

    /**
     * Set the current user
     *
     * @param user user name
     */
    public static void setUser(String user) {
        username = user;
        logger.info("set user " + user);
    }

    /**
     * Send message to server
     *
     * @param message message content
     * @param time    message time
     * @param count   message count
     */
    public static VBox chatBubbleSelf(String message, String time, int count) {
        // Set chat bubble styles
        Label chatBubbleText = new Label(message);
        chatBubbleText.setWrapText(true); // Wrap text within the label
        chatBubbleText.setMaxWidth(300); // Maximum width for the chat bubble

        // Set user label and chat bubble styles
        Label chatBubbleUser = new Label("Me");
        chatBubbleUser.setTextFill(Color.WHITE);
        chatBubbleUser.setAlignment(Pos.CENTER);
        chatBubbleUser.getStyleClass().addAll("chat-bubble-user", "h4");
        chatBubbleUser.setPrefHeight(30);
        chatBubbleUser.setPrefWidth(30);
        chatBubbleUser.setBackground(new Background(
                new BackgroundFill(Color.web("#94ED88"), new CornerRadii(15), Insets.EMPTY)));

        chatBubbleText.getStyleClass().addAll("chat-bubble-text", "h5");
        chatBubbleText.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(0, 0, 0, 0.1),
                        new CornerRadii(5), Insets.EMPTY)
                )
        );
        chatBubbleText.setPadding(new Insets(10));

        // Set user avatar color
        Label chatBubbleUsername = new Label(username);
        chatBubbleUsername.setMaxWidth(0);
        chatBubbleUsername.setMinWidth(0);
        chatBubbleUsername.setPrefWidth(0);
        chatBubbleUsername.setVisible(false);

        // Set tooltip for the user avatar to show the full name
        Tooltip tooltip = new Tooltip(username);
        Tooltip.install(chatBubbleUser, tooltip);

        // Align to the right
        HBox chatBubble = new HBox(5, chatBubbleText, chatBubbleUser, chatBubbleUsername);
        // 5 is the spacing between user label and text
        chatBubble.setAlignment(Pos.CENTER_RIGHT);
        chatBubble.getStyleClass().addAll("chat-bubble", "chat-bubble-right");

        // Set time label
        Label timeLabel = new Label(!time.contains(".") ? time : time.split("\\.")[0]);
        timeLabel.getStyleClass().addAll("chat-bubble-time", "h6");
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        timeLabel.setTextFill(Color.rgb(0, 0, 0, 0.5));

        // Set count label
        Label countLabel = new Label(String.valueOf(count));
        countLabel.getStyleClass().addAll("chat-bubble-count", "h6");
        countLabel.setAlignment(Pos.CENTER_RIGHT);
        countLabel.setTextFill(Color.rgb(0, 0, 0, 0.5));

        // Align to the right
        VBox chatBubbleVBox = new VBox(5, chatBubble, timeLabel, countLabel);
        chatBubbleVBox.setAlignment(Pos.CENTER_RIGHT);
        return chatBubbleVBox;
    }

    public static VBox systemBubble(String text, String time) {
        // Set chat bubble styles
        Label chatBubbleText = new Label(text);
        chatBubbleText.setWrapText(true); // Wrap text within the label
        chatBubbleText.setMinWidth(200); // Minimum width for the chat bubble
        chatBubbleText.setMaxWidth(300); // Maximum width for the chat bubble
        chatBubbleText.getStyleClass().addAll("chat-bubble-text", "h5");
        chatBubbleText.setAlignment(Pos.CENTER);
        chatBubbleText.setPadding(new Insets(10));

        // Align to the center
        Label timeLabel = new Label(!time.contains(".") ? time : time.split("\\.")[0]);
        timeLabel.getStyleClass().addAll("chat-bubble-time", "h6");
        timeLabel.setAlignment(Pos.CENTER);
        timeLabel.setTextFill(Color.rgb(0, 0, 0, 0.5));

        // Align to the center
        VBox chatBubbleVBox = new VBox(5, chatBubbleText, timeLabel);
        chatBubbleVBox.setAlignment(Pos.CENTER);
        chatBubbleVBox.getStyleClass().addAll("chat-bubble", "chat-bubble-right");
        chatBubbleVBox.setBackground(new Background(
                        new BackgroundFill(
                                Color.rgb(0, 0, 0, 0.1),
                                new CornerRadii(5), Insets.EMPTY)
                )
        );
        return chatBubbleVBox;
    }

    /**
     * Update server info
     *
     * @param headerText header text
     * @param host       host
     */
    public static void updateServerInfo(String headerText, String host) {
        Platform.runLater(() -> {
            if (header != null) {
                header.setText("Chatroom Client " + headerText);
                roomInfo.setText("Host: " + host);
            } else {
                logger.error("Label header is null, but update server info");
            }
        });
    }

    /**
     * Render the chat page
     *
     * @param primaryStage primary stage
     */
    public void render(Stage primaryStage) {
        // Create a VBox to hold all the messages
        chatHistory = new VBox(20); // 10 is the spacing between chat bubbles
        chatHistory.setFillWidth(true); // Fill the width with the VBox
        chatHistory.setPadding(new Insets(30));

        // Attempt to load history messages
        try {
            logger.info("Attempt to load history messages");
            while (!historyMsg.isEmpty()) {
                JSONObject json = historyMsg.pop();
                if (json.get("user").toString().equals("system"))
                    chatHistory.getChildren().add(
                            0,
                            systemBubble(json.get("message").toString(), json.get("time").toString()));
                else if (!json.get("user").toString().equals(username))
                    chatHistory.getChildren().add(
                            0,
                            chatBubbleOthers(
                                    json.get("message").toString(),
                                    json.get("user").toString(),
                                    json.get("time").toString(),
                                    Integer.parseInt(json.get("count").toString())
                            ));
                else {
                    chatHistory.getChildren().add(
                            0,
                            chatBubbleSelf(
                                    json.get("message").toString(),
                                    json.get("time").toString(),
                                    Integer.parseInt(json.get("count").toString())
                            ));
                }
            }
        } catch (JSONException e) {
            logger.error("Error loading history messages: " + e.getMessage());
        }

        // Segment the chat history with a label
        Label historyLabel = new Label("-----------------History Messages Above-----------------");
        historyLabel.getStyleClass().addAll("chat-bubble-time", "h6");
        historyLabel.setAlignment(Pos.CENTER);
        historyLabel.setPadding(new Insets(10));
        historyLabel.setTextFill(Color.rgb(0, 0, 0, 0.5));
        HBox historyLabelHBox = new HBox(5, historyLabel);
        historyLabelHBox.setAlignment(Pos.CENTER);
        chatHistory.getChildren().add(historyLabelHBox);

        // Create a scroll pane to hold the chat history
        scrollPane = new ScrollPane(chatHistory);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Create an input box for the user to type messages
        chatInput = new TextField();
        chatInput.setPrefHeight(50);
        chatInput.getStyleClass().addAll("form-control", "h4");
        chatInput.setPromptText("在此输入聊天内容...");

        // Create a send button to send messages
        Button sendButton = new Button("发送");
        sendButton.setPrefHeight(50);
        sendButton.setPrefWidth(200);
        sendButton.getStyleClass().setAll("btn", "btn-primary", "btn-success", "h3");
        sendButton.setAlignment(Pos.CENTER);
        sendButton.setDefaultButton(true); // Set the send button as the default button

        // Set the send button action
        sendButton.setOnAction(event -> {
            String message = chatInput.getText(); // Get the message from the input box
            if (!message.isEmpty()) {
                chatInput.clear(); // Clear the input box
                Render.sendMsg(message);
                chatHistory.getChildren().add(chatBubbleSelf(
                        message,
                        LocalTime.now().toString(), messageCount
                )); // 将消息追加到聊天历史
                messageCount++;
                scrollToBottom();
            }
            chatInput.requestFocus(); // Input box gets focus
        });

        // Create an input box and a send button
        HBox inputBox = new HBox(10, chatInput, sendButton);
        HBox.setHgrow(chatInput, Priority.ALWAYS); // Set the input box to grow horizontally

        // Create a BorderPane to hold the chat history and the input box
        BorderPane chatPane = new BorderPane();
        chatPane.setCenter(scrollPane);
        chatPane.setBottom(inputBox);

        // Create an expandable pane to hold the room info, leave room button, and search button
        TitledPane expandablePane = new TitledPane();
        expandablePane.setAnimated(true);
        VBox expandableContent = new VBox(10);
        expandablePane.setText("Show More");
        expandablePane.setExpanded(false);

        roomInfo = new Label("Host: --");
        roomInfo.setVisible(!Render.isServerMode());
        roomInfo.getStyleClass().addAll("h5");

        // Create a leave room button
        Button leaveRoomButton = new Button("Leave Room");
        leaveRoomButton.setOnAction(event -> {
            logger.info("Leaving room...");
            Render.leaveRoom();
        });

        // Create a search button and a search input box
        Button searchButton = new Button("Search");
        searchInput = new TextField(); // 搜索输入框
        searchInput.setPromptText("Search messages or users...");
        searchInput.setVisible(false); // 默认不显示
        searchButton.setOnAction(event -> {
            searchInput.setVisible(!searchInput.isVisible()); // Show or hide the search input box
            if (searchInput.isVisible()) {
                searchButton.setText("Cancel Search");
                logger.info("Start searching");
            } else {
                searchButton.setText("Search");
                logger.info("End searching");
                Platform.runLater(() -> searchFunction(false));
            }
        });

        // Set the search input action
        searchInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                // Search input is not empty, start searching
                logger.info("Searching: " + newValue);
                Platform.runLater(() -> searchFunction(true));
            } else {
                // Search input is empty, restore chat history
                logger.info("Search input is empty, restore chat history");
                Platform.runLater(() -> searchFunction(false));
            }
        });

        // Add the room info, leave room button, search button, and search input box to the expandable pane
        expandableContent.getChildren().addAll(roomInfo, leaveRoomButton, searchButton, searchInput);
        expandablePane.setContent(expandableContent);

        // Add the expandable pane to the top of the chat pane
        VBox topContainer = new VBox(expandablePane);
        chatPane.setTop(topContainer);

        // Set the leave room button and search button styles
        if (historyMode) {
            logger.info("Currently in history mode");
            inputBox.setVisible(false);
            inputBox.setManaged(false);
        }

        // Create a panel to hold the chat pane
        panel.setBody(chatPane);
        panel.setPadding(new Insets(0));
        Scene scene = new Scene(panel, 1024, 720);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        primaryStage.setScene(scene);
        primaryStage.show();
        logger.info("show chat page");
        while (!tempQueue.isEmpty()) {
            JSONObject json = tempQueue.poll();
            receiveMsg(
                    json.get("message").toString(),
                    json.get("user").toString(),
                    json.get("time").toString(),
                    Integer.parseInt(json.get("count").toString())
            );
        }
    }

    /**
     * Search function
     *
     * @param enable enable search
     */
    private void searchFunction(boolean enable) {
        lock.lock();
        try {
            // Define a common action to reset visibility and managed status
            Consumer<Node> resetVisibility = node -> {
                node.setVisible(true);
                node.setManaged(true);
            };

            if (!enable) {
                // When not in search mode, make all messages visible and managed
                chatHistory.getChildren().forEach(resetVisibility);
                scrollToBottom();
            } else {
                // In search mode
                String searchText = searchInput.getText().toLowerCase().trim();
                if (searchText.isEmpty()) {
                    // Treat empty search as normal mode, restoring all messages
                    chatHistory.getChildren().forEach(resetVisibility);
                } else {
                    // Only show and manage messages that match the search criteria
                    chatHistory.getChildren().forEach(node -> searchAndFilter(node, searchText));
                }
                scrollToTop();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Search and filter function
     *
     * @param node       node
     * @param searchText search text
     */
    private void searchAndFilter(Node node, String searchText) {
        if (node instanceof VBox vBox) {
            boolean matchFound = vBox.getChildren().stream()
                    .filter(HBox.class::isInstance)
                    .flatMap(hBox -> ((HBox) hBox).getChildren().stream())
                    .filter(Label.class::isInstance)
                    .map(Label.class::cast)
                    .anyMatch(label -> label.getText().toLowerCase().contains(searchText));

            // Set visibility and managed status based on search result
            node.setVisible(matchFound);
            node.setManaged(matchFound);
        }
    }

    /**
     * Scroll to the bottom of the chat history
     */
    private static void scrollToBottom() {
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(100),
                ae -> scrollPane.setVvalue(scrollPane.getVmax())
        ));
        timeline.play();
    }

    /**
     * Scroll to the top of the chat history
     */
    private static void scrollToTop() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), ae -> scrollPane.setVvalue(0)));
        timeline.play();
    }

}