package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.SiteDeclarationDTO;
import com.exam.entity.SiteDeclaration;
import com.exam.service.SiteDeclarationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网站声明-管理后台
 */
@RestController
@RequestMapping("/admin/setting/declaration")
public class DeclarationManageController {

    @Autowired
    private SiteDeclarationService service;

    @GetMapping
    public Result<SiteDeclaration> get() {
        return Result.success(service.getDeclaration());
    }

    @PutMapping
    public Result<Void> update(@RequestBody SiteDeclarationDTO dto) {
        service.updateDeclaration(dto);
        return Result.success();
    }
}
