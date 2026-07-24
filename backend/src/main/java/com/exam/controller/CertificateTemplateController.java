package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.CertificateTemplateDTO;
import com.exam.entity.CertificateTemplate;
import com.exam.service.CertificateTemplateService;
import com.exam.vo.CertificateTemplateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/certificate/template")
public class CertificateTemplateController {

    @Autowired
    private CertificateTemplateService templateService;

    @Autowired
    private com.exam.mapper.CertificateExportColumnMapper exportColumnMapper;

    @Autowired
    private com.exam.mapper.CertificateFieldMapper certificateFieldMapper;

    @GetMapping("/list")
    public Result<List<CertificateTemplate>> list() {
        return Result.success(templateService.listAll());
    }

    @GetMapping("/{id}")
    public Result<CertificateTemplateVO> detail(@PathVariable Long id) {
        return Result.success(templateService.detail(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody CertificateTemplateDTO dto) {
        templateService.save(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        templateService.setDefault(id);
        return Result.success();
    }

    /**
     * 获取模板的导出列配置
     */
    @GetMapping("/{id}/export-columns")
    public Result<List<java.util.Map<String, Object>>> getExportColumns(@PathVariable Long id) {
        // 查询该模板已配置的导出列
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.exam.entity.CertificateExportColumn> w =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        w.eq(com.exam.entity.CertificateExportColumn::getTemplateId, id)
         .orderByAsc(com.exam.entity.CertificateExportColumn::getSort);
        List<com.exam.entity.CertificateExportColumn> columns = exportColumnMapper.selectList(w);

        // 查询所有可用字段
        List<com.exam.entity.CertificateField> fields = certificateFieldMapper.selectList(null);

        // 构建返回数据: 已配置列 + 所有可用字段列表
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("configured", columns);
        result.put("available", fields);

        return Result.success(java.util.Arrays.asList(result));
    }

    /**
     * 保存模板的导出列配置 (整体替换)
     */
    @PutMapping("/{id}/export-columns")
    public Result<Void> saveExportColumns(@PathVariable Long id, @RequestBody List<com.exam.entity.CertificateExportColumn> columns) {
        // 先删除旧配置
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.exam.entity.CertificateExportColumn> w =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        w.eq(com.exam.entity.CertificateExportColumn::getTemplateId, id);
        exportColumnMapper.delete(w);

        // 保存新配置
        if (columns != null) {
            for (int i = 0; i < columns.size(); i++) {
                com.exam.entity.CertificateExportColumn col = columns.get(i);
                col.setId(null); // 确保是新增
                col.setTemplateId(id);
                col.setSort(i);
                exportColumnMapper.insert(col);
            }
        }
        return Result.success();
    }
}
