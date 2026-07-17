package com.exam.dto;

import lombok.Data;

/**
 * 更新个人信息DTO
 */
@Data
public class ProfileUpdateDTO {
    private String nickname;
    private String avatar;
}
