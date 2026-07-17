package com.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台视频分页/详情 VO
 * <p>
 * 与 {@link VideoVO}(学生端播放信息)解耦,字段对齐管理端列表/编辑页需求。
 */
@Data
public class AdminVideoVO {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Long courseId;
    private String courseName;
    private Long professionId;
    private String professionName;
    private String url;
    private String coverUrl;
    private Integer duration;
    private Long size;
    private Integer playCount;
    /**
     * 基础学习人数(后台手工设置)
     */
    private Integer baseStudyCount;
    /**
     * 展示用学习人数 = baseStudyCount + playCount
     */
    private Integer studyCount;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
