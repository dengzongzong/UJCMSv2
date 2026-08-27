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
    /** 题目类型: 1-单选 2-多选 3-填空 4-判断 5-简答 */
    private Integer type;
    private Integer isCorrect;
    /** 该题得分(人工批改简答题时设置) */
    private BigDecimal score;
    private String studentAnswer;
    private String correctAnswer;
    private String content;
    private String analysis;
    private List<OptionVO> options;
}
