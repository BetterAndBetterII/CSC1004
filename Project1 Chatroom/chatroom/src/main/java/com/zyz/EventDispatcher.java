package com.zyz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// 事件分发器
public class EventDispatcher {
    private static final Logger logger = LogManager.getLogger(EventDispatcher.class);

    private final Map<String, List<EventListener>> listeners = new HashMap<>();
    // Listener Map, key is event type, value is a list of listeners

    /**
     * Register a listener for the event
     * @param eventType event type
     * @param listener listener
     */
    public void registerListener(String eventType, EventListener listener) {
        if (this.listeners.containsKey(eventType)) {
            // If eventType already exists, add the listener to the list
            this.listeners.get(eventType).add(listener);
            return;
        }
        this.listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        // If eventType does not exist, create a new list and add the listener to the list
    }

    /**
     * Unregister a listener for the event
     * @param eventType event type
     * @param listener listener
     */
    public void unregisterListener(String eventType, EventListener listener) {
        if(!this.listeners.containsKey(eventType)){
            logger.error("No listener for event: " + eventType);
            return;
        }
        this.listeners.getOrDefault(eventType, new CopyOnWriteArrayList<>()).remove(listener);
        // Unregister the listener from the list
    }

    /**
     * Dispatch an event
     * @param event event
     */
    public void dispatchEvent(Event event){
        // Dispatch the event to all listeners
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
                // Catch the exception and continue to dispatch the event to other listeners
            }
        }
    }

    /**
     * Check if the event type has listener
     * @param eventType event type
     * @return true if the event type has listener, otherwise false
     */
    public boolean hasListener(String eventType){
        return this.listeners.containsKey(eventType);
    }
}