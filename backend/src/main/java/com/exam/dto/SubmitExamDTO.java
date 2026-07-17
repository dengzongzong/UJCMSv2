package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 提交考试DTO
 */
@Data
public class SubmitExamDTO {
    private Long recordId;
    private List<AnswerDTO> answers;
    /**
     * 用时（秒）
     */
    private Integer duration;
}
