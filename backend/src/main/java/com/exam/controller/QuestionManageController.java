package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.QuestionDTO;
import com.exam.service.QuestionManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 题目管理
 */
@RestController
@RequestMapping("/admin/question")
public class QuestionManageController {

    @Autowired
    private QuestionManageService questionManageService;

    /**
     * 分页查询题目
     */
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) Integer type,
                                                        @RequestParam(required = false) Long categoryId,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String createTimeStart,
                                                        @RequestParam(required = false) String createTimeEnd,
                                                        @RequestParam(required = false) Integer enabled,
                                                        @RequestParam(required = false) Long professionId,
                                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Map<String, Object>> result = questionManageService.page(page, size, type, categoryId,
                keyword, createTimeStart, createTimeEnd, enabled, professionId);
        return Result.success(result);
    }

    /**
     * 题目详情（含选项）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> result = questionManageService.detail(id);
        return Result.success(result);
    }

    /**
     * 题干重复检测：新增/编辑试题前，按题目分类+专业+题干查询库中是否已存在相同题目，
     * 返回重复题目详情列表(含选项/答案/解析)供前端对比展示。
     * 重复标准: 题目分类+专业+题干(归一化后) 完全一致才算重复
     */
    @GetMapping("/check-duplicate")
    public Result<List<Map<String, Object>>> checkDuplicate(@RequestParam String content,
                                                             @RequestParam(required = false) Long categoryId,
                                                             @RequestParam(required = false) Long professionId,
                                                             @RequestParam(required = false) Long excludeId,
                                                             @RequestParam(required = false) Integer type) {
        return Result.success(questionManageService.checkDuplicate(content, categoryId, professionId, excludeId, type));
    }

    /**
     * 新增题目
     */
    @PostMapping
    public Result<Void> add(@RequestBody QuestionDTO dto) {
        questionManageService.add(dto);
        return Result.success();
    }

    /**
     * 编辑题目
     */
    @PutMapping
    public Result<Void> update(@RequestBody QuestionDTO dto) {
        questionManageService.update(dto);
        return Result.success();
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionManageService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除题目
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        questionManageService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 导出题目为Excel
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) Integer type,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String createTimeStart,
                       @RequestParam(required = false) String createTimeEnd,
                       @RequestParam(required = false) Integer enabled,
                       @RequestParam(required = false) Long professionId,
                       HttpServletResponse response) {
        questionManageService.export(response, type, categoryId, keyword, createTimeStart, createTimeEnd, enabled, professionId);
    }

    /**
     * 批量导入题目(返回成功条数和失败明细)
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file) {
        return Result.success(questionManageService.importQuestions(file));
    }

    /**
     * 下载题目导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        questionManageService.downloadTemplate(response);
    }
}
