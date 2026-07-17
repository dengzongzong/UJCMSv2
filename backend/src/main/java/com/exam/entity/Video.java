package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video")
public class Video {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long categoryId;
    /**
     * 所属课程ID
     */
    private Long courseId;
    /**
     * 所属专业ID
     */
    private Long professionId;
    private String url;
    private String coverUrl;
    private Integer duration;
    private Long size;
    private Integer playCount;
    /**
     * 基础学习人数(后台手工设置,展示时 = baseStudyCount + playCount)
     */
    private Integer baseStudyCount;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    /**
     * 视频分类名称（非数据库字段，查询时手动填充）
     */
    @TableField(exist = false)
    private String categoryName;
    /**
     * 所属课程名称（非数据库字段，查询时手动填充）
     */
    @TableField(exist = false)
    private String courseName;
    /**
     * 所属专业名称（非数据库字段，查询时手动填充）
     */
    @TableField(exist = false)
    private String professionName;
}
