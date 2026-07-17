package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.FeedbackMessageDTO;
import com.exam.entity.FeedbackMessage;
import com.exam.service.FeedbackMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 合作咨询/投诉建议 - 用户端提交接口(无需鉴权)
 */
@RestController
@RequestMapping("/public/feedback")
public class FeedbackPublicController {

    @Autowired
    private FeedbackMessageService service;

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody FeedbackMessageDTO dto, HttpServletRequest request) {
        FeedbackMessage m = new FeedbackMessage();
        m.setType(dto.getType());
        m.setOrgName(dto.getOrgName());
        m.setContactName(dto.getContactName());
        m.setPhone(dto.getPhone());
        m.setEmail(dto.getEmail());
        m.setContent(dto.getContent());
        m.setStatus(0);
        m.setIp(getClientIp(request));
        service.save(m);
        return Result.success();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多个代理时第一个为真实IP
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
