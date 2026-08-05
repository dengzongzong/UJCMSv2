package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.FaceVerifyLog;
import com.exam.vo.FaceVerifyConfigVO;
import com.exam.vo.FaceVerifyLogVO;
import com.exam.vo.FaceVerifyStatsVO;
import com.exam.vo.FaceVerifyStatusVO;

import java.util.List;
import java.util.Map;

public interface FaceVerifyService extends IService<FaceVerifyLog> {

    FaceVerifyConfigVO getConfig();

    Map<String, String> getIdPhoto(Long studentId);

    void submitVerify(Long studentId, Long examId, Double similarity, Boolean passed, String deviceInfo, String ipAddress);

    /**
     * 后端人脸比对: 接收前端拍摄的照片, 与证件照比对
     * 不保存拍摄照片, 只返回比对结果并更新考试记录
     *
     * @param studentId  学生ID
     * @param examId     考试ID
     * @param photoBase64 拍摄照片(base64编码, 可带 data:image/jpeg;base64, 前缀)
     * @param deviceInfo 设备信息
     * @param ipAddress  IP地址
     * @return Map: {passed, similarity, message}
     */
    Map<String, Object> compare(Long studentId, Long examId, String photoBase64, String deviceInfo, String ipAddress);

    FaceVerifyStatusVO getStatus(Long studentId, Long examId);

    void saveConfig(String enabled, String threshold, String maxRetries);

    FaceVerifyStatsVO getStats(String date);

    List<FaceVerifyLogVO> getLogList(Integer page, Integer size, String studentName, String examName, Integer verifyResult);

    Long getLogCount(String studentName, String examName, Integer verifyResult);
}
