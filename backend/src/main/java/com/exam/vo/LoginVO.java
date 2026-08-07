package com.exam.vo;

import lombok.Data;

/**
 * 登录返回结果
 */
@Data
public class LoginVO {

    /**
     * JWT 令牌
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名（管理员为账号，学生为手机号）
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 角色：admin / student
     */
    private String role;

    /**
     * 学生所属专业 ID(student 角色才会有值)
     * <p>用于前端判断:登录后是否需要跳"选择专业页"</p>
     * <p>非空 → 已有专业,跳过选择页;为空 → 需要跳选专业页</p>
     */
    private Long professionId;

    /**
     * 学生所属专业名称
     */
    private String professionName;

    /** 管理员权限列表(仅admin角色有值) */
    private java.util.List<String> permissions;

    /** 子管理员可操作的证书类型名称列表(仅admin子管理员有值) */
    private java.util.List<String> certTypeIds;

    /** 是否超级管理员: 1=是, 0=子管理员 */
    private Integer isSuper;
}
