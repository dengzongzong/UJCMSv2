package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 题目VO（不含正确答案和解析）
 */
@Data
public class QuestionVO {
    private Long id;
    private Integer type;
    private String content;
    private BigDecimal score;
    private List<OptionVO> options;
    /**
     * 学生已答答案（断点续考时回填），未作答则为 null
     */
    private String userAnswer;
}
