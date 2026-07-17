package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 我的课程VO（包含学习进度信息）
 */
@Data
public class MyCourseVO {
    private Long id;
    private String name;
    private String coverUrl;
    private String tag;
    private BigDecimal price;
    private Integer totalDuration;
    private Integer sectionCount;
    /**
     * 已学习时长（秒），从VideoStudyRecord汇总watchedDuration
     */
    private Integer watchedDuration;
    /**
     * 学习进度百分比（已学习时长 / 课程总时长 * 100）
     */
    private BigDecimal studyProgress;
}
