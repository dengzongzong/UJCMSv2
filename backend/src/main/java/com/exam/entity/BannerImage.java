package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 首页横幅图片(轮播图下方的长条图片)
 */
@Data
@TableName("banner_image")
public class BannerImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;          // 标题文字(显示在图片上)
    private String imageUrl;      // 图片URL
    private String linkUrl;       // 点击跳转链接(可选)
    private Integer status;       // 0-隐藏 1-显示
    private Integer sort;         // 排序
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
