package com.exam.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentImportVO {
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("手机号")
    private String phone;
    @ExcelProperty("身份证号")
    private String idCard;
    @ExcelProperty("专业名称")
    private String professionName;
}
