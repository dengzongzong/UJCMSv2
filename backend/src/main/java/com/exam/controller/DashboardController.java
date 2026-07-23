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

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("student", studentMapper.selectCount(null));
        data.put("course", courseMapper.selectCount(null));
        data.put("exam", examMapper.selectCount(null));
        data.put("question", questionMapper.selectCount(null));
        data.put("paper", paperMapper.selectCount(null));
        data.put("video", videoMapper.selectCount(null));
        data.put("examRecord", examRecordMapper.selectCount(null));
        data.put("certificateUser", certificateUserMapper.selectCount(null));
        return Result.success(data);
    }
}
