package com.exam.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码生成结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResult {
    /** 验证码 key,前端注册/重置密码时回传(配合 captchaCode 一起) */
    private String captchaKey;
    /** PNG base64 字符串(不含 data:image/png;base64, 前缀) */
    private String imageBase64;
    /** 过期时间(毫秒) */
    private Long expireMillis;
}
