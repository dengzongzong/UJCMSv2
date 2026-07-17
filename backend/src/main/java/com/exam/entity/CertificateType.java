package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 证书类型
 */
@Data
@TableName("certificate_type")
public class CertificateType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;       // 类型名称
    private String code;       // 类型编码(mcode)
    private Integer sort;      // 排序
    private Integer status;    // 0-禁用 1-启用
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
