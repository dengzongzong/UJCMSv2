package com.exam.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录态修改密码请求
 * <p>必须带 Authorization Bearer token, 需要校验"原密码"才能改</p>
 * <p>用途: 学员端"我的"页改密码</p>
 */
@Data
public class ChangePasswordDTO {

    /**
     * 原密码
     */
    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String newPassword;
}
