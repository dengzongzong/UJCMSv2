package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 答题结果VO（含正确答案和解析）
 */
@Data
public class AnswerResultVO {
    private Long questionId;
    private Integer sort;
    private Integer isCorrect;
    /** 该题得分(人工批改简答题时设置) */
    private BigDecimal score;
    private String studentAnswer;
    private String correctAnswer;
    private String content;
    private String analysis;
    private List<OptionVO> options;
}
