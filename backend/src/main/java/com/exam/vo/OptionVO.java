package com.exam.vo;

import lombok.Data;

/**
 * 选项VO（不包含正确答案标识）
 */
@Data
public class OptionVO {
    private Long id;
    private String label;
    private String content;
}
