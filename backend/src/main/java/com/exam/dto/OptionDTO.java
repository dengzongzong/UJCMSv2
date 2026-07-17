package com.exam.dto;

import lombok.Data;

/**
 * 题目选项DTO
 */
@Data
public class OptionDTO {
    private Long id;
    /** 选项标识 A/B/C/D */
    private String label;
    private String content;
    /** 0-错误 1-正确 */
    private Integer isCorrect;
    private Integer sort;
}
