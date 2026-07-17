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
}
