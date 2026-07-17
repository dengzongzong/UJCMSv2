package com.exam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginDTO {

    /**
     * 用户名（管理员为账号，学生为手机号）
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 登录角色：admin / student
     */
    @NotBlank(message = "角色不能为空")
    private String role;

    /**
     * 登录方式(仅 student 角色生效): phone-手机号登录(默认) idCard-身份证号登录
     */
    private String loginType;

    /**
     * 是否勾选用户协议
     */
    private Boolean agreement;
}
