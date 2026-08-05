package com.exam.aspect;

import com.exam.annotation.RequirePermission;
import com.exam.annotation.RequireSuper;
import com.exam.common.BusinessException;
import com.exam.entity.Admin;
import com.exam.mapper.AdminMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private AdminMapper adminMapper;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        Long userId = getCurrentUserId();
        String requiredPerm = requirePermission.value();

        Admin admin = adminMapper.selectById(userId);
        if (admin == null) {
            throw new BusinessException(403, "用户不存在");
        }

        // 超级管理员拥有全部权限
        if (admin.getIsSuper() != null && admin.getIsSuper() == 1) {
            return joinPoint.proceed();
        }

        // 检查子管理员是否有该权限
        List<String> permissions = parsePermissions(admin.getPermissions());
        if (!permissions.contains(requiredPerm)) {
            throw new BusinessException(403, "无操作权限，请联系主账号开启");
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(requireSuper)")
    public Object checkSuper(ProceedingJoinPoint joinPoint, RequireSuper requireSuper) throws Throwable {
        Long userId = getCurrentUserId();
        Admin admin = adminMapper.selectById(userId);
        if (admin == null || admin.getIsSuper() == null || admin.getIsSuper() != 1) {
            throw new BusinessException(403, "仅超级管理员可执行此操作");
        }
        return joinPoint.proceed();
    }

    private Long getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(403, "无法获取请求信息");
        }
        HttpServletRequest request = attrs.getRequest();
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(403, "未登录");
        }
        return Long.valueOf(userId.toString());
    }

    private List<String> parsePermissions(String perms) {
        if (perms == null || perms.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        try {
            return cn.hutool.json.JSONUtil.parseArray(perms).toList(String.class);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}
