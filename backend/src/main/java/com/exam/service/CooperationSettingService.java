package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.dto.CooperationSettingDTO;
import com.exam.entity.CooperationSetting;

public interface CooperationSettingService extends IService<CooperationSetting> {

    /** 获取合作咨询配置(单条) */
    CooperationSetting getSetting();

    /** 更新合作咨询配置 */
    void updateSetting(CooperationSettingDTO dto);
}
