package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question_template")
public class QuestionTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private Long professionId;
    private Integer questionCount;
    private BigDecimal totalScore;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    // 非持久化字段
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String professionName;
}
