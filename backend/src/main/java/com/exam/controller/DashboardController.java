package com.exam.controller;

import com.exam.common.Result;
import com.exam.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private ExamRecordMapper examRecordMapper;
    @Autowired
    private CertificateUserMapper certificateUserMapper;
    @Autowired
    private CertificateMapper certificateMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new HashMap<>();
        // 学生总数:按身份证号去重,避免同一人多条记录
        data.put("student", studentMapper.countDistinctByIdCard());
        // 证书用户:按身份证号去重,certificate_user按专业维度有多条记录
        data.put("certificateUser", certificateUserMapper.countDistinctByIdCard());
        // 已发证书:certificate表中实际已颁发的证书数
        data.put("certificate", certificateMapper.selectCount(null));
        data.put("course", courseMapper.selectCount(null));
        data.put("exam", examMapper.selectCount(null));
        data.put("question", questionMapper.selectCount(null));
        data.put("paper", paperMapper.selectCount(null));
        data.put("video", videoMapper.selectCount(null));
        data.put("examRecord", examRecordMapper.selectCount(null));
        return Result.success(data);
    }
}
