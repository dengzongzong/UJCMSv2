package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量导入学生DTO
 */
@Data
public class StudentImportDTO {
    /** 手机号列表 */
    private List<String> phones;
}
