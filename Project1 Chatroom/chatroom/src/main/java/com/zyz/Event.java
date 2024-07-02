package com.zyz;

import org.json.JSONObject;

// Event Class with Callback
public class Event {
    private final String type;
    private final Object data;
    private Callback callback; // Callback Interface

    public Event(String type, JSONObject data) {
        this.type = type;
        this.data = data;
        this.callback = null;
    }
    public Event(String type, Object data) {
        this.type = type;
        this.data = data;
        this.callback = null;
    }

    public Event(String type, Object data, Callback callback) {
        this.type = type;
        this.data = data;
        this.callback = callback;
    }

    // Getter and Setter
    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }
}
