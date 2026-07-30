package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.CooperationApply;
import com.exam.service.CooperationApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合作申请管理
 */
@RestController
@RequestMapping("/admin/cooperation-apply")
public class CooperationApplyController {

    @Autowired
    private CooperationApplyService cooperationApplyService;

    /**
     * 分页查询合作申请
     */
    @GetMapping("/page")
    public Result<PageResult<CooperationApply>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String unitName,
            @RequestParam(required = false) String authCode,
            @RequestParam(required = false) Integer status) {
        PageResult<CooperationApply> result = cooperationApplyService.page(page, size, unitName, authCode, status);
        return Result.success(result);
    }

    /**
     * 新增合作申请
     */
    @PostMapping
    public Result<Void> add(@RequestBody CooperationApply cooperationApply) {
        cooperationApplyService.add(cooperationApply);
        return Result.success(null);
    }

    /**
     * 修改合作申请
     */
    @PutMapping
    public Result<Void> update(@RequestBody CooperationApply cooperationApply) {
        cooperationApplyService.update(cooperationApply);
        return Result.success(null);
    }

    /**
     * 删除合作申请
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cooperationApplyService.delete(id);
        return Result.success(null);
    }

    /**
     * 批量删除合作申请
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        cooperationApplyService.batchDelete(ids);
        return Result.success(null);
    }

    /**
     * 获取指定合作单位的证书内容
     */
    @GetMapping("/{id}/cert-content")
    public Result<CooperationApply> getCertContent(@PathVariable Long id) {
        CooperationApply item = cooperationApplyService.getById(id);
        if (item == null) {
            return Result.error(404, "合作单位不存在");
        }
        // 只返回证书相关字段,避免泄露其他数据
        CooperationApply vo = new CooperationApply();
        vo.setId(item.getId());
        vo.setCertImageUrl(item.getCertImageUrl());
        vo.setCertRichText(item.getCertRichText());
        vo.setCertBgScale(item.getCertBgScale());
        vo.setCertEditorWidth(item.getCertEditorWidth());
        return Result.success(vo);
    }

    /**
     * 保存指定合作单位的证书内容
     */
    @PutMapping("/{id}/cert-content")
    public Result<Void> saveCertContent(@PathVariable Long id, @RequestBody CooperationApply body) {
        CooperationApply item = cooperationApplyService.getById(id);
        if (item == null) {
            return Result.error(404, "合作单位不存在");
        }
        CooperationApply update = new CooperationApply();
        update.setId(id);
        update.setCertImageUrl(body.getCertImageUrl());
        update.setCertRichText(body.getCertRichText());
        update.setCertBgScale(body.getCertBgScale());
        update.setCertEditorWidth(body.getCertEditorWidth());
        cooperationApplyService.update(update);
        return Result.success(null);
    }
}
