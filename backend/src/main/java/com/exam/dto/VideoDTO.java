package com.exam.dto;

import lombok.Data;

/**
 * 视频新增/编辑DTO
 */
@Data
public class VideoDTO {
    private Long id;
    private String name;
    private Long categoryId;
    /** 所属课程ID */
    private Long courseId;
    /** 所属专业ID */
    private Long professionId;
    private String url;
    private String coverUrl;
    /** 时长(秒) */
    private Integer duration;
    /** 大小(字节) */
    private Long size;
    /** 基础学习人数(后台手工设置) */
    private Integer baseStudyCount;
    private String remark;
}
