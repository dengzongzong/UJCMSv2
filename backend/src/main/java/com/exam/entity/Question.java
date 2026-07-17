package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 1-单选 2-多选 3-填空 4-判断 5-简答 */
    private Integer type;
    private Long categoryId;
    private Long professionId;
    private String content;
    private String analysis;
    /** 填空题的正确答案文本（多个空用逗号分隔） */
    private String correctAnswer;
    private BigDecimal score;
    private Integer hasImage;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String professionName;
    @TableField(exist = false)
    private String categoryName;
}
