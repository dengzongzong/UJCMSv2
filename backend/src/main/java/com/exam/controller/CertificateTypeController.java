package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.CertificateType;
import com.exam.service.CertificateTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 证书类型管理Controller
 */
@RestController
@RequestMapping("/admin/certificate-type")
public class CertificateTypeController {

    @Autowired
    private CertificateTypeService certificateTypeService;

    /**
     * 查询全部证书类型(管理端)
     */
    @GetMapping("/list")
    public Result<List<CertificateType>> list() {
        return Result.success(certificateTypeService.listAll());
    }

    /**
     * 新增
     */
    @PostMapping
    public Result<Void> add(@RequestBody CertificateType type) {
        certificateTypeService.addType(type);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping
    public Result<Void> update(@RequestBody CertificateType type) {
        certificateTypeService.updateType(type);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        certificateTypeService.deleteType(id);
        return Result.success();
    }
}
