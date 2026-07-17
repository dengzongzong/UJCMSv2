package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.entity.SystemSetting;
import com.exam.mapper.SystemSettingMapper;
import com.exam.service.SystemSettingService;
import org.springframework.stereotype.Service;

/**
 * 系统设置服务实现
 */
@Service
public class SystemSettingServiceImpl extends ServiceImpl<SystemSettingMapper, SystemSetting> implements SystemSettingService {

    @Override
    public String getValueByKey(String key) {
        SystemSetting setting = this.getOne(new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, key));
        return setting != null ? setting.getSettingValue() : null;
    }
}
