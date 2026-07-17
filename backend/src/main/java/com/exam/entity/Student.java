package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("student")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;       // 学生姓名
    private String studentNo;  // 学号
    private String phone;
    private String password;
    private String nickname;
    /** 身份证号 */
    private String idCard;
    private String avatar;
    private Long professionId;
    private Long subjectId;
    private Integer status;
    /** 证书类型(可为空,关联 certificate_type.name) */
    private String certType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registerTime;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String professionName;
    @TableField(exist = false)
    private List<Long> professionIds;
    @TableField(exist = false)
    private List<String> professionNames;
}
