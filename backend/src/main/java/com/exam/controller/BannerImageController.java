package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.BannerImage;
import com.exam.service.BannerImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 首页横幅图片管理
 */
@RestController
@RequestMapping("/admin/banner-image")
public class BannerImageController {

    @Autowired
    private BannerImageService bannerImageService;

    @GetMapping("/list")
    public Result<List<BannerImage>> list() {
        return Result.success(bannerImageService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody BannerImage banner) {
        bannerImageService.save(banner);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody BannerImage banner) {
        bannerImageService.updateById(banner);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerImageService.removeById(id);
        return Result.success();
    }
}
