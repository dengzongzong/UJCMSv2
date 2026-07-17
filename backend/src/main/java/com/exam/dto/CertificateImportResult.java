package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * Excel 导入结果
 * - dryRun=false: successCount/failCount 是真正入库后的统计,failedRows 是入库失败的行
 * - dryRun=true:  successCount/failCount 是 dry-run 校验结果;pendingRows 是待入库的行(可由 commitImport 真正写入)
 */
@Data
public class CertificateImportResult {
    /** 成功条数(校验通过 / 入库成功) */
    private Integer successCount;
    /** 失败条数 */
    private Integer failCount;
    /** 失败行明细(回传给前端) */
    private List<CertificateImportRow> failedRows;
    /** dry-run 模式下,待入库的行(校验通过) */
    private List<CertificateImportRow> pendingRows;
    /** 是否为 dry-run(只解析未入库) */
    private Boolean dryRun;
    /** dry-run 时把 pendingRows 编码,前端确认时回传 */
    private String dryRunToken;
}
