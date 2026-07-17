package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.QuestionTemplate;
import com.exam.service.QuestionTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/question-template")
public class QuestionTemplateController {

    @Autowired
    private QuestionTemplateService questionTemplateService;

    @GetMapping("/page")
    public Result<PageResult<QuestionTemplate>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long professionId,
            HttpServletRequest request) {
        return Result.success(questionTemplateService.page(page, size, name, categoryId, professionId));
    }

    @GetMapping("/list")
    public Result<List<QuestionTemplate>> listAll() {
        return Result.success(questionTemplateService.listAll());
    }

    @GetMapping("/{id}")
    public Result<List<Map<String, Object>>> detail(@PathVariable Long id) {
        return Result.success(questionTemplateService.detail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Long categoryId = body.get("categoryId") != null ? Long.valueOf(body.get("categoryId").toString()) : null;
        Long professionId = body.get("professionId") != null ? Long.valueOf(body.get("professionId").toString()) : null;
        @SuppressWarnings("unchecked")
        List<Integer> questionIdsRaw = (List<Integer>) body.get("questionIds");
        List<Long> questionIds = questionIdsRaw != null ? questionIdsRaw.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList()) : null;
        questionTemplateService.create(name, description, categoryId, professionId, questionIds);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Long categoryId = body.get("categoryId") != null ? Long.valueOf(body.get("categoryId").toString()) : null;
        Long professionId = body.get("professionId") != null ? Long.valueOf(body.get("professionId").toString()) : null;
        @SuppressWarnings("unchecked")
        List<Integer> questionIdsRaw = (List<Integer>) body.get("questionIds");
        List<Long> questionIds = questionIdsRaw != null ? questionIdsRaw.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList()) : null;
        questionTemplateService.update(id, name, description, categoryId, professionId, questionIds);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionTemplateService.delete(id);
        return Result.success();
    }
}
