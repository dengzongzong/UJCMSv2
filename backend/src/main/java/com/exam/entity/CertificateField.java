package com.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 证书字段定义
 */
@Data
@TableName("certificate_field")
public class CertificateField {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 字段键(英文唯一,系统内置不可改) */
    private String fieldKey;
    /** 字段显示名 */
    private String fieldName;
    /** 1-文本 2-数字 3-日期 4-选择项 5-图片 */
    private Integer fieldType;
    /** 是否必填 */
    private Integer required;
    /** 显示顺序 */
    private Integer sort;
    /** 默认值 */
    private String defaultValue;
    /** 选择项(逗号分隔) */
    private String options;
    /** 1-系统内置 0-自定义 */
    private Integer isSystem;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
