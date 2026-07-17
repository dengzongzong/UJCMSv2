package com.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频VO
 */
@Data
public class VideoVO {
    private Long id;
    private String name;
    private String url;
    private String coverUrl;
    private Integer duration;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    /**
     * 学习进度（秒）
     */
    private Integer progress;
    /**
     * 查看权限 0-所有已开通用户可看 1-需指定权限
     */
    private Integer viewPermission;
}
