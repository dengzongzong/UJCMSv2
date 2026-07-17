package com.exam.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.common.AsyncTask;
import com.exam.dto.CertificateDTO;
import com.exam.dto.CertificateImportRow;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateTemplate;
import com.exam.mapper.CertificateMapper;
import com.exam.service.AsyncTaskService;
import com.exam.service.CertificateGenerateService;
import com.exam.service.CertificateService;
import com.exam.service.CertificateTaskService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 证书模块异步任务
 * - 大批量操作走异步,避免阻塞 HTTP 请求
 * - 任务状态/进度/结果文件通过 AsyncTaskService 暴露
 */
@Service
public class CertificateTaskServiceImpl implements CertificateTaskService {

    private static final Logger log = LoggerFactory.getLogger(CertificateTaskServiceImpl.class);

    /** 触发异步的阈值,数据量超过此值走异步,否则同步处理 */
    public static final int ASYNC_THRESHOLD = 50;

    private final com.fasterxml.jackson.databind.ObjectMapper objMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Autowired
    private AsyncTaskService taskService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private CertificateMapper certificateMapper;
    @Autowired
    private com.exam.mapper.CertificateTemplateMapper templateMapper;
    @Autowired
    @Qualifier("certificateGenerateServiceImpl")
    private CertificateGenerateService generateService;

    @Override
    public String submitImport(MultipartFile file) {
        // 老路径保留(给老 UI 兜底),新流程改用 submitImportAndCommit
        return submitImportInternal(file, false);
    }

    @Override
    public String submitImportAndCommit(MultipartFile file) {
        return submitImportInternal(file, true);
    }

    private String submitImportInternal(MultipartFile file, boolean doCommit) {
        // 先快速读取总行数,用于进度展示
        // 为避免异步任务跨线程读 MultipartFile 出问题,先把文件落盘
        // 落盘文件保留到任务结束后(给"重试"按钮读取),由定时清理清理 N 天前的源文件
        File tempFile = null;
        try {
            tempFile = File.createTempFile("cert_import_", ".xlsx");
            try (OutputStream os = new FileOutputStream(tempFile)) {
                os.write(file.getBytes());
            }
        } catch (Exception e) {
            log.error("保存上传文件失败", e);
            throw new RuntimeException("保存上传文件失败: " + e.getMessage());
        }
        final File saved = tempFile;
        final long size = saved.length();
        final int total = Math.max(1, (int) (size / 200));
        final String bizType = doCommit ? "certificate-import-commit" : "certificate-import";
        final String bizName = doCommit ? "Excel 证书导入(同步入库)" : "Excel 证书导入(dry-run)";
        final String taskId = taskService.submit(
                bizType, bizName, total,
                task -> doImport(task, saved, total, doCommit));
        // 记录源文件路径(给"重试"使用)
        com.exam.common.AsyncTask created = taskService.get(taskId);
        if (created != null) {
            created.setExtraJson(buildSourceFileExtra(saved.getAbsolutePath()));
            taskService.flushTaskExtra(created);
        }
        return taskId;
    }

    private String buildSourceFileExtra(String filePath) {
        try {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("sourceFilePath", filePath);
            return objMapper.writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String submitBatchGenerate(List<Certificate> certs, String format) {
        final int total = certs == null ? 0 : certs.size();
        final String fmt = format == null ? "image" : format;
        return taskService.submit(
                "certificate-batch-generate",
                "批量生成证书(" + (fmt.equalsIgnoreCase("pdf") ? "PDF" : "图片") + ")",
                total,
                task -> doBatchGenerate(task, certs, fmt));
    }

    @Override
    public String submitBatchSwitchExamQr(List<Long> ids, int enabled) {
        // 先查询总数量
        int total;
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            w.in(Certificate::getId, ids);
        }
        total = certificateMapper.selectCount(w).intValue();
        final int v = enabled;
        return taskService.submit(
                "exam-qr-batch",
                enabled == 1 ? "批量开启考试二维码" : "批量关闭考试二维码",
                total,
                task -> doBatchSwitchExamQr(task, ids, v, total));
    }

    @Override
    public String submitCommitImport(String dryRunToken) {
        return taskService.submit(
                "certificate-import-commit",
                "证书确认导入",
                0,
                task -> doCommitImport(task, dryRunToken));
    }

    @Override
    public String retry(String originalTaskId) {
        com.exam.common.AsyncTask orig = taskService.get(originalTaskId);
        if (orig == null) throw new RuntimeException("任务不存在或已过期");
        if (!com.exam.common.AsyncTask.STATUS_FAILED.equals(orig.getStatus())
                && !com.exam.common.AsyncTask.STATUS_CANCELLED.equals(orig.getStatus())) {
            throw new RuntimeException("只有失败/已取消的任务才能重试");
        }
        String bizType = orig.getBizType();
        if (bizType == null) throw new RuntimeException("任务类型为空");
        switch (bizType) {
            case "certificate-import": {
                // 重试: 重新读源文件再走一次 dry-run
                String src = taskService.getSourceFilePath(orig);
                if (src == null) throw new RuntimeException("源文件路径已丢失(可能已被清理),无法重试。请重新上传 Excel");
                File f = new File(src);
                if (!f.exists()) throw new RuntimeException("源文件已不存在: " + src + ",请重新上传 Excel");
                long size = f.length();
                final int total = Math.max(1, (int) (size / 200));
                final String newTaskId = taskService.submit(
                        "certificate-import",
                        "Excel 证书导入(重试)",
                        total,
                        task -> doImport(task, f, total, false));  // 重试 = dry-run,用户确认后再入库
                // 写源文件路径到 extraJson
                com.exam.common.AsyncTask created = taskService.get(newTaskId);
                if (created != null) {
                    created.setExtraJson(buildSourceFileExtra(src));
                    taskService.flushTaskExtra(created);
                }
                return newTaskId;
            }
            case "certificate-import-commit": {
                // 重试 commit: 需要 dryRunToken
                if (orig.getExtraJson() == null) throw new RuntimeException("任务上下文丢失,无法重试");
                try {
                    java.util.Map<?, ?> extra = objMapper.readValue(orig.getExtraJson(), java.util.Map.class);
                    Object tk = extra == null ? null : extra.get("dryRunToken");
                    if (tk == null) throw new RuntimeException("任务 token 已丢失,无法重试");
                    return submitCommitImport(tk.toString());
                } catch (Exception e) {
                    throw new RuntimeException("任务上下文解析失败: " + e.getMessage());
                }
            }
            case "certificate-batch-generate": {
                // 重试批量生成: 重新查询当前 certs
                // 注意: 这种任务没有持久的 cert 列表(任务结束时 certs 变量已释放)
                // 兜底: 从 bizType 推断用户意图,要求前端传 ids
                throw new RuntimeException("批量生成任务暂不支持直接重试,请在证书列表中重新选择并下载");
            }
            case "exam-qr-batch": {
                // 重试开关考试二维码: 重新按"全量+原 enabled"再跑一次
                String bizName = orig.getBizName() == null ? "" : orig.getBizName();
                int v = bizName.contains("开启") ? 1 : 0;
                return submitBatchSwitchExamQr(null, v);
            }
            default:
                throw new RuntimeException("暂不支持该任务类型的重试: " + bizType);
        }
    }

    // ============== 实际任务处理器 ==============

    private void doImport(AsyncTask task, File file, int total, boolean doCommit) {
        // 两种模式:
        // - doCommit=false: dry-run 模式,只解析不入库,把 token 放到 extraJson(老 UI 兜底)
        // - doCommit=true:  直接入库,边入库边推进度,失败行导出到 resultFile
        // 源文件保留(给"重试"使用),由定时清理清理 N 天前的文件
        List<CertificateImportRow> failed = new ArrayList<>();
        List<CertificateDTO> validDtos = new ArrayList<>();
        int[] processed = {0};
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            EasyExcel.read(fis, new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> row, AnalysisContext ctx) {
                    if (taskService.isCancelled(task.getTaskId())) {
                        throw new RuntimeException("__TASK_CANCELLED__");
                    }
                    if (row == null) return;
                    try {
                        CertificateImportRow r = certificateService.parseImportRow(row, ctx.readRowHolder().getRowIndex());
                        if (r.getError() != null) {
                            failed.add(r);
                        } else {
                            validDtos.add(certificateService.toImportDto(r));
                        }
                    } catch (Exception ignore) { /* parse 内部已 setError */ }
                    processed[0]++;
                    if (total > 0) {
                        task.setProgress(processed[0] * 100 / total);
                        task.setProcessed(processed[0]);
                    }
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext ctx) { /* noop */ }
            }).sheet().doRead();
        } catch (Exception e) {
            if (!String.valueOf(e.getMessage()).contains("__TASK_CANCELLED__")) {
                log.error("异步 dry-run 失败", e);
                throw new RuntimeException(e.getMessage() == null ? "解析失败" : e.getMessage());
            }
        }
        // 注意:不删除源文件,留给"重试"使用
        task.setProcessed(processed[0]);

        // 入库(仅 doCommit=true 模式)
        List<CertificateImportRow> dbFailed = new ArrayList<>();
        if (doCommit && !validDtos.isEmpty()) {
            int totalToInsert = validDtos.size();
            int ok = 0;
            for (int i = 0; i < validDtos.size(); i++) {
                if (taskService.isCancelled(task.getTaskId())) {
                    task.setStatus(AsyncTask.STATUS_CANCELLED);
                    return;
                }
                CertificateDTO d = validDtos.get(i);
                try {
                    boolean created = certificateService.add(d);
                    if (created) ok++;
                    // add 返回 false = 去重跳过,不计入成功
                } catch (Exception e) {
                    log.warn("证书入库失败: name={}, idCard={}, reason={}", d.getName(), d.getIdCard(), e.getMessage());
                    CertificateImportRow fr = certificateService.toImportDtoForRow(d);
                    fr.setError(e.getMessage());
                    dbFailed.add(fr);
                }
                task.setProcessed(processed[0] + i + 1);
                int totalWork = processed[0] + totalToInsert;
                task.setProgress(Math.min(100, (processed[0] + i + 1) * 100 / Math.max(1, totalWork)));
                task.setSuccessCount(ok);
                task.setFailCount(failed.size() + dbFailed.size());
            }
        }

        // 计数(doCommit 模式: 真正入库后的成功/失败)
        if (doCommit) {
            task.setSuccessCount(validDtos.size() - dbFailed.size());
            task.setFailCount(failed.size() + dbFailed.size());
        } else {
            // dry-run: 成功数 = 校验通过行数,失败数 = 解析失败行数
            task.setSuccessCount(validDtos.size());
            task.setFailCount(failed.size());
        }
        task.setProgress(100);

        // 合并 extraJson: sourceFilePath(给"重试"使用) + dryRunToken(老 UI 兜底, 仅 doCommit=false) + 失败行摘要
        java.util.Map<String, Object> extra = new java.util.LinkedHashMap<>();
        // 先读取原 extra(可能已含 sourceFilePath)
        if (task.getExtraJson() != null) {
            try {
                java.util.Map<?, ?> orig = objMapper.readValue(task.getExtraJson(), java.util.Map.class);
                if (orig != null) {
                    Object p = orig.get("sourceFilePath");
                    if (p != null) extra.put("sourceFilePath", p.toString());
                }
            } catch (Exception ignore) { /* ignore */ }
        }
        if (file != null) extra.put("sourceFilePath", file.getAbsolutePath());

        if (!doCommit && !validDtos.isEmpty()) {
            // dry-run 模式: 写 token
            try {
                List<CertificateImportRow> pending = new ArrayList<>();
                for (CertificateDTO d : validDtos) {
                    pending.add(certificateService.toImportDtoForRow(d));
                }
                String token = Base64.getUrlEncoder().withoutPadding().encodeToString(
                        objMapper.writeValueAsString(pending).getBytes(StandardCharsets.UTF_8));
                extra.put("dryRunToken", token);
                extra.put("dryRun", true);
            } catch (Exception e) {
                log.warn("编码 dry-run token 失败", e);
            }
        } else if (doCommit) {
            // doCommit 模式: 标记"已入库", 不写 token
            extra.put("dryRun", false);
            extra.put("committed", true);
        }
        try {
            task.setExtraJson(objMapper.writeValueAsString(extra));
            taskService.flushTaskExtra(task);
        } catch (Exception e) {
            log.warn("刷新任务 extraJson 失败", e);
        }
        // 失败行 Excel 仍然可下载(包含: 解析失败 + 入库失败)
        List<CertificateImportRow> allFailed = new ArrayList<>();
        allFailed.addAll(failed);
        allFailed.addAll(dbFailed);
        if (!allFailed.isEmpty()) {
            try {
                File failedFile = writeFailedExcel(allFailed);
                task.setResultFile(failedFile);
                task.setResultFileName(
                        (doCommit ? "证书导入失败行_" : "证书导入失败行_") +
                                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
            } catch (Exception e) {
                log.warn("生成失败 Excel 失败", e);
            }
        }
    }

    private void doBatchGenerate(AsyncTask task, List<Certificate> certs, String format) {
        if (certs == null || certs.isEmpty()) {
            task.setProgress(100);
            return;
        }
        // 批量共用系统默认模板(templateId 由 controller 解析后传入,这里只是兜底)
        CertificateTemplate template = pickDefaultTemplate(certs);
        if (template == null) {
            throw new RuntimeException("未找到可用的证书模板,请先在模板管理中创建或设置默认模板");
        }
        int total = certs.size();
        int ok = 0, fail = 0;
        File zipFile = new File(System.getProperty("java.io.tmpdir"),
                "cert_batch_" + System.currentTimeMillis() + ".zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zip = new ZipOutputStream(fos)) {
            for (int i = 0; i < certs.size(); i++) {
                if (taskService.isCancelled(task.getTaskId())) {
                    task.setStatus(AsyncTask.STATUS_CANCELLED);
                    break;
                }
                Certificate c = certs.get(i);
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if ("pdf".equalsIgnoreCase(format)) {
                        generateService.renderSinglePdf(c, template, baos);
                    } else {
                        generateService.renderSingle(c, template, baos);
                    }
                    String name = fileNameOf(c);
                    String ext = "pdf".equalsIgnoreCase(format) ? "pdf" : "png";
                    zip.putNextEntry(new ZipEntry(name + "." + ext));
                    zip.write(baos.toByteArray());
                    zip.closeEntry();
                    ok++;
                } catch (Exception e) {
                    fail++;
                    log.warn("证书 {} 生成失败: {}", c.getId(), e.getMessage());
                }
                int processed = i + 1;
                task.setProcessed(processed);
                task.setProgress(processed * 100 / total);
                task.setSuccessCount(ok);
                task.setFailCount(fail);
            }
        } catch (Exception e) {
            throw new RuntimeException("批量生成失败: " + e.getMessage(), e);
        }
        if (AsyncTask.STATUS_RUNNING.equals(task.getStatus())) {
            task.setResultFile(zipFile);
            task.setResultFileName("certificates_" +
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip");
        } else {
            // 取消则删除临时文件
            zipFile.delete();
        }
    }

    private void doCommitImport(AsyncTask task, String dryRunToken) {
        try {
            // 1. token -> List<ImportRow>
            byte[] decoded = Base64.getUrlDecoder().decode(dryRunToken);
            String json = new String(decoded, StandardCharsets.UTF_8);
            List<CertificateImportRow> rows = objMapper.readValue(json, new TypeReference<List<CertificateImportRow>>() {});
            if (rows == null || rows.isEmpty()) {
                task.setProgress(100);
                return;
            }
            task.setTotal(rows.size());
            // 2. 边入库边推进度
            int ok = 0, fail = 0;
            List<CertificateImportRow> failed = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                if (taskService.isCancelled(task.getTaskId())) {
                    task.setStatus(AsyncTask.STATUS_CANCELLED);
                    return;
                }
                CertificateImportRow row = rows.get(i);
                try {
                    boolean created = certificateService.add(certificateService.toImportDto(row));
                    if (created) ok++;
                    // add 返回 false = 去重跳过,不计入成功
                } catch (Exception e) {
                    fail++;
                    row.setError(e.getMessage());
                    failed.add(row);
                }
                int processed = i + 1;
                task.setProcessed(processed);
                task.setProgress(processed * 100 / rows.size());
                task.setSuccessCount(ok);
                task.setFailCount(fail);
            }
            // 3. 把失败行导出
            if (!failed.isEmpty()) {
                try {
                    File failedFile = writeFailedExcel(failed);
                    task.setResultFile(failedFile);
                    task.setResultFileName("证书确认导入失败行_" +
                            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
                } catch (Exception e) {
                    log.warn("生成失败 Excel 失败", e);
                }
            }
        } catch (Exception e) {
            log.error("commit 失败", e);
            throw new RuntimeException("token 无效或已损坏: " + e.getMessage());
        }
    }

    private void doBatchSwitchExamQr(AsyncTask task, List<Long> ids, int enabled, int total) {
        if (total == 0) {
            task.setProgress(100);
            return;
        }
        // 分批: 每 200 条一次,共 total/200 批
        int batchSize = 200;
        int processed = 0;
        if (ids != null && !ids.isEmpty()) {
            for (int i = 0; i < ids.size(); i += batchSize) {
                if (taskService.isCancelled(task.getTaskId())) {
                    task.setStatus(AsyncTask.STATUS_CANCELLED);
                    return;
                }
                List<Long> sub = ids.subList(i, Math.min(i + batchSize, ids.size()));
                certificateMapper.update(null, new LambdaUpdateWrapper<Certificate>()
                        .in(Certificate::getId, sub)
                        .set(Certificate::getExamQrEnabled, enabled)
                        .set(Certificate::getUpdateTime, LocalDateTime.now()));
                processed += sub.size();
                task.setProcessed(processed);
                task.setProgress(processed * 100 / total);
            }
        } else {
            // 全量更新(分页)
            int pageNo = 1;
            int pageSize = 500;
            while (true) {
                if (taskService.isCancelled(task.getTaskId())) {
                    task.setStatus(AsyncTask.STATUS_CANCELLED);
                    return;
                }
                com.baomidou.mybatisplus.extension.plugins.pagination.Page<Certificate> p =
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNo, pageSize);
                List<Certificate> records = certificateMapper.selectPage(p, new LambdaQueryWrapper<>()).getRecords();
                if (records == null || records.isEmpty()) break;
                List<Long> subIds = new ArrayList<>();
                for (Certificate c : records) subIds.add(c.getId());
                certificateMapper.update(null, new LambdaUpdateWrapper<Certificate>()
                        .in(Certificate::getId, subIds)
                        .set(Certificate::getExamQrEnabled, enabled)
                        .set(Certificate::getUpdateTime, LocalDateTime.now()));
                processed += subIds.size();
                task.setProcessed(processed);
                task.setProgress(processed * 100 / total);
                if (records.size() < pageSize) break;
                pageNo++;
            }
        }
        task.setSuccessCount(processed);
        task.setProgress(100);
    }

    // ============== 辅助方法 ==============

    /**
     * 选取默认模板(支持传入多张证书但共用同一模板的批量场景)
     */
    private CertificateTemplate pickDefaultTemplate(List<Certificate> certs) {
        if (templateMapper == null) return null;
        return templateMapper.selectOne(new LambdaQueryWrapper<CertificateTemplate>()
                .eq(CertificateTemplate::getIsDefault, 1)
                .last("LIMIT 1"));
    }

    private File writeFailedExcel(List<CertificateImportRow> failed) throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir"),
                "cert_import_failed_" + System.currentTimeMillis() + ".xlsx");
        List<List<String>> head = new ArrayList<>();
        head.add(Arrays.asList("行号"));
        head.add(Arrays.asList("姓名"));
        head.add(Arrays.asList("身份证"));
        head.add(Arrays.asList("错误原因"));
        List<List<Object>> data = new ArrayList<>();
        for (CertificateImportRow r : failed) {
            List<Object> row = new ArrayList<>();
            row.add(r.getRowIndex());
            row.add(r.getName());
            row.add(r.getIdCard());
            row.add(r.getError());
            data.add(row);
        }
        try (OutputStream os = new FileOutputStream(tmp)) {
            EasyExcel.write(os).head(head).sheet("失败行").doWrite(data);
        }
        return tmp;
    }

    private String fileNameOf(Certificate c) {
        String name = c.getName() == null ? "未命名" : c.getName();
        String id = c.getIdCard() == null ? "" : c.getIdCard();
        if (id.length() >= 6) id = id.substring(id.length() - 6);
        return name + "_" + id;
    }
}
