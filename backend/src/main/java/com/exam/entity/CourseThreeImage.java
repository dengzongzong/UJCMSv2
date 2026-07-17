package com.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程关联三图(视频下方三张宣传图,可点击跳转)
 */
@Data
@TableName("course_three_image")
public class CourseThreeImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联课程ID(NULL=全站通用) */
    private Long courseId;
    /** 图片标题/Alt */
    private String title;
    /** 描述文字 */
    private String description;
    /** 图片URL */
    private String imageUrl;
    /** 0-不跳转 1-跳转外链 2-跳转试卷 3-跳转课程 */
    private Integer linkType;
    /** 跳转URL(link_type=1 时使用) */
    private String linkUrl;
    /** 跳转目标ID(link_type=2/3 时使用) */
    private Long linkId;
    /** 显示顺序(小的在前) */
    private Integer sort;
    /** 0-禁用 1-启用 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
