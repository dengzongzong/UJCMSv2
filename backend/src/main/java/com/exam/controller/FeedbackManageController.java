package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.FeedbackMessage;
import com.exam.service.FeedbackMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 合作咨询/投诉建议-管理后台
 */
@RestController
@RequestMapping("/admin/feedback")
public class FeedbackManageController {

    @Autowired
    private FeedbackMessageService service;

    @GetMapping("/page")
    public Result<PageResult<FeedbackMessage>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(service.page(page, size, type, status, keyword));
    }

    @PutMapping("/handle/{id}")
    public Result<Void> handle(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body == null ? null : body.get("remark");
        service.markHandled(id, remark);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> delete(@RequestBody List<Long> ids) {
        service.deleteBatch(ids);
        return Result.success();
    }
}
