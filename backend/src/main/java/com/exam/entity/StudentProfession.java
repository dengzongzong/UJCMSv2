package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生-专业关联表
 */
@Data
@TableName("student_profession")
public class StudentProfession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long professionId;
    @TableField(exist = false)
    private String professionName;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
