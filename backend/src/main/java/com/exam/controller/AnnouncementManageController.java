package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Announcement;
import com.exam.service.AnnouncementManageService;
import org.springframework.beans.factory.annotation.Autowired;
import com.exam.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公告后台管理Controller（需要JWT鉴权）
 */
@RestController
@RequestMapping("/admin/announcement")
public class AnnouncementManageController {

    @Autowired
    private AnnouncementManageService announcementManageService;

    /**
     * 分页查询公告
     */
    @GetMapping("/page")
    public Result<PageResult<Announcement>> page(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String title,
                                                 @RequestParam(required = false) Integer status) {
        PageResult<Announcement> result = announcementManageService.page(page, size, title, status);
        return Result.success(result);
    }

    /**
     * 新增公告
     */
    @PostMapping
    public Result<Void> add(@RequestBody Announcement announcement) {
        announcementManageService.add(announcement);
        return Result.success();
    }

    /**
     * 修改公告
     */
    @PutMapping
    public Result<Void> update(@RequestBody Announcement announcement) {
        announcementManageService.update(announcement);
        return Result.success();
    }

    /**
     * 删除公告
     */
    @RequirePermission("delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementManageService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除公告
     */
    @RequirePermission("delete")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        announcementManageService.batchDelete(ids);
        return Result.success();
    }
}
