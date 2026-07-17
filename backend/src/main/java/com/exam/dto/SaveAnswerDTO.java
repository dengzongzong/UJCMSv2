package com.exam.dto;

import lombok.Data;

/**
 * 保存单题答案DTO（断点续考用）
 */
@Data
public class SaveAnswerDTO {
    private Long recordId;
    private Long questionId;
    /**
     * 学生答案，选项label逗号分隔，例如 "A" 或 "A,C"
     */
    private String studentAnswer;
}
