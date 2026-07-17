package com.exam.vo;

import lombok.Data;

import java.util.List;

/**
 * 错题VO
 */
@Data
public class WrongQuestionVO {
    private Long id;
    private Long questionId;
    private String content;
    private Integer type;
    private String studentAnswer;
    private String correctAnswer;
    private String analysis;
    private List<OptionVO> options;
    /** 是否已掌握: 0=未掌握, 1=已掌握 */
    private Integer mastered;
}
