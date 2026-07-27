package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.ExamDTO;
import com.exam.entity.Exam;
import com.exam.entity.Student;

import java.util.List;
import java.util.Map;

public interface ExamManageService extends IService<Exam> {

    /**
     * 分页查询考试
     */
    PageResult<Exam> page(Integer page, Integer size, String name, String category,
                          String createTimeStart, String createTimeEnd, Integer status, Long professionId);

    /**
     * 考试详情（含题目列表）
     */
    Map<String, Object> detail(Long id);

    /**
     * 新增考试（含题目关联）
     */
    void add(ExamDTO dto);

    /**
     * 编辑考试（先删除旧题目关联再重新创建）
     */
    void update(ExamDTO dto);

    /**
     * 删除考试（含题目关联、学生开通）
     */
    void delete(Long id);

    /**
     * 批量删除考试（级联删除关联数据）
     */
    void batchDelete(List<Long> ids);

    /**
     * 查看已开通该考试的学生名单
     */
    List<Student> students(Long id);

    /**
     * 分页查询学生名单（支持未开通查询、手机号和身份证号搜索、显示最新N条）
     * unopened == null 或 0：查询已开通该考试的学生
     * unopened == 1：查询未开通该考试的学生
     * exactCount != null 且 > 0：重置为第1页，每页条数=exactCount，用于显示最新N条
     */
    PageResult<Student> studentsPage(Long examId, Integer page, Integer size, String phone, String idCard, Integer exactCount, Integer unopened, Integer unexamined);

    /**
     * 为考试新增开通学生
     */
    void openStudents(Long examId, List<Long> studentIds);

    /**
     * 取消开通（删除某学生的考试开通记录）
     */
    void closeStudent(Long examId, Long studentId);

    /**
     * 自动考试（为已开通学生一键生成考试记录）
     */
    void autoExam(Long examId, List<Long> studentIds);

    /**
     * 修复历史自动考试记录:为缺少 exam_answer 明细的考试记录补生成作答明细
     * @return 修复的记录数
     */
    int fixMissingExamAnswers();
}
