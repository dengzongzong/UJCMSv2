package com.exam.service;

import com.exam.dto.CertificateDTO;
import com.exam.entity.Certificate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 证书相关的异步任务统一入口
 * - Excel 异步导入
 * - 批量异步生成证书(图片/PDF)
 * - 批量异步开启考试二维码
 */
public interface CertificateTaskService {

    /**
     * 异步导入 Excel(适用于数据量 > 50)
     *
     * @return 任务 ID
     */
    String submitImport(MultipartFile file);

    /**
     * 异步导入并入库(校验通过的行直接入库,任务 SUCCESS = 全部入库完成)
     * <p>适用场景: 校验已经通过,但 valid rows >= 50 走异步入库,失败行可下载</p>
     */
    String submitImportAndCommit(MultipartFile file);

    /**
     * 异步批量生成证书并打包
     *
     * @param certs      证书列表
     * @param format     image / pdf
     * @return 任务 ID
     */
    String submitBatchGenerate(List<Certificate> certs, String format);

    /**
     * 异步批量开启/关闭考试二维码
     *
     * @param ids      证书 ID 列表(为空则全量)
     * @param enabled  0-关 1-开
     * @return 任务 ID
     */
    String submitBatchSwitchExamQr(List<Long> ids, int enabled);

    /**
     * 异步提交 dry-run token 入库(由任务中心"确认导入"按钮调用)
     *
     * @param dryRunToken 前端传来的 dryRunToken
     * @return 任务 ID
     */
    String submitCommitImport(String dryRunToken);

    /**
     * 重试失败的任务(根据原 taskId)
     * - 行为: 重新提交一个等价的任务,返回新 taskId
     * - 失败原因: 临时性问题(网络抖动/服务重启)可重试
     * - 不支持的 bizType 返回 null
     */
    String retry(String originalTaskId);
}
