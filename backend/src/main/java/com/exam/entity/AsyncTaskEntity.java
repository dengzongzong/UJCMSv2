package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步任务(持久化)
 * - 服务重启后从 DB 加载,以便查看历史/重新下载结果
 * - 内存 ConcurrentHashMap 仍然保留作为热数据缓存
 */
@Data
@TableName("async_task")
public class AsyncTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String bizType;
    private String bizName;
    /** pending / running / success / failed / cancelled */
    private String status;
    private Integer progress;
    private Integer processed;
    private Integer total;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
    private String resultFilePath;
    private String resultFileName;
    private String extraJson;
    private String createdBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
