package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.entity.Course;
import com.exam.entity.Exam;
import com.exam.entity.Subject;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.SubjectMapper;
import com.exam.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubjectServiceImpl extends ServiceImpl<SubjectMapper, Subject> implements SubjectService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithCheck(Long id) {
        List<String> refs = new ArrayList<>();
        if (examMapper.selectCount(new LambdaQueryWrapper<Exam>().eq(Exam::getSubjectId, id)) > 0)
            refs.add("考试");
        if (courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getSubjectId, id)) > 0)
            refs.add("课程");
        if (!refs.isEmpty()) {
            throw new BusinessException("该科目已被引用,不能删除，引用方: " + String.join("、", refs));
        }
        this.removeById(id);
    }
}
