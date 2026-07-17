package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.CourseDTO;
import com.exam.entity.Student;
import com.exam.service.CourseManageService;
import com.exam.vo.AdminCourseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程管理
 */
@RestController
@RequestMapping("/admin/course")
public class CourseManageController {

    @Autowired
    private CourseManageService courseManageService;

    /**
     * 分页查询课程
     */
    @GetMapping("/page")
    public Result<PageResult<AdminCourseVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String name,
                                                 @RequestParam(required = false) String createTimeStart,
                                                 @RequestParam(required = false) String createTimeEnd,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) Integer sectionCount,
                                                 @RequestParam(required = false) Long professionId,
                                                 @RequestParam(required = false) Long categoryId) {
        PageResult<AdminCourseVO> result = courseManageService.page(page, size, name, createTimeStart,
                createTimeEnd, status, sectionCount, professionId, categoryId);
        return Result.success(result);
    }

    /**
     * 课程详情（含小节和小节视频）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> result = courseManageService.detail(id);
        return Result.success(result);
    }

    /**
     * 新增课程
     */
    @PostMapping
    public Result<Void> add(@RequestBody CourseDTO dto) {
        courseManageService.add(dto);
        return Result.success();
    }

    /**
     * 编辑课程
     */
    @PutMapping
    public Result<Void> update(@RequestBody CourseDTO dto) {
        courseManageService.update(dto);
        return Result.success();
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseManageService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除课程
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        courseManageService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 查看已开通该课程的学生名单（分页，支持未开通查询和手机号搜索）
     */
    @GetMapping("/{id}/students")
    public Result<PageResult<Student>> students(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam(required = false) Integer unopened) {
        PageResult<Student> result = courseManageService.studentsPage(id, page, size, phone, unopened);
        return Result.success(result);
    }

    /**
     * 为课程新增开通学生
     */
    @PostMapping("/open-students")
    public Result<Void> openStudents(@RequestBody Map<String, Object> params) {
        Long courseId = Long.valueOf(params.get("courseId").toString());
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) params.get("studentIds");
        List<Long> studentIds = rawIds.stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
        courseManageService.openStudents(courseId, studentIds);
        return Result.success();
    }

    /**
     * 取消开通（删除某学生的课程开通记录）
     */
    @DeleteMapping("/close-student")
    public Result<Void> closeStudent(@RequestParam Long courseId,
                                     @RequestParam Long studentId) {
        courseManageService.closeStudent(courseId, studentId);
        return Result.success();
    }
}
