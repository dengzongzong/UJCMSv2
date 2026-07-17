package com.exam.service;

import com.exam.dto.ChooseSubjectDTO;
import com.exam.dto.LoginDTO;
import com.exam.dto.RegisterDTO;
import com.exam.dto.ResetPasswordDTO;
import com.exam.vo.LoginVO;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册
     */
    void register(RegisterDTO dto);

    /**
     * 重置密码(未登录态, 走图形验证码)
     */
    void resetPassword(ResetPasswordDTO dto);

    /**
     * 登录态修改密码(已登录, 需要原密码)
     * @param studentId 学员ID(从 token 解析)
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    void changePassword(Long studentId, String oldPassword, String newPassword);

    /**
     * 选择专业科目
     *
     * @param studentId 学生ID
     */
    void chooseSubject(Long studentId, ChooseSubjectDTO dto);

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户ID
     * @param role   角色
     */
    LoginVO getCurrentUserInfo(Long userId, String role);
}
