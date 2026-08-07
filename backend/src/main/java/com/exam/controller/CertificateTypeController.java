package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.CertificateType;
import com.exam.service.CertificateTypeService;
import com.exam.service.AdminScopeService;
import com.exam.annotation.RequireSuper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 证书类型管理Controller
 */
@RestController
@RequestMapping("/admin/certificate-type")
public class CertificateTypeController {

    @Autowired
    private CertificateTypeService certificateTypeService;
    @Autowired
    private AdminScopeService adminScopeService;

    /**
     * 查询证书类型(管理端)
     * <p>超管返回全部; 子管理员仅返回其被授权的证书类型。</p>
     */
    @GetMapping("/list")
    public Result<List<CertificateType>> list() {
        List<CertificateType> all = certificateTypeService.listAll();
        List<String> scope = adminScopeService.scopeCertTypes();
        if (scope == null) {
            return Result.success(all);
        }
        return Result.success(all.stream()
                .filter(t -> scope.contains(t.getName()))
                .collect(Collectors.toList()));
    }

    /**
     * 新增(仅超管)
     */
    @RequireSuper
    @PostMapping
    public Result<Void> add(@RequestBody CertificateType type) {
        certificateTypeService.addType(type);
        return Result.success();
    }

    /**
     * 修改(仅超管)
     */
    @RequireSuper
    @PutMapping
    public Result<Void> update(@RequestBody CertificateType type) {
        certificateTypeService.updateType(type);
        return Result.success();
    }

    /**
     * 删除(仅超管)
     */
    @RequireSuper
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        certificateTypeService.deleteType(id);
        return Result.success();
    }
}
