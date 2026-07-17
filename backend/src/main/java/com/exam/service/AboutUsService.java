package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.dto.AboutUsDTO;
import com.exam.entity.AboutUs;

public interface AboutUsService extends IService<AboutUs> {

    /**
     * 获取关于我们信息
     */
    AboutUs getAboutUs();

    /**
     * 更新关于我们信息
     */
    void updateAboutUs(AboutUsDTO dto);
}
