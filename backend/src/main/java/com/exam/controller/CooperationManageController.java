package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.CooperationSettingDTO;
import com.exam.entity.CooperationSetting;
import com.exam.service.CooperationSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合作咨询-管理后台
 */
@RestController
@RequestMapping("/admin/setting/cooperation")
public class CooperationManageController {

    @Autowired
    private CooperationSettingService service;

    @GetMapping
    public Result<CooperationSetting> get() {
        return Result.success(service.getSetting());
    }

    @PutMapping
    public Result<Void> update(@RequestBody CooperationSettingDTO dto) {
        service.updateSetting(dto);
        return Result.success();
    }
}
