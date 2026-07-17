package com.exam.vo;

import lombok.Data;

/**
 * 个人信息VO
 */
@Data
public class ProfileVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private Long professionId;
    private Long subjectId;
}
