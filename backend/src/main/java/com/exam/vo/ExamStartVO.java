package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 开始考试返回VO
 */
@Data
public class ExamStartVO {
    private Long recordId;
    private String examName;
    private BigDecimal totalScore;
    private Integer duration;
    private Integer questionCount;
    private LocalDateTime startTime;
    private List<QuestionVO> questions;
}
