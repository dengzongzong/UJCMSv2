package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.ExamDTO;
import com.exam.entity.Exam;
import com.exam.entity.Student;
import com.exam.service.ExamManageService;
import org.springframework.beans.factory.annotation.Autowired;
import com.exam.annotation.RequirePermission;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题库/考试管理
 */
@RestController
@RequestMapping("/admin/exam")
public class ExamManageController {

    @Autowired
    private ExamManageService examManageService;

    /**
     * 分页查询考试
     */
    @GetMapping("/page")
    public Result<PageResult<Exam>> page(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String createTimeStart,
                                         @RequestParam(required = false) String createTimeEnd,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) Long professionId,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Exam> result = examManageService.page(page, size, name, category, createTimeStart, createTimeEnd, status, professionId);
        return Result.success(result);
    }

    /**
     * 考试详情（含题目列表）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> result = examManageService.detail(id);
        return Result.success(result);
    }

    /**
     * 新增考试
     */
    @PostMapping
    public Result<Void> add(@RequestBody ExamDTO dto) {
        examManageService.add(dto);
        return Result.success();
    }

    /**
     * 编辑考试
     */
    @PutMapping
    public Result<Void> update(@RequestBody ExamDTO dto) {
        examManageService.update(dto);
        return Result.success();
    }

    /**
     * 删除考试
     */
    @RequirePermission("delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        examManageService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除考试
     */
    @RequirePermission("delete")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        examManageService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 查看已开通该考试的学生名单（分页，支持未开通查询、手机号和身份证号搜索、显示最新N条）
     */
    @GetMapping("/{id}/students")
    public Result<PageResult<Student>> students(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam(required = false) String idCard,
                                                @RequestParam(required = false) Integer exactCount,
                                                @RequestParam(required = false) Integer unopened,
                                                @RequestParam(required = false) Integer unexamined,
                                                @RequestParam(required = false) String profession) {
        PageResult<Student> result = examManageService.studentsPage(id, page, size, phone, idCard, exactCount, unopened, unexamined, profession);
        return Result.success(result);
    }

    /**
     * 为考试新增开通学生
     */
    @PostMapping("/open-students")
    public Result<Void> openStudents(@RequestBody Map<String, Object> params) {
        Long examId = Long.valueOf(params.get("examId").toString());
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) params.get("studentIds");
        List<Long> studentIds = rawIds.stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
        examManageService.openStudents(examId, studentIds);
        return Result.success();
    }

    /**
     * 自动考试（为已开通学生一键生成考试记录）
     */
    @PostMapping("/auto-exam")
    public Result<Void> autoExam(@RequestBody Map<String, Object> params) {
        Long examId = Long.valueOf(params.get("examId").toString());
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) params.get("studentIds");
        List<Long> studentIds = rawIds.stream()
                .map(o -> Long.valueOf(o.toString()))
                .collect(Collectors.toList());
        examManageService.autoExam(examId, studentIds);
        return Result.success();
    }

    /**
     * 取消开通（删除某学生的考试开通记录）
     */
    @DeleteMapping("/close-student")
    public Result<Void> closeStudent(@RequestParam Long examId,
                                     @RequestParam Long studentId) {
        examManageService.closeStudent(examId, studentId);
        return Result.success();
    }

    /**
     * 修复历史自动考试记录:为缺少 exam_answer 明细的考试记录补生成作答明细
     * <p>解决"查看解析"题目和答案为空的问题</p>
     */
    @PostMapping("/fix-exam-answers")
    public Result<Map<String, Object>> fixExamAnswers() {
        int fixed = examManageService.fixMissingExamAnswers();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("fixed", fixed);
        return Result.success(data);
    }
}
