package com.exam.controller;

import com.exam.common.Result;
import com.exam.service.FaceVerifyService;
import com.exam.util.FaceCompareUtil;
import com.exam.vo.FaceVerifyConfigVO;
import com.exam.vo.FaceVerifyStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/face")
public class FaceVerifyController {

    @Autowired
    private FaceVerifyService faceVerifyService;

    @Autowired
    private FaceCompareUtil faceCompareUtil;

    /**
     * 检查人脸比对引擎状态(用于排查 OpenCV 加载问题)
     */
    @GetMapping("/engine-status")
    public Result<Map<String, Object>> getEngineStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("initialized", faceCompareUtil.isInitialized());
        status.put("error", faceCompareUtil.getInitError());
        return Result.success(status);
    }

    @GetMapping("/config")
    public Result<FaceVerifyConfigVO> getConfig() {
        return Result.success(faceVerifyService.getConfig());
    }

    @GetMapping("/id-photo")
    public Result<Map<String, String>> getIdPhoto(@RequestAttribute("userId") Long userId) {
        return Result.success(faceVerifyService.getIdPhoto(userId));
    }

    @PostMapping("/verify")
    public Result<Void> submitVerify(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        Long examId = params.get("examId") != null
            ? Long.valueOf(params.get("examId").toString()) : null;
        Double similarity = params.get("similarity") != null
            ? Double.valueOf(params.get("similarity").toString()) : null;
        Boolean passed = (Boolean) params.get("passed");
        String deviceInfo = params.get("deviceInfo") != null ? params.get("deviceInfo").toString() : "";
        String ipAddress = getClientIp(request);
        faceVerifyService.submitVerify(userId, examId, similarity, passed, deviceInfo, ipAddress);
        return Result.success();
    }

    /**
     * 后端人脸比对: 前端拍摄照片上传, 后端与证件照比对
     * 不保存拍摄照片, 比对后即丢弃
     */
    @PostMapping("/compare")
    public Result<Map<String, Object>> compare(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        Long examId = params.get("examId") != null
            ? Long.valueOf(params.get("examId").toString()) : null;
        String photo = params.get("photo") != null ? params.get("photo").toString() : "";
        String deviceInfo = params.get("deviceInfo") != null ? params.get("deviceInfo").toString() : "";
        String ipAddress = getClientIp(request);
        Map<String, Object> result = faceVerifyService.compare(userId, examId, photo, deviceInfo, ipAddress);
        return Result.success(result);
    }

    @GetMapping("/status")
    public Result<FaceVerifyStatusVO> getStatus(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long examId) {
        return Result.success(faceVerifyService.getStatus(userId, examId));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
