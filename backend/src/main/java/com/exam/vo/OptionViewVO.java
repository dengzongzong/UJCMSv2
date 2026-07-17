package com.exam.vo;

import lombok.Data;

/**
 * 试卷查看-选项 VO
 */
@Data
public class OptionViewVO {
    private Long id;
    private String label;
    private String content;
    /** 是否正确 */
    private Boolean isCorrect;
}
