package com.exam.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 注册请求
 */
@Data
public class RegisterDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 姓名(注册时必填)
     */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 身份证号(注册时必填)
     */
    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

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
     * 是否勾选用户协议
     */
    private Boolean agreement;

    /**
     * 昵称(可选,不填则默认取手机号后四位)
     */
    private String nickname;
}
