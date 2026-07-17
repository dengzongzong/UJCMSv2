package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.CooperationSetting;
import com.exam.service.CooperationSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合作咨询-公开接口(无需鉴权)
 */
@RestController
@RequestMapping("/public/cooperation")
public class CooperationPublicController {

    @Autowired
    private CooperationSettingService service;

    @GetMapping
    public Result<CooperationSetting> get() {
        return Result.success(service.getSetting());
    }
}
