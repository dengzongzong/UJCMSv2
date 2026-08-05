package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.Result;
import com.exam.entity.LiveMessage;
import com.exam.entity.LiveRoom;
import com.exam.security.JwtUtil;
import com.exam.service.LiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户端直播间Controller
 * - /api/user/live/** 需要登录(拦截器注入 userId)
 * - /api/user/live/public/** 公开(未登录 userId=null, 不返回播放地址)
 */
@Slf4j
@RestController
@RequestMapping("/user/live")
public class LiveController {

    @Autowired
    private LiveService liveService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 直播大厅(需登录) */
    @GetMapping("/list")
    public Result<List<LiveRoom>> list(@RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(liveService.liveList(userId));
    }

    /** 公开直播大厅(未登录可看列表, 已登录则标记是否开通) */
    @GetMapping("/public/list")
    public Result<List<LiveRoom>> publicList(HttpServletRequest request) {
        return Result.success(liveService.liveList(resolveUserId(request)));
    }

    /** 直播间详情(已开通该直播才返回播放地址) */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id,
                                              @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(liveService.publicDetail(id, userId));
    }

    /** 公开直播间详情(未登录 userId=null, 已登录则从 token 取 userId 判断是否开通) */
    @GetMapping("/public/{id}")
    public Result<Map<String, Object>> publicDetail(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(liveService.publicDetail(id, resolveUserId(request)));
    }

    /** 进入直播间(校验直播开通 + 累计观看人次) */
    @PostMapping("/{id}/enter")
    public Result<Void> enter(@PathVariable Long id,
                              @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        liveService.enter(id, userId);
        return Result.success();
    }

    /** 拉取最近聊天记录(需登录) */
    @GetMapping("/{id}/messages")
    public Result<List<LiveMessage>> messages(@PathVariable Long id,
                                              @RequestParam(required = false) Integer limit) {
        return Result.success(liveService.messages(id, limit));
    }

    /** 拉取最近聊天记录(公开, 未登录也可看聊天) */
    @GetMapping("/public/{id}/messages")
    public Result<List<LiveMessage>> publicMessages(@PathVariable Long id,
                                                    @RequestParam(required = false) Integer limit) {
        return Result.success(liveService.messages(id, limit));
    }

    /** 发送聊天消息(HTTP 兜底, 主要走 WebSocket) */
    @PostMapping("/{id}/message")
    public Result<LiveMessage> sendMessage(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        String content = body == null ? null : body.get("content");
        return Result.success(liveService.sendMessage(id, userId, content));
    }

    /** 从 Authorization Bearer token 解析 userId(未带或无效返回 null) */
    private Long resolveUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isEmpty()) {
            log.warn("[live] resolveUserId: 请求未带Authorization头, uri={}", request.getRequestURI());
            return null;
        }
        String token = header.startsWith("Bearer ") ? header.substring(7) : header;
        Long uid = jwtUtil.getUserId(token);
        log.info("[live] resolveUserId: uri={}, tokenLen={}, userId={}", request.getRequestURI(), token.length(), uid);
        return uid;
    }
}
