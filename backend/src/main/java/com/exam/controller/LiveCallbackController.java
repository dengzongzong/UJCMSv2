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

    /** 兼容腾讯云/阿里云录制回调结构, 提取流名与录制地址并回填 */
    private boolean handleCallback(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return false;
        }
        String streamName = extract(body, "StreamName", "stream_name", "streamName",
                "StreamId", "stream_id", "streamId");
        String replayUrl = extract(body, "VideoUrl", "video_url", "videoUrl",
                "RecordUrl", "record_url", "recordUrl", "PlayUrl", "play_url", "playUrl", "Url", "url");
        if (streamName == null || replayUrl == null) {
            log.debug("录制回调缺少流名或地址: body={}", body);
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
