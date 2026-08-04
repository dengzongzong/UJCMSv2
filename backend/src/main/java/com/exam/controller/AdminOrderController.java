package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.CourseOrder;
import com.exam.entity.SystemSetting;
import com.exam.service.OrderService;
import com.exam.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端课程订单
 */
@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    private static final String PAY_ENABLED_KEY = "pay_enabled";

    @Autowired
    private OrderService orderService;

    @Autowired
    private SystemSettingService systemSettingService;

    /**
     * 订单分页(支持按订单号/课程名搜索, 状态/渠道筛选)
     */
    @GetMapping("/page")
    public Result<PageResult<CourseOrder>> page(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) String channel) {
        return Result.success(orderService.adminPage(page, size, keyword, status, channel));
    }

    /**
     * 支付总开关状态
     */
    @GetMapping("/pay-switch")
    public Result<Map<String, Object>> paySwitch() {
        String value = systemSettingService.getValueByKey(PAY_ENABLED_KEY);
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", "1".equals(value));
        return Result.success(result);
    }

    /**
     * 设置支付总开关
     */
    @PutMapping("/pay-switch")
    public Result<Void> setPaySwitch(@RequestBody Map<String, Object> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(PAY_ENABLED_KEY);
        setting.setSettingValue(enabled ? "1" : "0");
        setting.setRemark("课程在线支付总开关: 0-关闭 1-开启");
        systemSettingService.saveOrUpdate(setting, new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, PAY_ENABLED_KEY));
        return Result.success();
    }
}
