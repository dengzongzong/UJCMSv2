package com.exam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.dto.ProfileUpdateDTO;
import com.exam.entity.Student;
import com.exam.vo.ProfileVO;
import com.exam.vo.QuestionVO;
import com.exam.vo.WrongQuestionVO;

import java.util.List;

/**
 * 个人中心Service
 */
public interface ProfileService extends IService<Student> {

    /**
     * 获取个人信息
     */
    ProfileVO getProfile(Long studentId);

    /**
     * 更新个人信息
     */
    void updateProfile(Long studentId, ProfileUpdateDTO dto);

    /**
     * 我的错题列表(分页)
     *
     * @param type 0=全部, 1=未掌握(已掌握过滤掉)
     * @param examId 考试筛选
     * @param keyword 关键词筛选(题目内容)
     */
    Page<WrongQuestionVO> getWrongQuestions(Long studentId, Integer type, Long examId,
                                            String keyword, Integer page, Integer size);

    /**
     * 移除错题
     */
    void removeWrongQuestion(Long studentId, Long wrongQuestionId);

    /**
     * 清空学员的所有错题
     */
    void clearWrongQuestions(Long studentId);

    /**
     * 更新错题状态(已掌握/未掌握)
     */
    void updateWrongQuestion(Long studentId, Long wrongQuestionId, Integer mastered);

    /**
     * 错题练习（返回题目列表，不含正确答案）
     */
    List<QuestionVO> practiceWrongQuestions(Long studentId, List<Long> wrongQuestionIds);
}
