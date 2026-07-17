package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.entity.Banner;
import com.exam.mapper.BannerMapper;
import com.exam.service.BannerService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 轮播图Service实现
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Override
    public List<Banner> getEnabledList() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort);
        return list(wrapper);
    }
}
