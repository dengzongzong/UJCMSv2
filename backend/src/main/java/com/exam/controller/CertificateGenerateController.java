package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.common.Result;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateTemplate;
import com.exam.mapper.CertificateTemplateMapper;
import com.exam.service.CertificateGenerateService;
import com.exam.service.CertificateService;
import com.exam.service.CertificateTaskService;
import com.exam.service.impl.CertificateTaskServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 证书生成 / 下载
 * <p>
 * - 单张: 走同步流式下载
 * - 批量: < 50 张走同步, >= 50 张走异步任务(返回 taskId)
 */
@RestController
@RequestMapping("/admin/certificate/generate")
public class CertificateGenerateController {

    public static final String FORMAT_IMAGE = "image";
    public static final String FORMAT_PDF = "pdf";

    @Autowired
    private CertificateService certificateService;
    @Autowired
    private CertificateGenerateService generateService;
    @Autowired
    private CertificateTaskService certificateTaskService;
    @Autowired
    private CertificateTemplateMapper templateMapper;

    /**
     * 单个证书下载
     */
    @GetMapping("/single/{id}")
    public void single(@PathVariable Long id,
                       @RequestParam(required = false) Long templateId,
                       @RequestParam(defaultValue = FORMAT_IMAGE) String format,
                       HttpServletResponse response) throws Exception {
        Certificate cert = certificateService.getById(id);
        if (cert == null) throw new BusinessException("证书不存在");
        CertificateTemplate template = null;
        if (cert.getTemplateId() != null) {
            template = templateMapper.selectById(cert.getTemplateId());
        }
        if (template == null) {
            throw new BusinessException("该证书未绑定证书模板，无法生成");
        }
        if (FORMAT_PDF.equalsIgnoreCase(format)) {
            // 先渲染到内存,成功后再写响应(避免渲染异常导致响应已提交无法返回错误)
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            generateService.renderSinglePdf(cert, template, baos);
            prepareDownload(response, fileName(cert) + ".pdf", "application/pdf");
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        } else {
            // 先渲染图片到内存,成功后再写响应
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            generateService.renderSingle(cert, template, baos);
            prepareDownload(response, fileName(cert) + ".jpg", "image/jpeg");
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        }
    }

    /**
     * 批量下载
     * - forceAsync=true: 强制异步(用于"批量下载全部")
     * - 数量 >= 50: 异步任务,返回 { taskId, async: true }
     * - 数量 < 50: 同步流式下载
     */
    @PostMapping("/batch")
    public Object batch(@RequestBody BatchRequest body, HttpServletResponse response) throws Exception {
        if (body.getIds() == null || body.getIds().isEmpty()) {
            throw new BusinessException("请选择证书");
        }
        // forceAsync 或 数量 >= 阈值 -> 走异步
        boolean forceAsync = Boolean.TRUE.equals(body.getForceAsync());
        if (forceAsync || body.getIds().size() >= CertificateTaskServiceImpl.ASYNC_THRESHOLD) {
            // 预解析模板和证书
            List<Certificate> certs = certificateService.listByIds(body.getIds());
            if (certs == null || certs.isEmpty()) {
                throw new BusinessException("未找到证书");
            }
            // 过滤掉未绑定模板的证书
            certs = certs.stream().filter(c -> c.getTemplateId() != null).collect(Collectors.toList());
            if (certs.isEmpty()) {
                throw new BusinessException("选中的证书均未绑定模板，无法生成");
            }
            String taskId = certificateTaskService.submitBatchGenerate(certs, body.getFormat());
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("async", true);
            return Result.success("已提交批量生成任务,请到任务中心查看进度", data);
        }
        // 同步路径(< 50):直接走流式返回
        List<Certificate> certs = certificateService.listByIds(body.getIds());
        if (certs == null || certs.isEmpty()) {
            throw new BusinessException("未找到证书");
        }
        // 过滤掉未绑定模板的证书
        certs = certs.stream().filter(c -> c.getTemplateId() != null).collect(Collectors.toList());
        if (certs.isEmpty()) {
            throw new BusinessException("选中的证书均未绑定模板，无法生成");
        }
        CertificateTemplate template = resolveTemplate(body.getTemplateId());
        boolean pdf = FORMAT_PDF.equalsIgnoreCase(body.getFormat());
        // 使用新命名: 日期_证书类型(次数).pdf / 日期_证书类型(次数).zip
        String certType = certs.stream()
                .map(Certificate::getCertType)
                .filter(c -> c != null && !c.isEmpty())
                .findFirst().orElse(null);
        if (pdf) {
            // 批量 PDF: 所有证书合并到同一个 PDF(多页),下载文件为 .pdf 而非 zip
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            generateService.renderBatchPdf(certs, template, baos);
            String fileName = certificateService.buildDownloadFileName(certType, "batch_download", ".pdf");
            prepareDownload(response, fileName, "application/pdf");
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        } else {
            // 批量图片: 仍然打包为 zip
            String fileName = certificateService.buildDownloadFileName(certType, "batch_download", ".zip");
            prepareDownload(response, fileName, "application/zip");
            try (OutputStream os = response.getOutputStream()) {
                generateService.renderBatchToZip(certs, template, os);
            }
        }
        return null;
    }

    /**
     * 同步批量下载(流式响应)
     */
    @PostMapping("/batch-sync")
    public void batchSync(@RequestBody BatchRequest body, HttpServletResponse response) throws Exception {
        if (body.getIds() == null || body.getIds().isEmpty()) {
            throw new BusinessException("请选择证书");
        }
        List<Certificate> certs = certificateService.listByIds(body.getIds());
        if (certs == null || certs.isEmpty()) {
            throw new BusinessException("未找到证书");
        }
        // 过滤掉未绑定模板的证书
        certs = certs.stream().filter(c -> c.getTemplateId() != null).collect(Collectors.toList());
        if (certs.isEmpty()) {
            throw new BusinessException("选中的证书均未绑定模板，无法生成");
        }
        CertificateTemplate template = resolveTemplate(body.getTemplateId());
        boolean pdf = FORMAT_PDF.equalsIgnoreCase(body.getFormat());
        // 使用新命名: 日期_证书类型(次数).pdf / 日期_证书类型(次数).zip
        String certType = certs.stream()
                .map(Certificate::getCertType)
                .filter(c -> c != null && !c.isEmpty())
                .findFirst().orElse(null);
        if (pdf) {
            // 批量 PDF: 所有证书合并到同一个 PDF(多页),下载文件为 .pdf 而非 zip
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            generateService.renderBatchPdf(certs, template, baos);
            String fileName = certificateService.buildDownloadFileName(certType, "batch_download", ".pdf");
            prepareDownload(response, fileName, "application/pdf");
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
        } else {
            // 批量图片: 仍然打包为 zip
            String fileName = certificateService.buildDownloadFileName(certType, "batch_download", ".zip");
            prepareDownload(response, fileName, "application/zip");
            try (OutputStream os = response.getOutputStream()) {
                generateService.renderBatchToZip(certs, template, os);
            }
        }
    }

    private CertificateTemplate resolveTemplate(Long templateId) {
        CertificateTemplate template;
        if (templateId != null) {
            template = templateMapper.selectById(templateId);
        } else {
            template = templateMapper.selectOne(new LambdaQueryWrapper<CertificateTemplate>()
                    .eq(CertificateTemplate::getIsDefault, 1)
                    .last("LIMIT 1"));
            if (template == null) {
                template = templateMapper.selectOne(new LambdaQueryWrapper<CertificateTemplate>()
                        .orderByDesc(CertificateTemplate::getId)
                        .last("LIMIT 1"));
            }
        }
        // 不再抛异常: 如果没有系统默认模板,返回 null,各证书使用自己绑定的模板
        return template;
    }

    private void prepareDownload(HttpServletResponse response, String fileName, String contentType) {
        response.setContentType(contentType);
        response.setCharacterEncoding("utf-8");
        try {
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded);
        } catch (java.io.UnsupportedEncodingException ignore) {
            response.setHeader("Content-disposition", "attachment;filename=\"" + fileName + "\"");
        }
    }

    private String fileName(Certificate c) {
        String name = c.getName() == null ? "未命名" : c.getName();
        String id = c.getIdCard() == null ? "" : c.getIdCard();
        if (id.length() >= 6) id = id.substring(id.length() - 6);
        return name + "_" + id;
    }

    public static class BatchRequest {
        private List<Long> ids;
        private Long templateId;
        private String format;
        private Boolean forceAsync;
        public List<Long> getIds() { return ids; }
        public void setIds(List<Long> ids) { this.ids = ids; }
        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public Boolean getForceAsync() { return forceAsync; }
        public void setForceAsync(Boolean forceAsync) { this.forceAsync = forceAsync; }
    }
}
