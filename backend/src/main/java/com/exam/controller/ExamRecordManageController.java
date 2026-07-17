package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.service.ExamRecordManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 考试记录管理
 */
@RestController
@RequestMapping("/admin/exam-record")
public class ExamRecordManageController {

    @Autowired
    private ExamRecordManageService examRecordManageService;

    /**
     * 分页查询考试记录
     */
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) String examName,
                                                        @RequestParam(required = false) String phone,
                                                        @RequestParam(required = false) Integer submitStatus,
                                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Map<String, Object>> result = examRecordManageService.page(page, size, examName, phone, submitStatus);
        return Result.success(result);
    }

    /**
     * 考试记录详情（含学生作答详情）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> result = examRecordManageService.detail(id);
        return Result.success(result);
    }

    /**
     * 导出考试记录为Excel
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String examName,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) Integer submitStatus,
                       HttpServletResponse response) {
        examRecordManageService.export(response, examName, phone, submitStatus);
    }

    /**
     * 批改简答题(人工评分)
     */
    @PostMapping("/{id}/grade")
    public Result<Void> grade(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> grades = (List<Map<String, Object>>) body.get("grades");
        examRecordManageService.gradeAnswers(id, grades);
        return Result.success();
    }

    /**
     * 删除考试记录（同时删除关联答题记录）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        examRecordManageService.deleteRecord(id);
        return Result.success();
    }

    /**
     * 批量删除考试记录
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        examRecordManageService.batchDeleteRecords(ids);
        return Result.success();
    }
}
