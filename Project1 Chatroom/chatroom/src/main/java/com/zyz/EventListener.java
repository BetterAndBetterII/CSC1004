package com.zyz;

import java.io.IOException;

// EventListener interface
public interface EventListener {
    void onEvent(Event event) throws IOException;
}
