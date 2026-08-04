package com.exam.controller;

import com.exam.common.Result;
import com.exam.config.LiveConfig;
import com.exam.service.LiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 云直播录制回调(公开接口, 无需登录)
 * - 腾讯云: 推流域名 → 事件回调(录制) → POST /public/live/callback/tencent
 * - 阿里云: 推流域名 → 事件回调(录制) → POST /public/live/callback/aliyun
 * 录制完成后云厂商自动通知, 按流名匹配场次并自动回填回放地址, 学生即可事后观看
 */
@Slf4j
@RestController
@RequestMapping("/public/live/callback")
public class LiveCallbackController {

    @Autowired
    private LiveService liveService;

    @Autowired
    private LiveConfig liveConfig;

    /** 腾讯云录制回调 */
    @PostMapping("/tencent")
    public Result<Void> tencent(@RequestBody(required = false) Map<String, Object> body,
                                HttpServletRequest request) {
        if (!checkKey(request)) {
            return Result.forbidden("回调密钥错误");
        }
        boolean ok = handleCallback(body);
        return ok ? Result.success() : Result.success("未匹配到直播场次", null);
    }

    /** 阿里云录制回调 */
    @PostMapping("/aliyun")
    public Result<Void> aliyun(@RequestBody(required = false) Map<String, Object> body,
                               HttpServletRequest request) {
        if (!checkKey(request)) {
            return Result.forbidden("回调密钥错误");
        }
        boolean ok = handleCallback(body);
        return ok ? Result.success() : Result.success("未匹配到直播场次", null);
    }

    /** 兼容腾讯云/阿里云回调结构: 优先识别推流开始/结束事件, 其余按录制回调处理 */
    private boolean handleCallback(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return false;
        }
        String streamName = extract(body, "StreamName", "stream_name", "streamName",
                "StreamId", "stream_id", "streamId", "stream");
        if (streamName == null) {
            return false;
        }

        // ---- 腾讯云: event_type/EventType 数值事件 (100=推流开始 200=推流结束 300=录制) ----
        Integer eventType = getInt(body, "EventType", "event_type");
        if (eventType != null) {
            if (eventType == 100) {
                return liveService.onPushStart(streamName);
            }
            if (eventType == 200) {
                return liveService.onPushEnd(streamName);
            }
            // 其他(录制等)继续走通用解析
        }

        // ---- 阿里云: Type=push + EventName 事件名 ----
        String eventName = getStr(body, "EventName", "event_name", "event");
        if (eventName != null) {
            String lower = eventName.toLowerCase();
            // 先判断结束(unpublish 也含 publish, 避免误判为开始)
            if (lower.contains("pushstopped") || lower.contains("push_stop")
                    || lower.contains("stopped") || lower.contains("unpublish")) {
                return liveService.onPushEnd(streamName);
            }
            if (lower.contains("pushstarted") || lower.contains("push_start")
                    || lower.contains("publish") || lower.contains("started")) {
                return liveService.onPushStart(streamName);
            }
        }

        // ---- 录制回调: 提取录制地址并自动回填回放 ----
        String replayUrl = extract(body, "VideoUrl", "video_url", "videoUrl",
                "RecordUrl", "record_url", "recordUrl", "PlayUrl", "play_url", "playUrl", "Url", "url");
        if (replayUrl == null) {
            log.debug("回调缺少录制地址: body={}", body);
            return false;
        }
        return liveService.autoReplay(streamName, replayUrl);
    }

    /** 先找顶层字段, 找不到再递归常见子对象(Video/Data等) */
    private String extract(Map<String, Object> map, String... keys) {
        String v = findIn(map, keys);
        if (v != null) {
            return v;
        }
        for (String sub : new String[]{"Video", "video", "Data", "data", "DataInfo", "dataInfo"}) {
            Object o = map.get(sub);
            if (o instanceof Map) {
                v = findIn((Map<String, Object>) o, keys);
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }

    /** 大小写不敏感查找指定 key 的字符串值 */
    private String findIn(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getKey().equalsIgnoreCase(key) && e.getValue() != null) {
                    return e.getValue().toString();
                }
            }
        }
        return null;
    }

    /** 大小写不敏感读取整型字段(先在顶层找, 再找子对象) */
    private Integer getInt(Map<String, Object> map, String... keys) {
        String v = findIn(map, keys);
        if (v == null) {
            for (String sub : new String[]{"Video", "video", "Data", "data"}) {
                Object o = map.get(sub);
                if (o instanceof Map) {
                    v = findIn((Map<String, Object>) o, keys);
                    if (v != null) break;
                }
            }
        }
        if (v == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 大小写不敏感读取字符串字段(先在顶层找, 再找子对象) */
    private String getStr(Map<String, Object> map, String... keys) {
        String v = findIn(map, keys);
        if (v != null) {
            return v;
        }
        for (String sub : new String[]{"Video", "video", "Data", "data"}) {
            Object o = map.get(sub);
            if (o instanceof Map) {
                v = findIn((Map<String, Object>) o, keys);
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }

    /** 校验回调密钥(?key=xxx, 配置为空则不校验) */
    private boolean checkKey(HttpServletRequest request) {
        String secret = liveConfig.getCallbackSecret();
        if (secret == null || secret.isEmpty()) {
            return true;
        }
        String key = request.getParameter("key");
        return secret.equals(key);
    }
}
