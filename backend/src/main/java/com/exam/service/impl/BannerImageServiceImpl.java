package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.entity.BannerImage;
import com.exam.mapper.BannerImageMapper;
import com.exam.service.BannerImageService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BannerImageServiceImpl extends ServiceImpl<BannerImageMapper, BannerImage> implements BannerImageService {

    @Override
    public List<BannerImage> listEnabled() {
        return this.list(new LambdaQueryWrapper<BannerImage>()
                .eq(BannerImage::getStatus, 1)
                .orderByAsc(BannerImage::getSort)
                .orderByDesc(BannerImage::getCreateTime));
    }
}
