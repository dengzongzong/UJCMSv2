package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的考试记录列表项VO
 */
@Data
public class ExamRecordVO {
    private Long id;
    private Long examId;
    private String examName;
    private BigDecimal score;
    private Integer duration;
    private Integer submitStatus;
    private LocalDateTime submitTime;
}
