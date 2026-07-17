package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.OpenCourseDTO;
import com.exam.dto.OpenExamDTO;
import com.exam.dto.StudentImportDTO;
import com.exam.dto.StudentSearchDTO;
import com.exam.entity.Student;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface StudentManageService extends IService<Student> {

    /**
     * 分页查询学生
     */
    PageResult<Student> page(StudentSearchDTO dto);

    /**
     * 新增学生
     */
    void addStudent(Student student);

    /**
     * 修改学生(基础信息 + 密码[仅当 password 非空时])
     */
    void updateStudent(Student student);

    /**
     * 删除学生(级联清理该学生所有关联数据)
     */
    void deleteStudent(Long id);

    /**
     * 批量删除学生(级联清理每个学生的所有关联数据)
     */
    void batchDeleteStudents(List<Long> ids);

    /**
     * 学生详情（含已开通课程和考试列表）
     */
    Map<String, Object> detail(Long id);

    /**
     * 冻结/解冻学生（切换status）
     */
    void freeze(Long id);

    /**
     * 获取学生已开通和未开通的课程列表
     */
    Map<String, Object> courses(Long id);

    /**
     * 开通课程
     */
    void openCourses(OpenCourseDTO dto);

    /**
     * 获取学生已开通和未开通的考试列表
     */
    Map<String, Object> exams(Long id);

    /**
     * 开通考试
     */
    void openExams(OpenExamDTO dto);

    /**
     * 取消开通课程（删除学生课程开通记录）
     */
    void closeCourse(Long studentId, Long courseId);

    /**
     * 取消开通考试（删除学生考试开通记录）
     */
    void closeExam(Long studentId, Long examId);

    /**
     * 批量导入学生（手机号列表方式）
     */
    void importStudents(StudentImportDTO dto);

    /**
     * 通过Excel文件批量导入学生
     * 返回导入成功数量和失败列表
     */
    Map<String, Object> importStudentsFromExcel(MultipartFile file);
}
