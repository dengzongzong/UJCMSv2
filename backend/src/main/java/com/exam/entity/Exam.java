package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam")
public class Exam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 考试分类(用于考试中心按分类分组) */
    private String category;
    private String coverUrl;
    private String intro;
    private BigDecimal totalScore;
    private Integer duration;
    private Integer questionCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private Integer allowRetry;
    /** 最大考试次数(0=不限) */
    private Integer maxAttempts;
    private Integer status;
    private Long professionId;
    @TableField(exist = false)
    private String professionName;
    private Long subjectId;
    private Long paperId;
    /** 基础考过人数(后台手工设置, 展示=基数+实际开通人数) */
    private Integer baseExamCount;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
