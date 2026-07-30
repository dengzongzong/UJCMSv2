package com.exam.dto;

import lombok.Data;

/**
 * 更新个人信息DTO
 */
@Data
public class ProfileUpdateDTO {
    private String nickname;
    private String avatar;
    /** 手机号(修改手机号时传入) */
    private String phone;
}
