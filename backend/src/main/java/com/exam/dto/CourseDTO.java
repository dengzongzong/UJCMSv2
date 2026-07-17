package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 课程新增/编辑DTO
 */
@Data
public class CourseDTO {
    private Long id;
    private String name;
    private String coverUrl;
    private String intro;
    private java.math.BigDecimal price;
    private String tag;
    private Long professionId;
    private Long subjectId;
    /** 课程分类ID(关联video_category表) */
    private Long categoryId;
    /** 基础学过人数(后台手工设置) */
    private Integer baseStudyCount;
    /** 基础学时(小时) */
    private Integer baseStudyHours;
    /** 排序号(越小越靠前) */
    private Integer sort;
    /** 是否置顶 0-否 1-是 */
    private Integer isTop;
    /** 置顶排序(越小越靠前) */
    private Integer topSort;
    private Integer status;
    /** 小节列表 */
    private List<SectionDTO> sections;
}
