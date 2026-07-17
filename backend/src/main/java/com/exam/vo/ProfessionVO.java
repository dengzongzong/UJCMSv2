package com.exam.vo;

import lombok.Data;

import java.util.List;

/**
 * 专业及科目树
 */
@Data
public class ProfessionVO {

    /**
     * 专业ID
     */
    private Long id;

    /**
     * 专业名称
     */
    private String name;

    /**
     * 该专业下的科目列表
     */
    private List<SubjectVO> subjects;
}
