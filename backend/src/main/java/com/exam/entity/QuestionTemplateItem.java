package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question_template_item")
public class QuestionTemplateItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long questionId;
    private Integer sort;
}
