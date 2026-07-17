package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 证书用户(由"用户管理"中的学员数据定时同步而来,作为证书发放的候选人员)
 */
@Data
@TableName("certificate_user")
public class CertificateUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联 student.id(可空) */
    private Long studentId;
    private String name;
    /** 身份证号(业务唯一键) */
    private String idCard;
    private String phone;
    private String professionName;
    /** 证书类型(从学生管理同步) */
    private String certType;
    /** 专业ID(支持同一学生不同专业创建多条记录) */
    private Long professionId;
    /** 1-男 2-女(由身份证号推断) */
    private Integer gender;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
