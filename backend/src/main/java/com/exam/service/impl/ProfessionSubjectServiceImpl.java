package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.Profession;
import com.exam.entity.Subject;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.SubjectMapper;
import com.exam.service.ProfessionSubjectService;
import com.exam.vo.ProfessionVO;
import com.exam.vo.SubjectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 专业科目服务实现
 */
@Service
public class ProfessionSubjectServiceImpl implements ProfessionSubjectService {

    @Autowired
    private ProfessionMapper professionMapper;

    @Autowired
    private SubjectMapper subjectMapper;

    @Override
    public List<ProfessionVO> listEnabledProfessions() {
        // 查询所有启用的专业，按 sort 排序
        List<Profession> professions = professionMapper.selectList(new LambdaQueryWrapper<Profession>()
                .eq(Profession::getStatus, 1)
                .orderByAsc(Profession::getSort));
        if (professions.isEmpty()) {
            return new ArrayList<>();
        }

        // 一次性查询所有启用科目，按 sort 排序
        List<Long> professionIds = professions.stream()
                .map(Profession::getId)
                .collect(Collectors.toList());
        List<Subject> subjects = subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                .in(Subject::getProfessionId, professionIds)
                .eq(Subject::getStatus, 1)
                .orderByAsc(Subject::getSort));

        // 组装专业+科目树
        return professions.stream().map(profession -> {
            ProfessionVO vo = new ProfessionVO();
            vo.setId(profession.getId());
            vo.setName(profession.getName());
            List<SubjectVO> subjectVOs = subjects.stream()
                    .filter(s -> s.getProfessionId().equals(profession.getId()))
                    .map(s -> {
                        SubjectVO svo = new SubjectVO();
                        svo.setId(s.getId());
                        svo.setName(s.getName());
                        return svo;
                    })
                    .collect(Collectors.toList());
            vo.setSubjects(subjectVOs);
            return vo;
        }).collect(Collectors.toList());
    }
}
