package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 题目新增/编辑DTO
 */
@Data
public class QuestionDTO {
    private Long id;
    /** 1-单选 2-多选 3-填空 4-判断 5-简答 */
    private Integer type;
    private Long categoryId;
    private Long professionId;
    private String content;
    private String analysis;
    /** 填空题的正确答案文本（多个空用逗号分隔） */
    private String correctAnswer;
    private java.math.BigDecimal score;
    private Integer hasImage;
    private Integer enabled;
    /** 选项列表 */
    private List<OptionDTO> options;
    /**
     * 强制创建/更新：true 表示跳过题干重复检测直接保存
     * （前端检测到重复并经用户确认后置为 true 再次提交）
     */
    private Boolean force;
}
