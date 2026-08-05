package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.CertificatePhotoService;
import com.exam.service.FaceVerifyService;
import com.exam.service.SystemSettingService;
import com.exam.vo.FaceVerifyConfigVO;
import com.exam.vo.FaceVerifyLogVO;
import com.exam.vo.FaceVerifyStatsVO;
import com.exam.vo.FaceVerifyStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FaceVerifyServiceImpl extends ServiceImpl<FaceVerifyLogMapper, FaceVerifyLog>
    implements FaceVerifyService {

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CertificateUserMapper certificateUserMapper;

    @Autowired
    private CertificatePhotoService certificatePhotoService;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private ExamMapper examMapper;

    @Override
    public FaceVerifyConfigVO getConfig() {
        FaceVerifyConfigVO vo = new FaceVerifyConfigVO();
        String enabled = systemSettingService.getValueByKey("face_verify_enabled");
        String threshold = systemSettingService.getValueByKey("face_verify_threshold");
        String maxRetries = systemSettingService.getValueByKey("face_verify_max_retries");

        vo.setEnabled("1".equals(enabled));
        vo.setThreshold(threshold != null ? Double.parseDouble(threshold) : 0.6);
        vo.setMaxRetries(maxRetries != null ? Integer.parseInt(maxRetries) : 3);
        return vo;
    }

    @Override
    public Map<String, String> getIdPhoto(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生信息不存在");
        }

        String idCard = null;
        LambdaQueryWrapper<CertificateUser> cuWrapper = new LambdaQueryWrapper<>();
        cuWrapper.eq(CertificateUser::getStudentId, studentId);
        CertificateUser certUser = certificateUserMapper.selectOne(cuWrapper);

        if (certUser != null && certUser.getIdCard() != null) {
            idCard = certUser.getIdCard();
        } else if (student.getIdCard() != null) {
            idCard = student.getIdCard();
        }

        Map<String, String> result = new HashMap<>();
        if (idCard == null) {
            result.put("hasPhoto", "false");
            result.put("message", "未找到身份证信息，请联系管理员");
            return result;
        }

        CertificatePhoto photo = certificatePhotoService.getLatestByIdCard(idCard);
        if (photo != null && photo.getUrl() != null) {
            result.put("photoUrl", photo.getUrl());
            result.put("hasPhoto", "true");
        } else {
            result.put("hasPhoto", "false");
            result.put("message", "未找到证书照片，请联系管理员上传");
        }
        return result;
    }

    @Override
    @Transactional
    public void submitVerify(Long studentId, Long examId, Double similarity, Boolean passed, String deviceInfo, String ipAddress) {
        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getSubmitStatus, 0)
                .orderByDesc(ExamRecord::getCreateTime)
                .last("LIMIT 1");
        ExamRecord record = examRecordMapper.selectOne(recordWrapper);

        if (record == null) {
            throw new BusinessException("考试记录不存在");
        }

        record.setFaceVerifyStatus(passed ? 1 : 2);
        record.setFaceVerifyTime(LocalDateTime.now());
        record.setFaceSimilarity(similarity != null ? BigDecimal.valueOf(similarity) : null);
        examRecordMapper.updateById(record);

        try {
            FaceVerifyLog faceLog = new FaceVerifyLog();
            faceLog.setStudentId(studentId);
            faceLog.setExamId(examId);
            faceLog.setRecordId(record.getId());
            faceLog.setVerifyResult(passed ? 1 : 0);
            faceLog.setSimilarity(similarity != null ? BigDecimal.valueOf(similarity) : null);
            faceLog.setDeviceInfo(deviceInfo);
            faceLog.setIpAddress(ipAddress);
            this.save(faceLog);
        } catch (Exception e) {
            // 日志表写入失败不影响考试流程
            log.warn("人脸验证日志写入失败: {}", e.getMessage());
        }
    }

    @Override
    public FaceVerifyStatusVO getStatus(Long studentId, Long examId) {
        FaceVerifyStatusVO vo = new FaceVerifyStatusVO();
        FaceVerifyConfigVO config = getConfig();
        vo.setEnabled(config.isEnabled());

        if (!config.isEnabled()) {
            vo.setVerified(true);
            vo.setMessage("人脸识别未启用");
            return vo;
        }

        LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getSubmitStatus, 0)
                .orderByDesc(ExamRecord::getCreateTime)
                .last("LIMIT 1");
        ExamRecord record = examRecordMapper.selectOne(recordWrapper);

        if (record == null) {
            vo.setVerified(false);
            return vo;
        }

        if (record.getFaceVerifyStatus() != null && record.getFaceVerifyStatus() == 1) {
            vo.setVerified(true);
            vo.setSimilarity(record.getFaceSimilarity() != null
                ? record.getFaceSimilarity().doubleValue() : null);
        } else {
            vo.setVerified(false);
        }
        return vo;
    }

    @Override
    public void saveConfig(String enabled, String threshold, String maxRetries) {
        SystemSetting enabledSetting = new SystemSetting();
        enabledSetting.setSettingKey("face_verify_enabled");
        enabledSetting.setSettingValue(enabled);
        enabledSetting.setRemark("考前人脸识别开关：0-关闭 1-开启");
        systemSettingService.saveOrUpdate(enabledSetting, new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, "face_verify_enabled"));

        SystemSetting thresholdSetting = new SystemSetting();
        thresholdSetting.setSettingKey("face_verify_threshold");
        thresholdSetting.setSettingValue(threshold);
        thresholdSetting.setRemark("人脸比对阈值");
        systemSettingService.saveOrUpdate(thresholdSetting, new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, "face_verify_threshold"));

        SystemSetting retriesSetting = new SystemSetting();
        retriesSetting.setSettingKey("face_verify_max_retries");
        retriesSetting.setSettingValue(maxRetries);
        retriesSetting.setRemark("人脸验证最大重试次数");
        systemSettingService.saveOrUpdate(retriesSetting, new LambdaQueryWrapper<SystemSetting>()
                .eq(SystemSetting::getSettingKey, "face_verify_max_retries"));
    }

    @Override
    public FaceVerifyStatsVO getStats(String date) {
        FaceVerifyStatsVO stats = new FaceVerifyStatsVO();
        LocalDate targetDate = date != null && !date.isEmpty() ? LocalDate.parse(date) : LocalDate.now();

        LambdaQueryWrapper<FaceVerifyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(FaceVerifyLog::getCreateTime, LocalDateTime.of(targetDate, LocalTime.MIN))
                .le(FaceVerifyLog::getCreateTime, LocalDateTime.of(targetDate, LocalTime.MAX));

        List<FaceVerifyLog> logs = this.list(wrapper);
        int total = logs.size();
        int success = 0;
        int fail = 0;
        for (FaceVerifyLog log : logs) {
            if (log.getVerifyResult() != null && log.getVerifyResult() == 1) {
                success++;
            } else {
                fail++;
            }
        }

        stats.setTotal(total);
        stats.setSuccess(success);
        stats.setFail(fail);
        stats.setPassRate(total > 0 ? (int) Math.round((double) success / total * 100) : 0);
        return stats;
    }

    @Override
    public List<FaceVerifyLogVO> getLogList(Integer page, Integer size, String studentName, String examName, Integer verifyResult) {
        int offset = (page - 1) * size;

        LambdaQueryWrapper<FaceVerifyLog> wrapper = new LambdaQueryWrapper<>();
        if (verifyResult != null) {
            wrapper.eq(FaceVerifyLog::getVerifyResult, verifyResult);
        }
        wrapper.orderByDesc(FaceVerifyLog::getCreateTime)
                .last("LIMIT " + offset + ", " + size);

        List<FaceVerifyLog> logs = this.list(wrapper);
        List<FaceVerifyLogVO> voList = new ArrayList<>();

        for (FaceVerifyLog log : logs) {
            FaceVerifyLogVO vo = new FaceVerifyLogVO();
            vo.setId(log.getId());
            vo.setStudentId(log.getStudentId());
            vo.setExamId(log.getExamId());
            vo.setVerifyResult(log.getVerifyResult());
            vo.setSimilarity(log.getSimilarity());
            vo.setRetryCount(log.getRetryCount());
            vo.setErrorMsg(log.getErrorMsg());
            vo.setDeviceInfo(log.getDeviceInfo());
            vo.setIpAddress(log.getIpAddress());
            vo.setCreateTime(log.getCreateTime());

            Student student = studentMapper.selectById(log.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getName());
                vo.setStudentPhone(student.getPhone());
            }

            Exam exam = examMapper.selectById(log.getExamId());
            if (exam != null) {
                vo.setExamName(exam.getName());
            }

            voList.add(vo);
        }

        if (studentName != null && !studentName.isEmpty()) {
            voList.removeIf(vo -> vo.getStudentName() == null || !vo.getStudentName().contains(studentName));
        }
        if (examName != null && !examName.isEmpty()) {
            voList.removeIf(vo -> vo.getExamName() == null || !vo.getExamName().contains(examName));
        }

        return voList;
    }

    @Override
    public Long getLogCount(String studentName, String examName, Integer verifyResult) {
        LambdaQueryWrapper<FaceVerifyLog> wrapper = new LambdaQueryWrapper<>();
        if (verifyResult != null) {
            wrapper.eq(FaceVerifyLog::getVerifyResult, verifyResult);
        }
        long count = this.count(wrapper);

        if ((studentName != null && !studentName.isEmpty()) || (examName != null && !examName.isEmpty())) {
            List<FaceVerifyLogVO> allLogs = getLogList(1, Integer.MAX_VALUE, studentName, examName, verifyResult);
            count = allLogs.size();
        }

        return count;
    }
}
