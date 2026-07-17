package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.AdminDTO;
import com.exam.entity.Admin;

public interface AdminManageService extends IService<Admin> {

    /**
     * 分页查询子管理员（isSuper=0）
     */
    PageResult<Admin> page(Integer page, Integer size, String username);

    /**
     * 新增子管理员
     */
    void add(AdminDTO dto);

    /**
     * 编辑子管理员（不传password则不修改密码）
     */
    void update(AdminDTO dto);

    /**
     * 删除子管理员
     */
    void delete(Long id);

    /**
     * 修改管理员密码(需要验证原密码)
     */
    void changePassword(Long adminId, String oldPassword, String newPassword);
}
