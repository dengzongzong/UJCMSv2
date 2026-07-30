package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 授权培育基地证书内容(单条记录)
 * 存储证书背景图片URL + 覆盖在图片上的富文本HTML
 */
@Data
@TableName("cooperation_cert_content")
public class CooperationCertContent {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 证书背景图片URL */
    private String imageUrl;

    /** 覆盖在图片上的富文本HTML内容 */
    private String richText;

    private LocalDateTime updateTime;
}
