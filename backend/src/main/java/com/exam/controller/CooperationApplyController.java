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
}
