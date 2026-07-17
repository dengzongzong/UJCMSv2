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
     * - 数量 < 50: 同步流式下载(直接走 /batch-sync 的实现,避免前端再多一次跳转)
     * - 数量 >= 50: 异步任务,返回 { taskId, async: true }
     */
    @PostMapping("/batch")
    public Object batch(@RequestBody BatchRequest body, HttpServletResponse response) throws Exception {
        if (body.getIds() == null || body.getIds().isEmpty()) {
            throw new BusinessException("请选择证书");
        }
        if (body.getIds().size() >= CertificateTaskServiceImpl.ASYNC_THRESHOLD) {
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
        // 同步路径(< 50):直接走流式返回(等价于 /batch-sync)
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
        String fileName = "certificates_" + (pdf ? "pdf_" : "img_") + System.currentTimeMillis() + ".zip";
        prepareDownload(response, fileName, "application/zip");
        try (OutputStream os = response.getOutputStream()) {
            if (pdf) {
                generateService.renderBatchPdfToZip(certs, template, os);
            } else {
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
        String fileName = "certificates_" + (pdf ? "pdf_" : "img_") + System.currentTimeMillis() + ".zip";
        prepareDownload(response, fileName, "application/zip");
        try (OutputStream os = response.getOutputStream()) {
            if (pdf) {
                generateService.renderBatchPdfToZip(certs, template, os);
            } else {
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
        if (template == null) throw new BusinessException("请先在模板管理中创建证书模板");
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
        public List<Long> getIds() { return ids; }
        public void setIds(List<Long> ids) { this.ids = ids; }
        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
}
