package org.zyz;

import java.io.IOException;

// 事件监听器接口
public interface EventListener {
    void onEvent(Event event) throws IOException;
}
