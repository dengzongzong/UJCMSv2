package com.exam.ws;

import com.exam.common.AsyncTask;
import com.exam.common.AsyncTaskEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 异步任务 WebSocket 推送
 * - 客户端连接 /ws/task
 * - 收到 subscribe 消息(JSON){"action":"subscribe","taskId":"xxx"} 后,服务端开始推送该任务的进度
 * - 任务进入终态(success/failed/cancelled)后,推送一条最终消息并自动退订
 */
@Component
public class AsyncTaskWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskWebSocketHandler.class);

    private final ObjectMapper mapper = new ObjectMapper();

    /** 任务ID -> 订阅该任务的 session 集合 */
    private final Map<String, Set<WebSocketSession>> subscribers = new ConcurrentHashMap<>();
    /** sessionId -> session(用于断开时清理) */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.debug("WS 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<?, ?> body = mapper.readValue(message.getPayload(), Map.class);
            String action = (String) body.get("action");
            if ("subscribe".equals(action)) {
                String taskId = (String) body.get("taskId");
                if (taskId != null) {
                    subscribe(session, taskId);
                }
            } else if ("unsubscribe".equals(action)) {
                String taskId = (String) body.get("taskId");
                if (taskId != null) {
                    unsubscribe(session, taskId);
                }
            } else if ("ping".equals(action)) {
                sendJson(session, "pong", null);
            }
        } catch (Exception e) {
            log.warn("WS 消息处理失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 清理该 session 所有的订阅
        for (Set<WebSocketSession> set : subscribers.values()) {
            set.remove(session);
        }
        sessions.remove(session.getId());
        log.debug("WS 连接关闭: {} status={}", session.getId(), status);
    }

    /**
     * 任务进度/状态变化时由 service 调用
     * 仅向订阅了 taskId 的 session 推送
     */
    @EventListener
    @Async
    public void onTaskEvent(AsyncTaskEvent event) {
        pushUpdate(event.getTask());
    }

    public void pushUpdate(AsyncTask task) {
        if (task == null || task.getTaskId() == null) return;
        Set<WebSocketSession> set = subscribers.get(task.getTaskId());
        if (set == null || set.isEmpty()) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "update");
        payload.put("task", toMap(task));
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : set) {
            if (!s.isOpen()) {
                set.remove(s);
                continue;
            }
            try {
                synchronized (s) {
                    s.sendMessage(msg);
                }
            } catch (IOException e) {
                log.debug("WS 推送失败,移除: " + s.getId());
                set.remove(s);
            }
        }
    }

    private void subscribe(WebSocketSession session, String taskId) {
        subscribers.computeIfAbsent(taskId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("WS 订阅: taskId={} session={}", taskId, session.getId());
    }

    private void unsubscribe(WebSocketSession session, String taskId) {
        Set<WebSocketSession> set = subscribers.get(taskId);
        if (set != null) set.remove(session);
    }

    private void sendJson(WebSocketSession s, String action, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", action);
            if (data != null) payload.put("data", data);
            synchronized (s) {
                s.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private Map<String, Object> toMap(AsyncTask t) {
        Map<String, Object> m = new HashMap<>();
        m.put("taskId", t.getTaskId());
        m.put("bizType", t.getBizType());
        m.put("bizName", t.getBizName());
        m.put("status", t.getStatus());
        m.put("progress", t.getProgress());
        m.put("processed", t.getProcessed());
        m.put("total", t.getTotal());
        m.put("successCount", t.getSuccessCount());
        m.put("failCount", t.getFailCount());
        m.put("errorMessage", t.getErrorMessage());
        m.put("resultFileName", t.getResultFileName());
        m.put("hasResultFile", t.getResultFile() != null || (t.getResultFilePath() != null));
        return m;
    }
}
