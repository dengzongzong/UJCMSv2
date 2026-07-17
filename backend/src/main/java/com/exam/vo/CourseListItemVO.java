package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程列表项VO
 */
@Data
public class CourseListItemVO {
    private Long id;
    private String name;
    private String coverUrl;
    private String tag;
    private BigDecimal price;
    private Integer sectionCount;
    /** 课程总学时(分钟) */
    private Integer totalDuration;
    /** 已学过该课程的人数(基数+实际开通人数) */
    private Integer studyCount;
    /** 学时(小时, 无单位) */
    private Integer studyHours;
    /**
     * 课程分类ID(关联video_category表)
     */
    private Long categoryId;
    /**
     * 课程分类名称
     */
    private String categoryName;
    /**
     * 学习进度百分比（已观看视频数/总视频数）
     */
    private Integer progress;
    /**
     * 当前学生是否已开通该课程(true=已开通 可学习;false=未开通 点击要校验)
     */
    private Boolean purchased;
}
