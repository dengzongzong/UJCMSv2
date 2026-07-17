package com.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台课程分页/详情 VO
 * <p>
 * 与 {@link CourseListItemVO} / {@link CourseDetailVO}(学生端)解耦。
 */
@Data
public class AdminCourseVO {
    private Long id;
    private String name;
    private String coverUrl;
    private String intro;
    private BigDecimal price;
    private String tag;
    private Integer totalDuration;
    private Integer sectionCount;
    private Integer status;
    private Long professionId;
    private String professionName;
    private Long subjectId;
    private String subjectName;
    /** 课程分类ID(关联video_category表) */
    private Long categoryId;
    /** 课程分类名称 */
    private String categoryName;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
