package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 错题练习DTO
 */
@Data
public class WrongQuestionPracticeDTO {
    /**
     * 错题ID列表
     */
    private List<Long> wrongQuestionIds;
}
