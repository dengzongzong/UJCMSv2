package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question_option")
public class QuestionOption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private String label;
    private String content;
    private Integer isCorrect;
    private Integer sort;
}
