package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.ProfessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProfessionServiceImpl extends ServiceImpl<ProfessionMapper, Profession> implements ProfessionService {

    @Autowired
    private SubjectMapper subjectMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private StudentProfessionMapper studentProfessionMapper;
    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private VideoMapper videoMapper;

    @Override
    public List<Map<String, Object>> listWithSubjects() {
        List<Profession> professions = this.list(new LambdaQueryWrapper<Profession>()
                .orderByAsc(Profession::getSort));
        if (professions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> professionIds = professions.stream().map(Profession::getId).collect(Collectors.toList());
        List<Subject> subjects = subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                .in(Subject::getProfessionId, professionIds)
                .orderByAsc(Subject::getSort));
        Map<Long, List<Subject>> subjectMap = subjects.stream()
                .collect(Collectors.groupingBy(Subject::getProfessionId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Profession profession : professions) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", profession.getId());
            item.put("name", profession.getName());
            item.put("sort", profession.getSort());
            item.put("status", profession.getStatus());
            item.put("subjects", subjectMap.getOrDefault(profession.getId(), new ArrayList<>()));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithCheck(Long id) {
        List<String> refs = new ArrayList<>();
        if (studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getProfessionId, id)) > 0)
            refs.add("学生");
        if (studentProfessionMapper.selectCount(new LambdaQueryWrapper<StudentProfession>().eq(StudentProfession::getProfessionId, id)) > 0)
            refs.add("学生专业关联");
        if (examMapper.selectCount(new LambdaQueryWrapper<Exam>().eq(Exam::getProfessionId, id)) > 0)
            refs.add("考试");
        if (courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getProfessionId, id)) > 0)
            refs.add("课程");
        if (paperMapper.selectCount(new LambdaQueryWrapper<Paper>().eq(Paper::getProfessionId, id)) > 0)
            refs.add("试卷");
        if (questionMapper.selectCount(new LambdaQueryWrapper<Question>().eq(Question::getProfessionId, id)) > 0)
            refs.add("题目");
        if (videoMapper.selectCount(new LambdaQueryWrapper<Video>().eq(Video::getProfessionId, id)) > 0)
            refs.add("视频");
        if (subjectMapper.selectCount(new LambdaQueryWrapper<Subject>().eq(Subject::getProfessionId, id)) > 0)
            refs.add("科目");
        if (!refs.isEmpty()) {
            throw new BusinessException("该专业已被引用,不能删除，引用方: " + String.join("、", refs));
        }
        this.removeById(id);
    }
}
