package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.CourseDTO;
import com.exam.entity.Course;
import com.exam.entity.Student;
import com.exam.vo.AdminCourseVO;

import java.util.List;
import java.util.Map;

public interface CourseManageService extends IService<Course> {

    /**
     * 分页查询课程
     */
    PageResult<AdminCourseVO> page(Integer page, Integer size, String name,
                                   String createTimeStart, String createTimeEnd,
                                   Integer status, Integer sectionCount, Long professionId, Long categoryId);

    /**
     * 课程详情（含小节和小节视频）
     */
    Map<String, Object> detail(Long id);

    /**
     * 新增课程（含小节和视频关联）
     */
    void add(CourseDTO dto);

    /**
     * 编辑课程（先删除旧小节和视频关联再重新创建）
     */
    void update(CourseDTO dto);

    /**
     * 删除课程（含关联小节、视频关联、学生开通）
     */
    void delete(Long id);

    /**
     * 批量删除课程（级联删除关联小节、视频关联、学生开通）
     */
    void batchDelete(List<Long> ids);

    /**
     * 查看已开通该课程的学生名单
     */
    List<Student> students(Long id);

    /**
     * 分页查询学生名单（支持未开通查询和手机号搜索）
     * unopened == null 或 0：查询已开通该课程的学生
     * unopened == 1：查询未开通该课程的学生
     */
    PageResult<Student> studentsPage(Long courseId, Integer page, Integer size, String phone, Integer unopened);

    /**
     * 为课程新增开通学生
     */
    void openStudents(Long courseId, List<Long> studentIds);

    /**
     * 取消开通（删除某学生的课程开通记录）
     */
    void closeStudent(Long courseId, Long studentId);
}
