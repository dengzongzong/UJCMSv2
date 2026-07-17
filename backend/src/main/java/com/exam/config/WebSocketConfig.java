package com.exam.config;

import com.exam.ws.AsyncTaskWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置: 注册异步任务推送端点
 * - 端点: /ws/task (无需 JWT 鉴权,仅推送公开进度信息)
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AsyncTaskWebSocketHandler asyncTaskHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(asyncTaskHandler, "/ws/task")
                .setAllowedOriginPatterns("*");
    }
}
