package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.Banner;
import com.exam.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 轮播图Controller（公开接口，无需鉴权）
 */
@RestController
@RequestMapping("/public/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    /**
     * 获取启用的轮播图列表，按sort排序
     */
    @GetMapping("/list")
    public Result<List<Banner>> list() {
        return Result.success(bannerService.getEnabledList());
    }
}
