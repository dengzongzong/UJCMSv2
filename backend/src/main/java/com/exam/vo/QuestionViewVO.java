package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 题目查看 VO(含正确答案,供证书查询页/查看试卷使用)
 */
@Data
public class QuestionViewVO {
    private Long id;
    private Integer type;
    private String content;
    private BigDecimal score;
    /** 参考答案(选项 label 逗号分隔;非选择题为 question.correctAnswer) */
    private String correctAnswer;
    /** 题目解析 */
    private String analysis;
    /** 题目选项(已标注是否正确) */
    private List<OptionViewVO> options;
}
