package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Banner;
import com.exam.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import com.exam.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

/**
 * 轮播图后台管理Controller（需要JWT鉴权）
 */
@RestController
@RequestMapping("/admin/banner")
public class BannerManageController {

    @Autowired
    private BannerService bannerService;

    /**
     * 分页查询轮播图
     */
    @GetMapping("/page")
    public Result<PageResult<Banner>> page(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String title) {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<Banner>()
                .like(StringUtils.hasText(title), Banner::getTitle, title)
                .orderByAsc(Banner::getSort);
        Page<Banner> p = new Page<>(page, size);
        Page<Banner> result = bannerService.page(p, wrapper);
        return Result.success(new PageResult<>(result));
    }

    /**
     * 轮播图详情
     */
    @GetMapping("/{id}")
    public Result<Banner> detail(@PathVariable Long id) {
        Banner banner = bannerService.getById(id);
        if (banner == null) {
            throw new BusinessException("轮播图不存在");
        }
        return Result.success(banner);
    }

    /**
     * 新增轮播图
     */
    @PostMapping
    public Result<Void> add(@RequestBody Banner banner) {
        bannerService.save(banner);
        return Result.success();
    }

    /**
     * 编辑轮播图
     */
    @PutMapping
    public Result<Void> update(@RequestBody Banner banner) {
        if (banner.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        bannerService.updateById(banner);
        return Result.success();
    }

    /**
     * 删除轮播图
     */
    @RequirePermission("delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.removeById(id);
        return Result.success();
    }
}
