package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.service.FaceVerifyService;
import com.exam.vo.FaceVerifyConfigVO;
import com.exam.vo.FaceVerifyLogVO;
import com.exam.vo.FaceVerifyStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/face-verify")
public class FaceVerifyManageController {

    @Autowired
    private FaceVerifyService faceVerifyService;

    @GetMapping("/config")
    public Result<FaceVerifyConfigVO> getConfig() {
        return Result.success(faceVerifyService.getConfig());
    }

    @PutMapping("/config")
    public Result<Void> saveConfig(@RequestBody Map<String, String> params) {
        String enabled = params.getOrDefault("enabled", "0");
        String threshold = params.getOrDefault("threshold", "0.6");
        String maxRetries = params.getOrDefault("maxRetries", "3");
        faceVerifyService.saveConfig(enabled, threshold, maxRetries);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<FaceVerifyStatsVO> getStats(@RequestParam(required = false) String date) {
        return Result.success(faceVerifyService.getStats(date));
    }

    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String examName,
            @RequestParam(required = false) Integer verifyResult) {
        Map<String, Object> result = new HashMap<>();
        List<FaceVerifyLogVO> list = faceVerifyService.getLogList(page, size, studentName, examName, verifyResult);
        Long total = faceVerifyService.getLogCount(studentName, examName, verifyResult);
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }
}
