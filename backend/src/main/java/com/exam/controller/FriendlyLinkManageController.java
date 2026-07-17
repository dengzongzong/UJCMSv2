package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.FriendlyLink;
import com.exam.service.FriendlyLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 友情链接(后台)
 */
@RestController
@RequestMapping("/admin/friendly-link")
public class FriendlyLinkManageController {

    @Autowired
    private FriendlyLinkService service;

    @GetMapping("/page")
    public Result<PageResult<FriendlyLink>> page(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) Integer status) {
        return Result.success(service.page(page, size, status));
    }

    @PostMapping
    public Result<Void> save(@RequestBody FriendlyLink entity) {
        service.saveLink(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody FriendlyLink entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        service.saveLink(entity);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> delete(@RequestBody List<Long> ids) {
        service.delete(ids);
        return Result.success();
    }
}
