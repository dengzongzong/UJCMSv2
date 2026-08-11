package com.exam.controller;

import com.exam.common.Result;
import com.exam.mapper.DashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    @Autowired
    private DashboardMapper dashboardMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        // 1 次 SQL 查询替代原来 9 次独立 COUNT，性能提升显著
        Map<String, Object> data = dashboardMapper.stats();
        // 确保所有 key 都不为 null（MyBatis 返回 null 时前端显示 --）
        // 如果某个 key 缺失，说明该表可能不存在，给个默认值 0
        String[] keys = {"student", "certificateUser", "certificate", "course", "exam", "question", "paper", "video", "examRecord"};
        for (String key : keys) {
            data.putIfAbsent(key, 0);
        }
        return Result.success(data);
    }
}