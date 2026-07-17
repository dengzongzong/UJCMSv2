package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.dto.CooperationSettingDTO;
import com.exam.entity.CooperationSetting;
import com.exam.mapper.CooperationSettingMapper;
import com.exam.service.CooperationSettingService;
import org.springframework.stereotype.Service;

@Service
public class CooperationSettingServiceImpl
        extends ServiceImpl<CooperationSettingMapper, CooperationSetting>
        implements CooperationSettingService {

    @Override
    public CooperationSetting getSetting() {
        CooperationSetting one = this.getOne(new LambdaQueryWrapper<CooperationSetting>().last("LIMIT 1"));
        if (one == null) {
            // 兜底:返回一条空配置,保证前端拿到非 null 结果
            CooperationSetting def = new CooperationSetting();
            def.setId(1L);
            def.setPhone1("");
            def.setPhone2("");
            def.setEmail1("");
            def.setEmail2("");
            def.setProcessDesc("");
            def.setIntro("");
            return def;
        }
        return one;
    }

    @Override
    public void updateSetting(CooperationSettingDTO dto) {
        CooperationSetting existing = this.getOne(new LambdaQueryWrapper<CooperationSetting>().last("LIMIT 1"));
        CooperationSetting target = existing == null ? new CooperationSetting() : existing;
        target.setPhone1(dto.getPhone1());
        target.setPhone2(dto.getPhone2());
        target.setEmail1(dto.getEmail1());
        target.setEmail2(dto.getEmail2());
        target.setProcessDesc(dto.getProcessDesc());
        target.setIntro(dto.getIntro());
        target.setAttachmentName(dto.getAttachmentName());
        target.setAttachmentUrl(dto.getAttachmentUrl());
        if (target.getId() == null) {
            this.save(target);
        } else {
            this.updateById(target);
        }
    }
}
