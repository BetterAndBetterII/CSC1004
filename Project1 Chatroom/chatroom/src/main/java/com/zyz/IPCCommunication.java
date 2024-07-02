package com.zyz;

import org.json.JSONObject;

public class IPCCommunication {
    private final EventDispatcher dispatcher;

    public IPCCommunication(EventDispatcher dispatcher) {  // dispatcher Created in Main, passed to IPCCommunication
        this.dispatcher = dispatcher;
    }

    public void sendMessage(String message) {
        // Register the event listener
        Event event = new Event("send-message", message);
        dispatcher.dispatchEvent(event);
    }

    public void login(JSONObject data, Callback callback) {
        Event event = new Event("login", data, callback);
        dispatcher.dispatchEvent(event);
    }

    public void createServer(JSONObject data, Callback callback) {
        Event event = new Event("create-server", data, callback);
        dispatcher.dispatchEvent(event);
    }

    public void closeSocket() {
        Event event = new Event("on-close", null);
        dispatcher.dispatchEvent(event);
    }

    public void getHistoryData(JSONObject data, Callback callback) {
        Event event = new Event("get-history-data", null, callback);
        dispatcher.dispatchEvent(event);
    }

    public void getServerInfo(Callback callback) {
        Event event = new Event("get-server-info", null, callback);
        dispatcher.dispatchEvent(event);
    }

    public void serverSendMsg(String message) {
        Event event = new Event("server-send-msg", message);
        dispatcher.dispatchEvent(event);
    }

    public void leaveRoom(Object data) {
        Event event = new Event("leave-room", data);
        dispatcher.dispatchEvent(event);
    }

    public void loadLocalChatRecord(Object data, Callback callback) {
        Event event = new Event("load-local-chat-record", data, callback);
        dispatcher.dispatchEvent(event);
    }
}
