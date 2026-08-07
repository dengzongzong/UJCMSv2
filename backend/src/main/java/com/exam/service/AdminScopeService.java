package com.exam.service;

import com.exam.common.BusinessException;
import com.exam.entity.Admin;
import com.exam.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 子管理员数据范围服务: 按证书类型限制子管理员可操作的数据。
 * 超管(isSuper=1)或非登录上下文(定时任务等)不过滤;
 * 子管理员通过 admin.cert_type_ids(JSON数组,存证书类型名称)限制范围。
 */
@Service
public class AdminScopeService {

    @Autowired
    private AdminMapper adminMapper;

    /** 获取当前登录管理员, 无登录上下文时返回 null */
    public Admin getCurrentAdmin() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        try {
            return adminMapper.selectById(Long.valueOf(userId.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 当前管理员可操作的证书类型名称列表。
     * 返回 null = 超管或系统上下文, 不过滤(全部可见);
     * 返回空列表 = 子管理员未配置任何证书类型, 看不到任何数据;
     * 否则为授权的证书类型名称列表。
     */
    public List<String> scopeCertTypes() {
        Admin admin = getCurrentAdmin();
        if (admin == null) {
            return null;
        }
        if (admin.getIsSuper() != null && admin.getIsSuper() == 1) {
            return null;
        }
        return parseCertTypes(admin.getCertTypeIds());
    }

    /** 当前管理员是否为超管(或系统上下文) */
    public boolean isUnrestricted() {
        List<String> scope = scopeCertTypes();
        return scope == null;
    }

    /** 判断证书类型是否在授权范围内(超管永远返回 true) */
    public boolean canOperateCertType(String certType) {
        List<String> scope = scopeCertTypes();
        if (scope == null) {
            return true;
        }
        return StringUtils.hasText(certType) && scope.contains(certType.trim());
    }

    /** 校验证书类型是否在授权范围内, 不在则抛异常(用于新增/修改等写操作) */
    public void checkCertType(String certType) {
        if (!canOperateCertType(certType)) {
            if (!StringUtils.hasText(certType)) {
                throw new BusinessException("未指定证书类型, 无法操作");
            }
            throw new BusinessException("无权限操作该证书类型(" + certType.trim() + ")");
        }
    }

    private List<String> parseCertTypes(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<String> list = cn.hutool.json.JSONUtil.parseArray(json).toList(String.class);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
