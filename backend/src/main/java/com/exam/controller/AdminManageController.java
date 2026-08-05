package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.AdminDTO;
import com.exam.entity.Admin;
import com.exam.service.AdminManageService;
import org.springframework.beans.factory.annotation.Autowired;
import com.exam.annotation.RequireSuper;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 子管理员管理
 */
@RestController
@RequestMapping("/admin/admin")
public class AdminManageController {

    @Autowired
    private AdminManageService adminManageService;

    /**
     * 分页查询子管理员（isSuper=0）
     */
    @GetMapping("/page")
    public Result<PageResult<Admin>> page(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String username,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Admin> result = adminManageService.page(page, size, username);
        return Result.success(result);
    }

    /**
     * 新增子管理员
     */
    @RequireSuper
    @PostMapping
    public Result<Void> add(@RequestBody AdminDTO dto) {
        adminManageService.add(dto);
        return Result.success();
    }

    /**
     * 编辑子管理员
     */
    @RequireSuper
    @PutMapping
    public Result<Void> update(@RequestBody AdminDTO dto) {
        adminManageService.update(dto);
        return Result.success();
    }

    /**
     * 删除子管理员
     */
    @RequireSuper
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminManageService.delete(id);
        return Result.success();
    }
}
