package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("about_us")
public class AboutUs {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String servicePhone;
    private String serviceQrcode;
    /** 关于我们页面右下角二维码所指向的链接(后台配置,前端据此生成二维码) */
    private String qrcodeLink;
    @TableField("content")
    private String content;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
