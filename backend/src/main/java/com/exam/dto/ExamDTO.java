package com.exam.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamDTO {
    private Long id;
    private String name;
    /** 考试分类(用于考试中心按分类分组) */
    private String category;

    @JsonAlias("cover")
    private String coverUrl;
    
    private String intro;
    private java.math.BigDecimal totalScore;
    private Integer duration;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer allowRetry;
    private Integer maxAttempts;
    private Integer status;
    private Long professionId;
    private Long subjectId;
    private Long paperId;
    /** 基础考过人数(后台手工设置) */
    private Integer baseExamCount;
}
