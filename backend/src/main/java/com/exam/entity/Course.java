package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
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
    private Long subjectId;
    /** 课程分类ID(关联video_category表) */
    private Long categoryId;
    /** 基础学过人数(后台手工设置, 展示=基数+实际开通人数) */
    private Integer baseStudyCount;
    /** 基础学时(小时, 后台手工设置, 无单位) */
    private Integer baseStudyHours;
    /** 课程排序(越小越靠前) */
    private Integer sort;
    /** 是否置顶 0-否 1-是 */
    private Integer isTop;
    /** 置顶排序(越小越靠前) */
    private Integer topSort;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
