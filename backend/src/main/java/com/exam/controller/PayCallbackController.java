package com.exam.controller;

import com.exam.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付回调(公开接口, 供微信/支付宝异步通知)
 * - 微信: POST /public/pay/callback/wechat, 期望返回 {"code":"SUCCESS"} (application/json)
 * - 支付宝: POST /public/pay/callback/alipay, 期望返回 "success" (纯文本)
 * 回调成功后自动开通课程(幂等)
 */
@Slf4j
@RestController
@RequestMapping("/public/pay/callback")
public class PayCallbackController {

    @Autowired
    private OrderService orderService;

    @PostMapping(value = "/wechat", produces = MediaType.APPLICATION_JSON_VALUE)
    public String wechat(HttpServletRequest request) {
        String result = orderService.handleWechatCallback(request);
        log.info("微信支付回调结果: {}", result);
        return result;
    }

    @PostMapping(value = "/alipay", produces = MediaType.TEXT_PLAIN_VALUE)
    public String alipay(HttpServletRequest request) {
        String result = orderService.handleAlipayCallback(request);
        log.info("支付宝支付回调结果: {}", result);
        return result;
    }

    /** 兼容按 {channel} 通配的回调地址(不推荐, 保留以防云厂商配置错误) */
    @PostMapping(value = "/{channel}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String any(@PathVariable String channel, HttpServletRequest request) {
        if ("alipay".equalsIgnoreCase(channel)) {
            return orderService.handleAlipayCallback(request);
        }
        return orderService.handleWechatCallback(request);
    }
}
