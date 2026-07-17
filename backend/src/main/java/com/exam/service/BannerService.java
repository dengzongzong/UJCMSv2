package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.Banner;

import java.util.List;

/**
 * 轮播图Service
 */
public interface BannerService extends IService<Banner> {

    /**
     * 获取启用的轮播图列表，按sort排序
     */
    List<Banner> getEnabledList();
}
