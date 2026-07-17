package com.exam.common;

import com.exam.entity.AsyncTaskEntity;
import lombok.Data;

import java.io.File;
import java.time.LocalDateTime;

/**
 * 异步任务实体(运行时态,内存/DB 共用)
 * 用于追踪 Excel 批量导入、批量证书生成、批量二维码处理等长耗时操作
 * - 字段命名与 AsyncTaskEntity 保持一致,便于互转
 * - resultFile 是运行时引用,持久化时存的是磁盘路径
 */
@Data
public class AsyncTask {
    /** 任务 ID,UUID */
    private String taskId;
    /** 业务类型: certificate-import / certificate-batch-generate / exam-qr-batch */
    private String bizType;
    /** 业务名称(展示用) */
    private String bizName;
    /** pending / running / success / failed / cancelled */
    private String status;
    /** 进度 0-100 */
    private Integer progress;
    /** 已处理数量 */
    private Integer processed;
    /** 总数量 */
    private Integer total;
    /** 成功数量 */
    private Integer successCount;
    /** 失败数量 */
    private Integer failCount;
    /** 任务开始时间 */
    private LocalDateTime startTime;
    /** 任务结束时间 */
    private LocalDateTime endTime;
    /** 错误信息 */
    private String errorMessage;
    /** 结果文件(批量生成时为 zip,导入时为失败 Excel) */
    private File resultFile;
    /** 结果文件磁盘路径(用于持久化) */
    private String resultFilePath;
    /** 结果文件名(下载用) */
    private String resultFileName;
    /** 任务发起人(可选) */
    private String createdBy;
    /** 任务创建时间(等于首次入库时间) */
    private LocalDateTime createTime;
    /** 业务扩展 JSON(import 任务可放 dryRunToken/sourceFilePath,用于前端"确认导入"和"重试") */
    private String extraJson;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELLED = "cancelled";

    /**
     * 从 DB 实体恢复运行时态
     */
    public static AsyncTask fromEntity(AsyncTaskEntity e) {
        if (e == null) return null;
        AsyncTask t = new AsyncTask();
        t.setTaskId(e.getTaskId());
        t.setBizType(e.getBizType());
        t.setBizName(e.getBizName());
        t.setStatus(e.getStatus());
        t.setProgress(e.getProgress());
        t.setProcessed(e.getProcessed());
        t.setTotal(e.getTotal());
        t.setSuccessCount(e.getSuccessCount());
        t.setFailCount(e.getFailCount());
        t.setErrorMessage(e.getErrorMessage());
        t.setResultFilePath(e.getResultFilePath());
        t.setResultFileName(e.getResultFileName());
        t.setCreatedBy(e.getCreatedBy());
        t.setStartTime(e.getStartTime());
        t.setEndTime(e.getEndTime());
        t.setCreateTime(e.getCreateTime());
        t.setExtraJson(e.getExtraJson());
        // 尝试加载结果文件
        if (e.getResultFilePath() != null) {
            File f = new File(e.getResultFilePath());
            if (f.exists()) t.setResultFile(f);
        }
        return t;
    }

    /**
     * 转为 DB 实体(用于入库)
     */
    public AsyncTaskEntity toEntity() {
        AsyncTaskEntity e = new AsyncTaskEntity();
        e.setTaskId(this.taskId);
        e.setBizType(this.bizType);
        e.setBizName(this.bizName);
        e.setStatus(this.status);
        e.setProgress(this.progress);
        e.setProcessed(this.processed);
        e.setTotal(this.total);
        e.setSuccessCount(this.successCount);
        e.setFailCount(this.failCount);
        e.setErrorMessage(this.errorMessage);
        e.setResultFilePath(this.resultFile != null ? this.resultFile.getAbsolutePath() : this.resultFilePath);
        e.setResultFileName(this.resultFileName);
        e.setCreatedBy(this.createdBy);
        e.setStartTime(this.startTime);
        e.setEndTime(this.endTime);
        e.setCreateTime(this.createTime);
        e.setExtraJson(this.extraJson);
        return e;
    }
}

