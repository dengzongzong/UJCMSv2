package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.HomepageSection;
import com.exam.service.HomepageSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 首页内容板块管理Controller(政策法规/信息公开)
 */
@RestController
@RequestMapping("/admin/homepage-section")
public class HomepageSectionController {

    @Autowired
    private HomepageSectionService homepageSectionService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public Result<PageResult<HomepageSection>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        PageResult<HomepageSection> result = homepageSectionService.page(page, size, title, type, status);
        return Result.success(result);
    }

    /**
     * 新增
     */
    @PostMapping
    public Result<Void> add(@RequestBody HomepageSection section) {
        homepageSectionService.add(section);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping
    public Result<Void> update(@RequestBody HomepageSection section) {
        homepageSectionService.update(section);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        homepageSectionService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        homepageSectionService.batchDelete(ids);
        return Result.success();
    }
}
