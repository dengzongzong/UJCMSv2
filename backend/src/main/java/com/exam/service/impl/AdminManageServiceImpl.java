package com.exam.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.AdminDTO;
import com.exam.entity.Admin;
import com.exam.mapper.AdminMapper;
import com.exam.service.AdminManageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminManageServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminManageService {

    @Override
    public PageResult<Admin> page(Integer page, Integer size, String username) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<Admin>()
                .eq(Admin::getIsSuper, 0)
                .like(StringUtils.hasText(username), Admin::getUsername, username)
                .orderByDesc(Admin::getCreateTime);
        Page<Admin> p = new Page<>(page, size);
        Page<Admin> result = this.page(p, wrapper);
        // 密码不返回
        result.getRecords().forEach(a -> a.setPassword(null));
        return new PageResult<>(result);
    }

    @Override
    public void add(AdminDTO dto) {
        // 校验用户名唯一
        long count = this.count(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        Admin admin = new Admin();
        admin.setUsername(dto.getUsername());
        admin.setPassword(BCrypt.hashpw(dto.getPassword()));
        admin.setRoleName(dto.getRoleName());
        admin.setIsSuper(0);
        admin.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getPermissions() != null) {
            admin.setPermissions(JSONUtil.toJsonStr(dto.getPermissions()));
        }
        if (dto.getCertTypeIds() != null) {
            admin.setCertTypeIds(JSONUtil.toJsonStr(dto.getCertTypeIds()));
        }
        this.save(admin);
    }

    @Override
    public void update(AdminDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        Admin admin = this.getById(dto.getId());
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        // 校验用户名唯一（排除自身）
        long count = this.count(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, dto.getUsername())
                .ne(Admin::getId, dto.getId()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        admin.setUsername(dto.getUsername());
        admin.setRoleName(dto.getRoleName());
        if (dto.getStatus() != null) {
            admin.setStatus(dto.getStatus());
        }
        if (dto.getPermissions() != null) {
            admin.setPermissions(JSONUtil.toJsonStr(dto.getPermissions()));
        }
        if (dto.getCertTypeIds() != null) {
            admin.setCertTypeIds(JSONUtil.toJsonStr(dto.getCertTypeIds()));
        }
        // 如果传了密码则修改密码
        if (StringUtils.hasText(dto.getPassword())) {
            admin.setPassword(BCrypt.hashpw(dto.getPassword()));
        }
        this.updateById(admin);
    }

    @Override
    public void delete(Long id) {
        Admin admin = this.getById(id);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (admin.getIsSuper() != null && admin.getIsSuper() == 1) {
            throw new BusinessException("超级管理员不可删除");
        }
        this.removeById(id);
    }

    @Override
    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        Admin admin = this.getById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (!cn.hutool.crypto.digest.BCrypt.checkpw(oldPassword, admin.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        admin.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw(newPassword));
        this.updateById(admin);
    }
}
