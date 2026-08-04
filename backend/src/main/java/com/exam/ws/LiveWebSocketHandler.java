package com.exam.ws;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.LiveMessage;
import com.exam.entity.Student;
import com.exam.mapper.LiveMessageMapper;
import com.exam.mapper.StudentMapper;
import com.exam.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 直播间 WebSocket
 * - 客户端连接 /ws/live/{liveId}?token=xxx
 * - 连接成功后服务端推送 history(最近50条聊天) 和 online(在线人数)
 * - 客户端发送 {"action":"chat","content":"xxx"} 进行聊天, 服务端持久化后广播
 * - 客户端发送 {"action":"ping"} 保活, 服务端回 pong
 * - 直播结束/删除时调用 closeRoom 关闭该直播间所有连接
 */
@Component
public class LiveWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveWebSocketHandler.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LiveMessageMapper liveMessageMapper;

    @Autowired
    private StudentMapper studentMapper;

    /** 直播ID -> 该直播间所有 session */
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    /** sessionId -> 直播ID(断开时清理) */
    private final Map<String, Long> sessionRooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(new CloseStatus(4000, "参数错误"));
            return;
        }
        Long liveId = parseLiveId(uri.getPath());
        // 鉴权: 从 query 取 token
        String token = parseQuery(uri.getQuery(), "token");
        Long userId = null;
        if (token != null && !token.isEmpty()) {
            userId = jwtUtil.getUserId(token);
            if (userId == null) {
                session.close(new CloseStatus(4001, "登录已过期,请重新登录"));
                return;
            }
        }
        if (liveId == null) {
            session.close(new CloseStatus(4000, "参数错误"));
            return;
        }
        session.getAttributes().put("liveId", liveId);
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("nickname", buildNickname(userId));
        roomSessions.computeIfAbsent(liveId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionRooms.put(session.getId(), liveId);

        // 推送历史消息
        sendHistory(session, liveId);
        // 广播最新在线人数
        broadcastOnline(liveId);
        log.debug("直播间WS连接: liveId={} session={} userId={}", liveId, session.getId(), userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long liveId = (Long) session.getAttributes().get("liveId");
        if (liveId == null) {
            return;
        }
        try {
            Map<?, ?> body = mapper.readValue(message.getPayload(), Map.class);
            String action = body.get("action") == null ? "" : body.get("action").toString();
            if ("chat".equals(action)) {
                String content = body.get("content") == null ? "" : body.get("content").toString().trim();
                if (!content.isEmpty()) {
                    if (content.length() > 500) {
                        content = content.substring(0, 500);
                    }
                    Long userId = (Long) session.getAttributes().get("userId");
                    String nickname = (String) session.getAttributes().get("nickname");
                    LiveMessage msg = new LiveMessage();
                    msg.setLiveId(liveId);
                    msg.setStudentId(userId);
                    msg.setNickname(nickname);
                    msg.setContent(content);
                    try {
                        liveMessageMapper.insert(msg);
                    } catch (Exception e) {
                        log.warn("聊天消息入库失败: {}", e.getMessage());
                    }
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("action", "chat");
                    payload.put("id", msg.getId());
                    payload.put("studentId", userId);
                    payload.put("nickname", nickname);
                    payload.put("content", content);
                    payload.put("createTime", msg.getCreateTime() == null ? null
                            : msg.getCreateTime().toString().replace('T', ' '));
                    broadcast(liveId, payload);
                }
            } else if ("ping".equals(action)) {
                sendJson(session, "pong", null);
            }
        } catch (Exception e) {
            log.warn("直播间WS消息处理失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long liveId = sessionRooms.remove(session.getId());
        if (liveId != null) {
            Set<WebSocketSession> set = roomSessions.get(liveId);
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) {
                    roomSessions.remove(liveId);
                }
            }
            broadcastOnline(liveId);
        }
        log.debug("直播间WS连接关闭: {} status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /** 在线人数 */
    public int getOnlineCount(Long liveId) {
        if (liveId == null) {
            return 0;
        }
        Set<WebSocketSession> set = roomSessions.get(liveId);
        return set == null ? 0 : set.size();
    }

    /** 直播结束/删除时关闭该直播间所有连接并清理 */
    public void closeRoom(Long liveId) {
        if (liveId == null) {
            return;
        }
        Set<WebSocketSession> set = roomSessions.remove(liveId);
        if (set != null) {
            for (WebSocketSession s : set) {
                sessionRooms.remove(s.getId());
                try {
                    if (s.isOpen()) {
                        s.close(new CloseStatus(4000, "直播已结束"));
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        log.debug("直播间WS清理: liveId={}", liveId);
    }

    /** 向直播间所有 session 广播 */
    private void broadcast(Long liveId, Map<String, Object> payload) {
        Set<WebSocketSession> set = roomSessions.get(liveId);
        if (set == null || set.isEmpty()) {
            return;
        }
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
                set.remove(s);
            }
        }
    }

    /** 广播在线人数 */
    private void broadcastOnline(Long liveId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "online");
        payload.put("count", getOnlineCount(liveId));
        broadcast(liveId, payload);
    }

    /** 连接后推送最近50条历史消息 */
    private void sendHistory(WebSocketSession session, Long liveId) {
        LambdaQueryWrapper<LiveMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveMessage::getLiveId, liveId)
                .orderByDesc(LiveMessage::getCreateTime)
                .last("LIMIT 50");
        List<LiveMessage> list = liveMessageMapper.selectList(wrapper);
        java.util.Collections.reverse(list);
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "history");
        payload.put("list", list);
        sendJson(session, null, payload);
    }

    private void sendJson(WebSocketSession session, String action, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            if (action != null) {
                payload.put("action", action);
            }
            if (data != null) {
                payload.put("data", data);
            }
            synchronized (session) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /** 从 /ws/live/{liveId} 解析直播ID */
    private Long parseLiveId(String path) {
        if (path == null) {
            return null;
        }
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("live".equals(parts[i])) {
                try {
                    return Long.valueOf(parts[i + 1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private String parseQuery(String query, String key) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && key.equals(pair.substring(0, idx))) {
                return pair.substring(idx + 1);
            }
        }
        return null;
    }

    private String buildNickname(Long userId) {
        if (userId == null) {
            return "游客";
        }
        Student student = studentMapper.selectById(userId);
        if (student == null) {
            return "游客";
        }
        return student.getName() != null ? student.getName()
                : (student.getPhone() != null ? student.getPhone() : "游客");
    }
}
