package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_message")
public class LiveMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long liveId;
    private Long studentId;
    private String nickname;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
