package com.exam.dto;

import lombok.Data;

/**
 * 视频播放进度上报DTO
 */
@Data
public class VideoProgressDTO {
    private Long videoId;
    private Long courseId;
    /**
     * 当前播放进度（秒）
     */
    private Integer progress;
    /**
     * 本次观看时长（秒）
     */
    private Integer watchedDuration;
}
