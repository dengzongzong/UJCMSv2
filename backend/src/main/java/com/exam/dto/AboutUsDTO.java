package com.exam.dto;

import lombok.Data;

/**
 * 关于我们设置DTO
 */
@Data
public class AboutUsDTO {
    private String servicePhone;
    private String serviceQrcode;
    /** 关于我们页面右下角二维码所指向的链接 */
    private String qrcodeLink;
    private String content;
}
