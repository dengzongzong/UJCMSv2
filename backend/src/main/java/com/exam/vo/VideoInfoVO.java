package com.exam.vo;

import lombok.Data;

/**
 * 视频播放信息VO
 */
@Data
public class VideoInfoVO {
    private Long id;
    private String name;
    private String url;
    private String coverUrl;
    /**
     * 视频总时长（秒）
     */
    private Integer duration;
    /**
     * 当前播放进度（秒）
     */
    private Integer progress;
}
