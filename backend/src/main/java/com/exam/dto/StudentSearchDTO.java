package com.exam.dto;

import lombok.Data;

/**
 * 学生搜索DTO
 */
@Data
public class StudentSearchDTO {
    /** 手机号 */
    private String phone;
    /** 身份证号(支持模糊搜索) */
    private String idCard;
    /** 关键词（搜索姓名/手机号/学号/身份证） */
    private String keyword;
    /** 注册开始时间 yyyy-MM-dd */
    private String registerTimeStart;
    /** 注册结束时间 yyyy-MM-dd */
    private String registerTimeEnd;
    /** 账号状态 0-冻结 1-正常 */
    private Integer status;
    /** 专业ID */
    private Long professionId;
    /** 当前页 */
    private Integer page;
    /** 每页大小 */
    private Integer size;
}
