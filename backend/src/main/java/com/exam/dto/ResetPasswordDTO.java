package com.exam.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 重置密码请求
 */
@Data
public class ResetPasswordDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 图形验证码 key(从 /public/captcha/generate 获取,代表"第几张图"的 key)
     */
    @NotBlank(message = "请输入图形验证码")
    private String captchaKey;

    /**
     * 图形验证码(用户输入的字符)
     */
    @NotBlank(message = "请输入图形验证码")
    private String captchaCode;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
