package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 证书下载次数追踪表(按证书类型+日期+下载类型维度统计当天下载次数)
 */
@Data
@TableName("certificate_download_count")
public class CertificateDownloadCount {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 证书类型(如:专业技能、专项职业;为空时用"全部") */
    private String certType;
    /** 下载日期(仅日期,不含时间) */
    private LocalDate downloadDate;
    /** 下载类型: export=导出数据, batch_download=批量下载 */
    private String downloadKind;
    /** 当天已下载次数 */
    private Integer count;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
