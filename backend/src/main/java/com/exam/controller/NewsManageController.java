package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.News;
import com.exam.service.NewsManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 新闻后台管理Controller（需要JWT鉴权）
 */
@RestController
@RequestMapping("/admin/news")
public class NewsManageController {

    @Autowired
    private NewsManageService newsManageService;

    /**
     * 分页查询新闻
     */
    @GetMapping("/page")
    public Result<PageResult<News>> page(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String title,
                                         @RequestParam(required = false) Integer status) {
        PageResult<News> result = newsManageService.page(page, size, title, status);
        return Result.success(result);
    }

    /**
     * 新增新闻
     */
    @PostMapping
    public Result<Void> add(@RequestBody News news) {
        newsManageService.add(news);
        return Result.success();
    }

    /**
     * 修改新闻
     */
    @PutMapping
    public Result<Void> update(@RequestBody News news) {
        newsManageService.update(news);
        return Result.success();
    }

    /**
     * 删除新闻
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        newsManageService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除新闻
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        newsManageService.batchDelete(ids);
        return Result.success();
    }
}
