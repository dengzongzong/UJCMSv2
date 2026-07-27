package com.exam.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.common.SpringContextHolder;
import com.exam.dto.CertificateDTO;
import com.exam.dto.CertificateImportResult;
import com.exam.dto.CertificateImportRow;
import com.exam.dto.CertificateIssueDTO;
import com.exam.common.AsyncTask;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateTemplate;
import com.exam.entity.CertificateNumberConfig;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.CertificateTemplateMapper;
import com.exam.mapper.CertificateNumberConfigMapper;
import com.exam.service.AsyncTaskService;
import com.exam.service.CertificateService;
import com.exam.service.CertificateTaskService;
import com.exam.service.impl.CertificateTaskServiceImpl;
import com.exam.vo.CertificateVO;
import com.exam.vo.CertificateUserExportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/admin/certificate")
public class CertificateController {

    @Autowired
    private CertificateService certificateService;
    @Autowired
    private CertificateTaskService certificateTaskService;
    @Autowired
    private CertificateMapper certificateMapper;
    @Autowired
    private CertificateTemplateMapper templateMapper;
    @Autowired
    private com.exam.service.CertificateGenerateService generateService;
    @Autowired
    private CertificateNumberConfigMapper numberConfigMapper;
    @Autowired
    private com.exam.service.CertificateNumberService numberService;
    @Autowired
    private com.exam.mapper.CertificateUrlConfigMapper urlConfigMapper;

    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) String idCard,
                                                @RequestParam(required = false) String agency,
                                                @RequestParam(required = false) String profession,
                                                @RequestParam(required = false) String importTimeStart,
                                                @RequestParam(required = false) String importTimeEnd,
                                                @RequestParam(required = false) Integer unboundTemplate,
                                                @RequestParam(required = false) String certType,
                                                @RequestParam(required = false) Integer exactCount) {
        // 精确显示条数: 仅返回最新N条(覆盖分页参数)
        if (exactCount != null && exactCount > 0) {
            page = 1;
            size = exactCount;
        }
        // 未绑定模板筛选:用大页查询后在内存中过滤 templateId 为空的
        // (certType 已下推到 SQL, 不再在内存中过滤)
        if (unboundTemplate != null && unboundTemplate == 1) {
            // 查全部数据(不分页),过滤 templateId 为空的
            int maxSize = 10000;
            PageResult<Map<String, Object>> all;
            if (profession == null || profession.isEmpty()) {
                all = certificateService.pageWithTemplateName(1, maxSize, name, idCard, agency, importTimeStart, importTimeEnd, certType);
            } else {
                all = certificateService.pageWithTemplateNameAndProfession(1, maxSize, name, idCard, agency, profession, importTimeStart, importTimeEnd, certType);
            }
            List<Map<String, Object>> filtered = new java.util.ArrayList<>();
            for (Map<String, Object> row : all.getRecords()) {
                Object tid = row.get("templateId");
                if (tid == null) {
                    filtered.add(row);
                }
            }
            // 手动分页
            int total = filtered.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            List<Map<String, Object>> pageRecords = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new java.util.ArrayList<>();
            PageResult<Map<String, Object>> result = new PageResult<>();
            result.setRecords(pageRecords);
            result.setTotal((long) total);
            return Result.success(result);
        }
        // 正常分页: certType 过滤已下推到 SQL(WHERE cert_type = ?), 不再查 1 万条再内存过滤
        if (profession == null || profession.isEmpty()) {
            return Result.success(certificateService.pageWithTemplateName(page, size, name, idCard, agency, importTimeStart, importTimeEnd, certType));
        }
        return Result.success(certificateService.pageWithTemplateNameAndProfession(page, size, name, idCard, agency, profession, importTimeStart, importTimeEnd, certType));
    }

    @GetMapping("/{id}")
    public Result<CertificateVO> detail(@PathVariable Long id) {
        return Result.success(certificateService.detail(id));
    }

    /**
     * 按列表筛选条件返回所有"已绑定模板"的证书 ID(用于批量下载全部)
     */
    @GetMapping("/all-ids")
    public Result<List<Long>> allIds(@RequestParam(required = false) String name,
                                     @RequestParam(required = false) String idCard,
                                     @RequestParam(required = false) String agency,
                                     @RequestParam(required = false) String profession,
                                     @RequestParam(required = false) String issueDateStart,
                                     @RequestParam(required = false) String issueDateEnd) {
        return Result.success(certificateService.listFilteredIdsWithTemplate(
                name, idCard, agency, profession, issueDateStart, issueDateEnd));
    }

    @PostMapping
    public Result<Void> add(@RequestBody CertificateDTO dto) {
        boolean created = certificateService.add(dto);
        if (!created) {
            return Result.error("该用户已存在相同专业和级别的证书记录,未重复创建");
        }
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody CertificateDTO dto) {
        certificateService.update(dto);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> delete(@RequestBody List<Long> ids) {
        certificateService.delete(ids);
        return Result.success();
    }

    /**
     * Excel 导入证书使用者(校验 + 入库 一体化)
     *
     * <p>行为(v2 改版,去掉 dry-run / commit 的两步流程):</p>
     * <ol>
     *   <li>同步解析 Excel + 同步校验每行(姓名/身份证/职业/编号等必填)</li>
     *   <li>若 0 行校验通过: 返 dryRun=false + failedRows, 提示用户修正 Excel</li>
     *   <li>若 validRows < 50: 同步入库(失败的 valid 行合并到 failedRows), 返 { successCount, failCount, failedRows }</li>
     *   <li>若 validRows >= 50: 异步入库, 返 { taskId, async: true, successCount, failCount }
     *       - 任务 SUCCESS = 入库全部完成
     *       - 任务的 resultFile = 失败行 Excel(可下载)
     *   </li>
     * </ol>
     *
     * <p>参数(原):dryRunToken 仍兼容 /import/commit(老用户 UI 未更新) 但新流程不再产出 token</p>
     */
    @PostMapping("/import")
    public Result<?> importExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        // 1) 解析 + 校验 (同步, 必然 O(n))
        CertificateImportResult parseResult = certificateService.parseExcel(file);
        List<CertificateImportRow> validRows = parseResult.getPendingRows();
        List<CertificateImportRow> failedRows = parseResult.getFailedRows();
        int parsedFailed = failedRows == null ? 0 : failedRows.size();
        int validCount = validRows == null ? 0 : validRows.size();

        // 2) 0 行有效: 告诉用户修 Excel
        if (validCount == 0) {
            CertificateImportResult r = new CertificateImportResult();
            r.setSuccessCount(0);
            r.setFailCount(parsedFailed);
            r.setFailedRows(failedRows);
            r.setDryRun(false);
            r.setPendingRows(null);
            return Result.success("未通过任何校验,请修正 Excel 后重传", r);
        }

        // 3) 同步 / 异步 分流(以 valid 数为阈值, 不是文件大小)
        int asyncThreshold = 50;
        if (validCount < asyncThreshold) {
            // 同步入库(走 service.commitImport 的同步路径)
            CertificateImportResult importResult = certificateService.commitImport(validRows, null);
            // 合并: 解析失败 + 入库失败
            List<CertificateImportRow> allFailed = new ArrayList<>();
            if (failedRows != null) allFailed.addAll(failedRows);
            if (importResult.getFailedRows() != null) allFailed.addAll(importResult.getFailedRows());
            int dbFailed = importResult.getFailCount() == null ? 0 : importResult.getFailCount();
            int success = validCount - dbFailed;
            importResult.setSuccessCount(success);
            importResult.setFailCount(allFailed.size());
            importResult.setFailedRows(allFailed);
            importResult.setDryRun(false);
            importResult.setPendingRows(null);
            return Result.success(
                    "导入完成:成功 " + success + " 条,失败 " + dbFailed + " 条",
                    importResult);
        }

        // 4) 异步入库(只做"入库"动作, 解析已在上面做完)
        String taskId = certificateTaskService.submitImportAndCommit(file);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("taskId", taskId);
        data.put("async", true);
        data.put("successCount", validCount);
        data.put("failCount", parsedFailed);
        data.put("failedRows", failedRows);
        return Result.success("已提交异步入库任务(" + validCount + " 条), 请在任务中心查看进度", data);
    }

    /**
     * 提交 dry-run 结果入库
     * - 同步路径: { dryRunToken: "..." } 或 { pendingRows: [...] }
     * - 异步路径: { taskId: "..." } 由任务中心走 commit
     */
    @PostMapping("/import/commit")
    public Result<?> commitImport(@RequestBody Map<String, Object> body) {
        Object tokenObj = body.get("dryRunToken");
        Object rowsObj = body.get("pendingRows");
        Object taskObj = body.get("taskId");
        String token = tokenObj == null ? null : tokenObj.toString();
        String taskId = taskObj == null ? null : taskObj.toString();
        List<CertificateImportRow> rows = null;
        if (rowsObj instanceof List) {
            rows = new ArrayList<>();
            for (Object o : (List<?>) rowsObj) {
                if (o instanceof Map) {
                    CertificateImportRow r = new CertificateImportRow();
                    Map<?, ?> m = (Map<?, ?>) o;
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        String k = String.valueOf(e.getKey());
                        Object v = e.getValue();
                        try {
                            java.lang.reflect.Field f = CertificateImportRow.class.getDeclaredField(k);
                            f.setAccessible(true);
                            f.set(r, v);
                        } catch (NoSuchFieldException nsf) {
                            // 未知字段(Excel 多了列):debug 级别记录
                            log.debug("导入 Excel 多出列(忽略): field={}", k);
                        } catch (IllegalAccessException iae) {
                            // 字段被 private/final 拦:warn,继续
                            log.warn("导入字段不可写(跳过): field={}, reason={}", k, iae.getMessage());
                        } catch (Exception ex) {
                            // 其它异常(类型不兼容等):记 warn
                            log.warn("导入行赋值失败,字段={}, 值={}, 原因={}", k, v, ex.getMessage());
                        }
                    }
                    rows.add(r);
                }
            }
        }
        // 异步路径: taskId 提供,需要从任务里拿 token 后再 commit
        if (taskId != null && (rows == null || rows.isEmpty()) && token == null) {
            com.exam.common.AsyncTask t = certificateService.getTaskById(taskId);
            if (t == null || t.getExtraJson() == null) {
                throw new BusinessException("任务不存在或已过期");
            }
            try {
                Map<?, ?> extra = new com.fasterxml.jackson.databind.ObjectMapper().readValue(t.getExtraJson(), Map.class);
                Object tk = extra.get("dryRunToken");
                token = tk == null ? null : tk.toString();
            } catch (Exception e) {
                throw new BusinessException("任务 token 解析失败: " + e.getMessage());
            }
            if (token == null) throw new BusinessException("该任务不包含 dry-run 数据");
            // 走异步 commit
            String newTaskId = certificateTaskService.submitCommitImport(token);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("taskId", newTaskId);
            data.put("async", true);
            return Result.success("已提交确认导入任务,请在任务中心查看结果", data);
        }
        return Result.success(certificateService.commitImport(rows, token));
    }

    /**
     * 下载模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        certificateService.downloadTemplate(response);
    }

    /**
     * 导出证书数据(Excel,按证书绑定的模板分组导出)
     * 支持按筛选条件导出全部,或按选中ID导出
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String name,
                       @RequestParam(required = false) String idCard,
                       @RequestParam(required = false) String agency,
                       @RequestParam(required = false) String profession,
                       @RequestParam(required = false) String issueDateStart,
                       @RequestParam(required = false) String issueDateEnd,
                       @RequestParam(required = false) List<Long> ids,
                       HttpServletResponse response) {
        certificateService.exportCertificates(response, name, idCard, agency, profession,
                issueDateStart, issueDateEnd, ids);
    }

    /**
     * 切换考试二维码启用状态
     * - 选择行数 < 50: 同步处理
     * - 选择行数 >= 50 或 allSelected=true: 异步处理
     */
    @PostMapping("/exam-qr/switch")
    public Result<?> switchExamQr(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("ids");
        Object allObj = body.get("all");
        Object enabledObj = body.get("enabled");
        Boolean all = allObj == null ? Boolean.FALSE : Boolean.valueOf(allObj.toString());
        Integer enabled = enabledObj == null ? null : Integer.valueOf(enabledObj.toString());
        if (enabled == null) throw new BusinessException("enabled 不能为空");
        List<Long> ids = null;
        if (idsObj instanceof List) {
            ids = new ArrayList<>();
            for (Object o : (List<?>) idsObj) {
                if (o != null) ids.add(Long.valueOf(o.toString()));
            }
        }
        // 走异步
        if (all || (ids != null && ids.size() >= CertificateTaskServiceImpl.ASYNC_THRESHOLD)) {
            String taskId = certificateTaskService.submitBatchSwitchExamQr(all ? null : ids, enabled);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("taskId", taskId);
            data.put("async", true);
            return Result.success("已提交异步任务", data);
        }
        certificateService.switchExamQr(ids, all, enabled);
        return Result.success();
    }

    /**
     * 为已存在的证书记录绑定模板 / 补盖章
     *
     * <p>请求:{ certificateIds:[1,2,3], templateId? }</p>
     *
     * <p>设计说明:
     * <ul>
     *   <li>证书使用者(姓名/身份证)是在『证书管理 → 新增证书』时录入的, 存在 certificate 表</li>
     *   <li>本接口不创建新的 cert 记录(那是『新增证书』做的事), 只对已存在的 cert 做模板绑定/补盖章</li>
     *   <li>对每个 certificateId: 不存在 -> failedDetails; cert 缺 idCard/name -> failedDetails; 其它 -> 成功(目前只是返回成功,模板走动态选)</li>
     * </ul>
     * </p>
     *
     * <p>返回:{ issuedCount, failedCount, failedDetails }</p>
     */
    @PostMapping("/issue")
    public Result<Map<String, Object>> issue(@RequestBody CertificateIssueDTO dto) {
        if (dto.getCertificateIds() == null || dto.getCertificateIds().isEmpty()) {
            throw new BusinessException("请至少选择一条证书记录");
        }

        // 解析模板(不传则用系统默认)
        CertificateTemplate template = null;
        if (dto.getTemplateId() != null) {
            template = templateMapper.selectById(dto.getTemplateId());
        }
        if (template == null) {
            template = templateMapper.selectOne(new LambdaQueryWrapper<CertificateTemplate>()
                    .eq(CertificateTemplate::getIsDefault, 1)
                    .last("LIMIT 1"));
        }
        if (template == null) {
            throw new BusinessException("请先在模板管理中创建证书模板");
        }

        // 逐个处理 cert 记录
        Set<Long> idSet = new LinkedHashSet<>(dto.getCertificateIds());
        List<Certificate> certs = certificateMapper.selectBatchIds(idSet);
        Map<Long, Certificate> certMap = new HashMap<>();
        for (Certificate c : certs) {
            certMap.put(c.getId(), c);
        }

        int issuedCount = 0;
        List<Map<String, Object>> failedDetails = new ArrayList<>();

        for (Long cid : idSet) {
            Certificate c = certMap.get(cid);
            if (c == null) {
                failedDetails.add(item(cid, "证书记录不存在"));
                continue;
            }
            // cert 必须有完整的"证书用户"信息(idCard+name)
            if (c.getIdCard() == null || c.getIdCard().isEmpty()
                    || c.getName() == null || c.getName().isEmpty()) {
                failedDetails.add(item(cid, "该证书记录缺少『姓名』或『身份证号』,请在证书编辑页补录"));
                continue;
            }
            // 真正写入 templateId(覆盖式绑定)
            try {
                // 绑定模板时生成证书编号(若尚未生成):证书编号只在绑定模板时才生成
                // 使用模板的 cert_no_prefix/cert_no_middle 配置(模板未配置时回落到全局编号配置)
                numberService.fillCertNoIfEmpty(c, template.getId());
                c.setTemplateId(template.getId());
                certificateMapper.updateById(c);
                issuedCount++;
                // 绑定时预渲染证书图片到缓存,后续预览/下载秒开(失败不影响绑定)
                try {
                    generateService.prerender(c, template);
                } catch (Exception pe) {
                    log.warn("证书预渲染失败(不影响绑定): certId={}, error={}", cid, pe.getMessage());
                }
            } catch (Exception e) {
                log.warn("绑定模板失败: certId={}, error={}", cid, e.getMessage());
                failedDetails.add(item(cid, "保存失败: " + e.getMessage()));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("issuedCount", issuedCount);
        data.put("failedCount", failedDetails.size());
        data.put("failedDetails", failedDetails);
        return Result.success("处理完成:成功 " + issuedCount + " 条", data);
    }

    private Map<String, Object> item(Long cid, String error) {
        Map<String, Object> m = new HashMap<>();
        m.put("certificateId", cid);
        m.put("error", error);
        return m;
    }

    // ==================== 编号配置 ====================

    @GetMapping("/number-config")
    public Result<CertificateNumberConfig> getNumberConfig() {
        List<CertificateNumberConfig> list = numberConfigMapper.selectList(null);
        if (list.isEmpty()) {
            CertificateNumberConfig config = new CertificateNumberConfig();
            config.setCertNoPrefix("ZGZH");
            config.setCertNoMiddle("M");
            config.setStudentNoPrefix("RCCP");
            config.setStudentNoMiddle("B");
            return Result.success(config);
        }
        return Result.success(list.get(0));
    }

    @PutMapping("/number-config")
    public Result<Void> updateNumberConfig(@RequestBody CertificateNumberConfig config) {
        List<CertificateNumberConfig> list = numberConfigMapper.selectList(null);
        if (list.isEmpty()) {
            numberConfigMapper.insert(config);
        } else {
            config.setId(list.get(0).getId());
            numberConfigMapper.updateById(config);
        }
        return Result.success();
    }

    // ==================== URL 配置(二维码生成规则) ====================

    @GetMapping("/url-config")
    public Result<com.exam.entity.CertificateUrlConfig> getUrlConfig() {
        List<com.exam.entity.CertificateUrlConfig> list = urlConfigMapper.selectList(null);
        if (list.isEmpty()) {
            return Result.success(new com.exam.entity.CertificateUrlConfig());
        }
        return Result.success(list.get(0));
    }

    @PutMapping("/url-config")
    public Result<Void> updateUrlConfig(@RequestBody com.exam.entity.CertificateUrlConfig config) {
        List<com.exam.entity.CertificateUrlConfig> list = urlConfigMapper.selectList(null);
        config.setUpdateTime(java.time.LocalDateTime.now());
        if (list.isEmpty()) {
            urlConfigMapper.insert(config);
        } else {
            config.setId(list.get(0).getId());
            urlConfigMapper.updateById(config);
        }
        return Result.success();
    }
}
