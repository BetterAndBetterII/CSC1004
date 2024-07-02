package org.zyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// 事件分发器
public class EventDispatcher {
    private static final Logger logger = LogManager.getLogger(EventDispatcher.class);

    private final Map<String, List<EventListener>> listeners = new HashMap<>();  // 监听器容器<事件名称，对应需要响应的events>

    public void registerListener(String eventType, EventListener listener) {
        if (this.listeners.containsKey(eventType)) {  // 如果eventType已经有需要响应的events，则直接将listener添加到对应的events中
            this.listeners.get(eventType).add(listener);
            return;
        }
        this.listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        // 如果eventType还没有需要响应的events，则创建一个新的CopyOnWriteArrayList存放这个新的event listener
    }

    public void unregisterListener(String eventType, EventListener listener) {
        if(!this.listeners.containsKey(eventType)){
            logger.error("No listener for event: " + eventType);
            return;
        }
        this.listeners.getOrDefault(eventType, new CopyOnWriteArrayList<>()).remove(listener);
        // 取消注册，从eventType对应的events中移除listener
    }

    public void dispatchEvent(Event event){  // 事件分发，接受一个事件作为参数，然后遍历对应的事件监听器列表，依次调用它们的onEvent方法
        List<EventListener> eventListeners = listeners.getOrDefault(event.getType(), new CopyOnWriteArrayList<>());
        if(eventListeners.isEmpty()){
            logger.error("No listener for event: " + event.getType());
            return;
        }
        for (EventListener listener : eventListeners) {
            try{
                listener.onEvent(event);
            }catch (IOException e){
                logger.error("Error occurred when dispatching event: " + event.getType(), e);
                // 捕获IOException，打印错误日志
            }
        }
    }
    public boolean hasListener(String eventType){
        return this.listeners.containsKey(eventType);
    }
}