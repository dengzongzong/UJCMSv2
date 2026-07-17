package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 子管理员新增/编辑DTO
 */
@Data
public class AdminDTO {
    private Long id;
    private String username;
    private String password;
    private String roleName;
    private List<String> permissions;
    private Integer status;
}
