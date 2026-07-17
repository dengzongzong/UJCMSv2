package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷介绍页VO
 */
@Data
public class ExamIntroVO {
    private Long id;
    private String name;
    private Integer questionCount;
    private BigDecimal totalScore;
    private Integer duration;
    private String intro;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer allowRetry;
    private Integer maxAttempts;
}
