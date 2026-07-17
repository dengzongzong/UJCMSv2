package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.SystemSetting;

/**
 * 系统设置服务
 */
public interface SystemSettingService extends IService<SystemSetting> {

    /**
     * 根据 key 获取配置值
     */
    String getValueByKey(String key);
}
