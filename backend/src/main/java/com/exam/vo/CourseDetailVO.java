package com.exam.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程详情VO
 */
@Data
public class CourseDetailVO {
    private Long id;
    private String name;
    private String coverUrl;
    private String intro;
    private BigDecimal price;
    private String tag;
    private Integer totalDuration;
    private Integer sectionCount;
    /**
     * 当前学生是否已开通该课程
     */
    private Boolean purchased;
    /**
     * 是否需要登录(true=当前用户未登录)
     */
    private Boolean needLogin;
    private List<SectionVO> sections;
}
