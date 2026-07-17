package com.exam.controller;

import com.exam.common.Result;
import com.exam.service.CaptchaResult;
import com.exam.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码 Controller(公开接口, 无需鉴权)
 */
@RestController
@RequestMapping("/public/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    /**
     * 生成图形验证码
     * <p>返回 { captchaKey, imageBase64, expireMillis },前端用 imageBase64 拼 data:image/png;base64, 显示</p>
     */
    @GetMapping("/generate")
    public Result<CaptchaResult> generate() {
        return Result.success(captchaService.generate());
    }
}
