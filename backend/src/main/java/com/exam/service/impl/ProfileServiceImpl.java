package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.dto.ProfileUpdateDTO;
import com.exam.entity.Question;
import com.exam.entity.QuestionOption;
import com.exam.entity.Student;
import com.exam.entity.WrongQuestion;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.QuestionOptionMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.WrongQuestionMapper;
import com.exam.service.ProfileService;
import com.exam.vo.OptionVO;
import com.exam.vo.ProfileVO;
import com.exam.vo.QuestionVO;
import com.exam.vo.WrongQuestionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 个人中心Service实现
 */
@Service
public class ProfileServiceImpl extends ServiceImpl<StudentMapper, Student> implements ProfileService {

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ProfileVO getProfile(Long studentId) {
        Student student = getById(studentId);
        if (student == null) {
            throw new BusinessException("用户不存在");
        }
        ProfileVO vo = new ProfileVO();
        vo.setId(student.getId());
        vo.setNickname(student.getNickname());
        vo.setAvatar(student.getAvatar());
        vo.setPhone(student.getPhone());
        vo.setProfessionId(student.getProfessionId());
        vo.setSubjectId(student.getSubjectId());
        return vo;
    }

    @Override
    public void updateProfile(Long studentId, ProfileUpdateDTO dto) {
        Student student = getById(studentId);
        if (student == null) {
            throw new BusinessException("用户不存在");
        }
        Student update = new Student();
        update.setId(studentId);
        update.setNickname(dto.getNickname());
        update.setAvatar(dto.getAvatar());
        updateById(update);
    }

    @Override
    public Page<WrongQuestionVO> getWrongQuestions(Long studentId, Integer type, Long examId,
                                                   String keyword, Integer page, Integer size) {
        Page<WrongQuestion> p = new Page<>(page, size);
        LambdaQueryWrapper<WrongQuestion> w = new LambdaQueryWrapper<>();
        w.eq(WrongQuestion::getStudentId, studentId)
                .orderByDesc(WrongQuestion::getCreateTime);
        if (examId != null) {
            w.eq(WrongQuestion::getExamId, examId);
        }
        // type=1 仅未掌握(type=0 或 null 时返回全部)
        if (Integer.valueOf(1).equals(type)) {
            w.eq(WrongQuestion::getMastered, 0);
        }
        Page<WrongQuestion> result = wrongQuestionMapper.selectPage(p, w);
        if (result.getRecords().isEmpty()) {
            return new Page<>(page, size, 0L);
        }
        Page<WrongQuestionVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toWrongQuestionVO).collect(Collectors.toList()));
        return voPage;
    }

    private WrongQuestionVO toWrongQuestionVO(WrongQuestion wq) {
        WrongQuestionVO vo = new WrongQuestionVO();
        vo.setId(wq.getId());
        vo.setQuestionId(wq.getQuestionId());
        vo.setStudentAnswer(wq.getStudentAnswer());
        vo.setMastered(wq.getMastered() == null ? 0 : wq.getMastered());
        Question q = questionMapper.selectById(wq.getQuestionId());
        if (q != null) {
            vo.setContent(q.getContent());
            vo.setType(q.getType());
            vo.setCorrectAnswer(q.getCorrectAnswer());
            vo.setAnalysis(q.getAnalysis());
            // 选项: Question 实体无 options 字段, 走 question_option 表查
            vo.setOptions(loadOptionsForQuestion(q.getId()));
        }
        return vo;
    }

    /**
     * 按 questionId 从 question_option 表查所有选项, 转 List<OptionVO>
     * <p>
     * 排序: 先 sort ASC, 再 id ASC
     * </p>
     */
    private List<OptionVO> loadOptionsForQuestion(Long questionId) {
        if (questionId == null) return Collections.emptyList();
        LambdaQueryWrapper<QuestionOption> w = new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getSort)
                .orderByAsc(QuestionOption::getId);
        List<QuestionOption> opts = questionOptionMapper.selectList(w);
        if (CollectionUtils.isEmpty(opts)) return Collections.emptyList();
        List<OptionVO> out = new ArrayList<>(opts.size());
        for (QuestionOption o : opts) {
            OptionVO v = new OptionVO();
            v.setId(o.getId());
            v.setLabel(o.getLabel());
            v.setContent(o.getContent());
            out.add(v);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWrongQuestion(Long studentId, Long wrongQuestionId) {
        WrongQuestion wq = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wq == null || !wq.getStudentId().equals(studentId)) {
            throw new BusinessException("错题不存在");
        }
        wrongQuestionMapper.deleteById(wrongQuestionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearWrongQuestions(Long studentId) {
        wrongQuestionMapper.delete(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getStudentId, studentId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWrongQuestion(Long studentId, Long wrongQuestionId, Integer mastered) {
        if (mastered == null || (mastered != 0 && mastered != 1)) {
            throw new BusinessException("mastered 必须是 0 或 1");
        }
        WrongQuestion wq = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wq == null || !wq.getStudentId().equals(studentId)) {
            throw new BusinessException("错题不存在");
        }
        wq.setMastered(mastered);
        wrongQuestionMapper.updateById(wq);
    }

    @Override
    public List<QuestionVO> practiceWrongQuestions(Long studentId, List<Long> wrongQuestionIds) {
        if (CollectionUtils.isEmpty(wrongQuestionIds)) {
            return new ArrayList<>();
        }
        // 查询错题（确保属于当前学生）
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WrongQuestion::getId, wrongQuestionIds)
                .eq(WrongQuestion::getStudentId, studentId);
        List<WrongQuestion> wrongQuestions = wrongQuestionMapper.selectList(wrapper);

        List<QuestionVO> result = new ArrayList<>();
        for (WrongQuestion wq : wrongQuestions) {
            Question question = questionMapper.selectById(wq.getQuestionId());
            if (question == null) {
                continue;
            }
            QuestionVO vo = new QuestionVO();
            vo.setId(question.getId());
            vo.setType(question.getType());
            vo.setContent(question.getContent());
            vo.setScore(question.getScore());
            // 填空题和简答题不返回选项列表（返回空列表）
            Integer type = question.getType();
            if (type != null && (type == 3 || type == 5)) {
                vo.setOptions(new ArrayList<>());
            } else {
                LambdaQueryWrapper<QuestionOption> optWrapper = new LambdaQueryWrapper<>();
                optWrapper.eq(QuestionOption::getQuestionId, wq.getQuestionId())
                        .orderByAsc(QuestionOption::getSort);
                List<QuestionOption> options = questionOptionMapper.selectList(optWrapper);
                vo.setOptions(options.stream().map(this::toOptionVO).collect(Collectors.toList()));
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 获取正确答案（选项label逗号分隔）
     */
    private String getCorrectAnswer(List<QuestionOption> options) {
        return options.stream()
                .filter(o -> o.getIsCorrect() != null && o.getIsCorrect() == 1)
                .map(QuestionOption::getLabel)
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * 将选项实体转换为OptionVO
     */
    private OptionVO toOptionVO(QuestionOption option) {
        OptionVO vo = new OptionVO();
        vo.setId(option.getId());
        vo.setLabel(option.getLabel());
        vo.setContent(option.getContent());
        return vo;
    }
}
