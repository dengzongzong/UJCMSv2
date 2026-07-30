package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.CooperationCertContent;
import com.exam.mapper.CooperationCertContentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 授权培育基地证书内容管理(单条记录)
 * - 管理后台: GET/PUT /admin/cooperation-cert-content
 * - 用户端公开: GET /public/cooperation-cert-content
 */
@RestController
public class CooperationCertContentController {

    @Autowired
    private CooperationCertContentMapper mapper;

    /** 管理后台 - 获取证书内容 */
    @GetMapping("/admin/cooperation-cert-content")
    public Result<CooperationCertContent> get() {
        CooperationCertContent content = mapper.selectById(1L);
        if (content == null) {
            content = new CooperationCertContent();
            content.setId(1L);
            content.setImageUrl("");
            content.setRichText("");
        }
        return Result.success(content);
    }

    /** 管理后台 - 保存证书内容(新增或更新) */
    @PutMapping("/admin/cooperation-cert-content")
    public Result<Void> save(@RequestBody CooperationCertContent body) {
        body.setId(1L);
        body.setUpdateTime(LocalDateTime.now());
        CooperationCertContent existing = mapper.selectById(1L);
        if (existing == null) {
            mapper.insert(body);
        } else {
            mapper.updateById(body);
        }
        return Result.success();
    }

    /** 用户端公开 - 获取证书内容 */
    @GetMapping("/public/cooperation-cert-content")
    public Result<CooperationCertContent> publicGet() {
        CooperationCertContent content = mapper.selectById(1L);
        if (content == null) {
            content = new CooperationCertContent();
            content.setId(1L);
            content.setImageUrl("");
            content.setRichText("");
        }
        return Result.success(content);
    }
}
