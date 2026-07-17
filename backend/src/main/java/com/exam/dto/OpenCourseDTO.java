package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 开通课程DTO
 */
@Data
public class OpenCourseDTO {
    private Long studentId;
    /** 课程ID列表 */
    private List<Long> courseIds;
}
