package org.zyz;

import org.json.JSONObject;

// 事件类，支持传输数据/回调函数
public class Event {
    private String type;
    private Object data;
    private Callback callback; // 添加回调字段

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

    // Getter 和 Setter
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    // 其他代码...
}
