package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_room")
public class LiveRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String coverUrl;
    private String anchorName;
    private String intro;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    /** 0-未开始 1-直播中 2-已结束 3-已取消 */
    private Integer status;
    private String streamName;
    private String pushUrl;
    private String playUrl;
    private String replayUrl;
    private Integer maxOnline;
    private Integer viewCount;
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 在线人数(非数据库字段, WebSocket维护) */
    @TableField(exist = false)
    private Integer onlineCount;
    /** 当前用户是否已开通该直播(非数据库字段, 用户端列表用) */
    @TableField(exist = false)
    private Boolean opened;
}
