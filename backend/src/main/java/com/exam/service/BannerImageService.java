package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.BannerImage;
import java.util.List;

public interface BannerImageService extends IService<BannerImage> {
    List<BannerImage> listEnabled();
}
