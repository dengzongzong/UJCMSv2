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
 * 学员照片
 */
@Data
@TableName("certificate_photo")
public class CertificatePhoto {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 身份证号 */
    private String idCard;
    /** 姓名(冗余便于管理) */
    private String name;
    /** 照片 URL */
    private String url;
    /** 关联的证书记录ID(支持同一个人不同证书设置不同照片,为空时使用身份证号匹配最新照片) */
    private Long certificateId;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
}
