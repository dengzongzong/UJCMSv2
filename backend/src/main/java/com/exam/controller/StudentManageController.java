package com.exam.controller;

import com.alibaba.excel.EasyExcel;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.OpenCourseDTO;
import com.exam.dto.OpenExamDTO;
import com.exam.dto.StudentImportDTO;
import com.exam.dto.StudentSearchDTO;
import com.exam.entity.Student;
import com.exam.service.StudentManageService;
import com.exam.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 学生管理
 */
@RestController
@RequestMapping("/admin/student")
public class StudentManageController {
    @Autowired
    private StudentManageService studentManageService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private com.exam.service.CertificateUserSyncService certificateUserSyncService;

    /**
     * 分页查询学生
     */
    @GetMapping("/page")
    public Result<PageResult<Student>> page(StudentSearchDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Student> result = studentManageService.page(dto);
        return Result.success(result);
    }

    /**
     * 新增学生
     */
    @PostMapping
    public Result<Void> add(@RequestBody Student student) {
        studentManageService.addStudent(student);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        studentManageService.updateStudent(student);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        studentManageService.deleteStudent(id);
        return Result.success();
    }

    /**
     * 批量删除学生
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        studentManageService.batchDeleteStudents(ids);
        return Result.success();
    }

    /**
     * 学生详情（含已开通课程和考试列表）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> result = studentManageService.detail(id);
        return Result.success(result);
    }

    /**
     * 冻结/解冻学生（切换status）
     */
    @PutMapping("/freeze/{id}")
    public Result<Void> freeze(@PathVariable Long id) {
        studentManageService.freeze(id);
        return Result.success();
    }

    /**
     * 获取学生已开通和未开通的课程列表
     */
    @GetMapping("/{id}/courses")
    public Result<Map<String, Object>> courses(@PathVariable Long id) {
        Map<String, Object> result = studentManageService.courses(id);
        return Result.success(result);
    }

    /**
     * 开通课程
     */
    @PostMapping("/open-courses")
    public Result<Void> openCourses(@RequestBody OpenCourseDTO dto) {
        studentManageService.openCourses(dto);
        return Result.success();
    }

    /**
     * 开通考试
     */
    @PostMapping("/open-exams")
    public Result<Void> openExams(@RequestBody OpenExamDTO dto) {
        studentManageService.openExams(dto);
        return Result.success();
    }

    /**
     * 取消开通课程（删除学生课程开通记录）
     */
    @DeleteMapping("/close-course")
    public Result<Void> closeCourse(@RequestParam Long studentId,
                                    @RequestParam Long courseId) {
        studentManageService.closeCourse(studentId, courseId);
        return Result.success();
    }

    /**
     * 取消开通考试（删除学生考试开通记录）
     */
    @DeleteMapping("/close-exam")
    public Result<Void> closeExam(@RequestParam Long studentId,
                                  @RequestParam Long examId) {
        studentManageService.closeExam(studentId, examId);
        return Result.success();
    }

    /**
     * 获取学生已开通和未开通的考试列表
     */
    @GetMapping("/{id}/exams")
    public Result<Map<String, Object>> exams(@PathVariable Long id) {
        Map<String, Object> result = studentManageService.exams(id);
        return Result.success(result);
    }

    /**
     * 批量导入学生（Excel文件上传方式）
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importStudents(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = studentManageService.importStudentsFromExcel(file);
        return Result.success(result);
    }

    /**
     * 批量导入学生（手机号列表方式，前端兼容）
     */
    @PostMapping("/import-batch")
    public Result<Void> importStudentsBatch(@RequestBody StudentImportDTO dto) {
        studentManageService.importStudents(dto);
        return Result.success();
    }

    /**
     * 手动同步某个学生到证书管理
     */
    @PostMapping("/sync-certificate/{id}")
    public Result<Void> syncCertificate(@PathVariable Long id) {
        Student student = studentManageService.getById(id);
        if (student == null) {
            return Result.error("学生不存在");
        }
        certificateUserSyncService.syncStudent(student);
        return Result.success();
    }

    /**
     * 下载学生导入模板（25列,独立于证书管理模板）
     */
    @GetMapping("/import/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        certificateService.downloadStudentTemplate(response, "学生导入模板", "学生导入模板.xlsx");
    }
}
