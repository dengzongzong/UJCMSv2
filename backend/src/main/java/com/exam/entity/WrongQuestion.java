package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_question")
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long questionId;
    private Long examId;
    private String studentAnswer;
    /**
     * 是否已掌握: 0=未掌握(默认), 1=已掌握
     */
    private Integer mastered;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
