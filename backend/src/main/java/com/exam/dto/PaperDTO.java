package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 试卷新增/编辑DTO
 */
@Data
public class PaperDTO {
    private Long id;
    private String name;
    private String description;
    /** 0-未发布 1-已发布 */
    private Integer status;
    /** 题目ID列表 */
    private List<Long> questionIds;
}
