package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 开通考试DTO
 */
@Data
public class OpenExamDTO {
    private Long studentId;
    /** 考试ID列表 */
    private List<Long> examIds;
}
