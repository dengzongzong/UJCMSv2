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

    FaceVerifyStatusVO getStatus(Long studentId, Long examId);

    void saveConfig(String enabled, String threshold, String maxRetries);

    FaceVerifyStatsVO getStats(String date);

    List<FaceVerifyLogVO> getLogList(Integer page, Integer size, String studentName, String examName, Integer verifyResult);

    Long getLogCount(String studentName, String examName, Integer verifyResult);
}
