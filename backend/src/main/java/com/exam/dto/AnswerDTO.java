package com.exam.dto;

import lombok.Data;

/**
 * 答案DTO（选项label逗号分隔）
 */
@Data
public class AnswerDTO {
    private Long questionId;
    /**
     * 学生答案，选项label逗号分隔，例如 "A" 或 "A,C"
     */
    private String studentAnswer;
}
