package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.dto.AboutUsDTO;
import com.exam.entity.AboutUs;
import com.exam.mapper.AboutUsMapper;
import com.exam.service.AboutUsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AboutUsServiceImpl extends ServiceImpl<AboutUsMapper, AboutUs> implements AboutUsService {

    @Override
    public AboutUs getAboutUs() {
        List<AboutUs> list = this.list();
        if (list.isEmpty()) {
            AboutUs aboutUs = new AboutUs();
            aboutUs.setServicePhone("");
            aboutUs.setContent("");
            return aboutUs;
        }
        return list.get(0);
    }

    @Override
    public void updateAboutUs(AboutUsDTO dto) {
        AboutUs aboutUs = getAboutUs();
        aboutUs.setServicePhone(dto.getServicePhone());
        aboutUs.setServiceQrcode(dto.getServiceQrcode());
        aboutUs.setQrcodeLink(dto.getQrcodeLink());
        aboutUs.setContent(dto.getContent());
        if (aboutUs.getId() == null) {
            this.save(aboutUs);
        } else {
            this.updateById(aboutUs);
        }
    }
}
