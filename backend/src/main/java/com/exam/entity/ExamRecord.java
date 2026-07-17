package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long examId;
    /** 冗余考试名称(删除考试后仍可展示) */
    private String examName;
    /** 冗余考试专业ID(删除考试后仍可按专业分组) */
    private Long examProfessionId;
    /** 冗余考试封面图(删除考试后仍可展示) */
    private String examCoverUrl;
    /** 冗余考试题目数(删除考试后仍可展示) */
    private Integer examQuestionCount;
    /** 冗余考试总分(删除考试后仍可展示) */
    private BigDecimal examTotalScore;
    /** 冗余试卷ID(删除考试后仍可查看试卷题目) */
    private Long paperId;
    private BigDecimal score;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer totalCount;
    /** 待批改题数（简答题） */
    private Integer pendingCount;
    private BigDecimal accuracy;
    private Integer duration;
    private Integer submitStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;
    private Integer hasCertificate;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
