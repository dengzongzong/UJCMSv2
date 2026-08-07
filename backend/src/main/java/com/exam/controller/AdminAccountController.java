package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.Result;
import com.exam.entity.Admin;
import com.exam.service.AdminManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员账号安全(当前登录人修改自己的密码/查看自己的信息)
 * <p>独立控制器,避免与 AdminManageController 的 @DeleteMapping("/{id}") 路径冲突</p>
 */
@RestController
@RequestMapping("/admin/account")
public class AdminAccountController {

    @Autowired
    private AdminManageService adminManageService;

    /**
     * 获取当前登录管理员信息
     */
    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrentAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Admin admin = adminManageService.getById(userId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", admin.getId());
        map.put("username", admin.getUsername());
        map.put("roleName", admin.getRoleName());
        map.put("avatar", admin.getAvatar());
        map.put("isSuper", admin.getIsSuper());
        map.put("status", admin.getStatus());
        map.put("lastLoginTime", admin.getLastLoginTime());
        map.put("createTime", admin.getCreateTime());
        if (admin.getPermissions() != null) {
            try {
                map.put("permissions", cn.hutool.json.JSONUtil.parseArray(admin.getPermissions()));
            } catch (Exception e) {
                map.put("permissions", Collections.emptyList());
            }
        } else {
            map.put("permissions", Collections.emptyList());
        }
        if (admin.getCertTypeIds() != null && !admin.getCertTypeIds().isEmpty()) {
            try {
                map.put("certTypeIds", cn.hutool.json.JSONUtil.parseArray(admin.getCertTypeIds()));
            } catch (Exception e) {
                map.put("certTypeIds", Collections.emptyList());
            }
        } else {
            map.put("certTypeIds", Collections.emptyList());
        }
        return Result.success(map);
    }

    /**
     * 修改当前登录管理员密码(需要验证原密码)
     */
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (!StringUtils.hasText(oldPassword)) {
            throw new BusinessException("请输入原密码");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException("请输入新密码");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        adminManageService.changePassword(userId, oldPassword, newPassword);
        return Result.success();
    }
}
