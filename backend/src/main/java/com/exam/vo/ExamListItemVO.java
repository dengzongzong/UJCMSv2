package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试列表项VO
 */
@Data
public class ExamListItemVO {
    private Long id;
    private String name;
    /** 考试分类(用于考试中心按分类分组) */
    private String category;
    private String coverUrl;
    private Integer questionCount;
    private BigDecimal totalScore;
    private Integer duration;
    /** 已考过人数(基数+实际开通权限人数) */
    private Integer examCount;
    /**
     * 所属专业ID(可为 null,表示通用考试)
     */
    private Long professionId;
    /**
     * 所属专业名称(professionId 为 null 时为 "通用考试")
     */
    private String professionName;
    /**
     * 上次考试时间
     */
    private LocalDateTime lastTime;
    /**
     * 上次考试分数
     */
    private BigDecimal lastScore;
    /**
     * 当前学生是否已开通该考试(true=已开通 可答题;false=未开通 点击要校验)
     */
    private Boolean purchased;
}
