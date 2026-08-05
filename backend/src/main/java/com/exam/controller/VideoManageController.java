package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.VideoDTO;
import com.exam.service.VideoManageService;
import com.exam.vo.AdminVideoVO;
import org.springframework.beans.factory.annotation.Autowired;
import com.exam.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 视频管理
 */
@RestController
@RequestMapping("/admin/video")
public class VideoManageController {

    @Autowired
    private VideoManageService videoManageService;

    /**
     * 分页查询视频
     */
    @GetMapping("/page")
    public Result<PageResult<AdminVideoVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String name,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Long professionId) {
        PageResult<AdminVideoVO> result = videoManageService.page(page, size, name, categoryId, professionId);
        return Result.success(result);
    }

    /**
     * 视频详情
     */
    @GetMapping("/{id}")
    public Result<AdminVideoVO> detail(@PathVariable Long id) {
        AdminVideoVO video = videoManageService.detail(id);
        return Result.success(video);
    }

    /**
     * 新增视频
     */
    @PostMapping
    public Result<Void> add(@RequestBody VideoDTO dto) {
        videoManageService.add(dto);
        return Result.success();
    }

    /**
     * 编辑视频
     */
    @PutMapping
    public Result<Void> update(@RequestBody VideoDTO dto) {
        videoManageService.update(dto);
        return Result.success();
    }

    /**
     * 删除视频
     */
    @RequirePermission("delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        videoManageService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除视频
     */
    @RequirePermission("delete")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        videoManageService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 按播放量排序返回视频列表
     */
    @GetMapping("/sort")
    public Result<List<AdminVideoVO>> sort() {
        List<AdminVideoVO> result = videoManageService.sortByPlayCount();
        return Result.success(result);
    }
}
