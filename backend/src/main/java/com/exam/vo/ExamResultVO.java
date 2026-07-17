package com.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试结果VO
 */
@Data
public class ExamResultVO {
    private Long recordId;
    private BigDecimal score;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer totalCount;
    /** 待批改题目数（简答题） */
    private Integer pendingCount;
    private BigDecimal accuracy;
    private Integer duration;
    /**
     * 是否允许重新作答（0-不允许 1-允许）
     */
    private Integer allowRetry;
    /** 交卷时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;
    /** 考试总分 */
    private BigDecimal totalScore;
    /** 及格分 */
    private BigDecimal passScore;
    /** 考试名称 */
    private String examName;
    /** 学生姓名 */
    private String studentName;
    private List<AnswerResultVO> answers;
}
