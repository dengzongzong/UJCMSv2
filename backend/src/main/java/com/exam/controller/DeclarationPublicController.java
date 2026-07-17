package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.SiteDeclaration;
import com.exam.service.SiteDeclarationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网站声明-公开接口(无需鉴权)
 */
@RestController
@RequestMapping("/public/declaration")
public class DeclarationPublicController {

    @Autowired
    private SiteDeclarationService service;

    @GetMapping
    public Result<SiteDeclaration> get() {
        return Result.success(service.getDeclaration());
    }
}
