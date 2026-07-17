package com.exam.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 发送短信验证码请求
 */
public class SendCodeDTO {
    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 业务类型: register / reset-password */
    @NotBlank(message = "请指定验证码类型")
    private String type;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
