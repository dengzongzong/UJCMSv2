package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long questionId;
    private String studentAnswer;
    private Integer isCorrect;
    /** 该题得分(人工批改简答题时设置) */
    private BigDecimal score;
    /** 题目解析 */
    private String analysis;
    private Integer sort;
}
