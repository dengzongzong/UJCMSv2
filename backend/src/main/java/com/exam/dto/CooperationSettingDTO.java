package com.exam.dto;

import lombok.Data;

/**
 * 合作咨询配置 DTO
 */
@Data
public class CooperationSettingDTO {
    private String phone1;
    private String phone2;
    private String email1;
    private String email2;
    private String processDesc;
    private String intro;
    private String attachmentName;
    private String attachmentUrl;
}
