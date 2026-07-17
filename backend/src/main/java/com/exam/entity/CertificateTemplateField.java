package com.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 证书模板字段位置
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@TableName("certificate_template_field")
public class CertificateTemplateField {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 模板ID */
    private Long templateId;
    /** 字段键 */
    private String fieldKey;
    /** X坐标(像素) */
    private Integer x;
    /** Y坐标 */
    private Integer y;
    /** 字号 */
    private Integer fontSize;
    /** 颜色 #RRGGBB */
    private String color;
    /** 1-常规 2-粗体 */
    private Integer fontWeight;
    /** 1-左 2-居中 3-右 */
    private Integer align;
    /** 文本框宽度 */
    private Integer width;
    /** 图片字段高度(photo/qr/examQr),null时按原始宽高比等比缩放 */
    private Integer height;
    /** 显示顺序 */
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
