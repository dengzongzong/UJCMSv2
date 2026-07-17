package com.exam.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class CertificateUserExportVO {

    @ExcelProperty(value = "序号", index = 0)
    @ColumnWidth(8)
    private Integer serialNumber;

    @ExcelProperty(value = "姓名", index = 1)
    @ColumnWidth(12)
    private String name;

    @ExcelProperty(value = "性别", index = 2)
    @ColumnWidth(8)
    private String gender;

    @ExcelProperty(value = "证件号码", index = 3)
    @ColumnWidth(18)
    private String idCard;

    @ExcelProperty(value = "职业名称", index = 4)
    @ColumnWidth(15)
    private String professionName;

    @ExcelProperty(value = "技能等级", index = 5)
    @ColumnWidth(12)
    private String skillLevel;

    @ExcelProperty(value = "证书编号", index = 6)
    @ColumnWidth(20)
    private String certNo;

    @ExcelProperty(value = "颁发日期", index = 7)
    @ColumnWidth(15)
    private String issueDate;

    @ExcelProperty(value = "报单机构", index = 8)
    @ColumnWidth(20)
    private String agency;

    @ExcelProperty(value = "报单机构费用统计", index = 9)
    @ColumnWidth(15)
    private String agencyFee;

    @ExcelProperty(value = "培训专业", index = 10)
    @ColumnWidth(15)
    private String trainingProfession;

    @ExcelProperty(value = "培训学时", index = 11)
    @ColumnWidth(10)
    private String trainingHours;

    @ExcelProperty(value = "培训日期", index = 12)
    @ColumnWidth(15)
    private String trainingDate;

    @ExcelProperty(value = "理论成绩", index = 13)
    @ColumnWidth(10)
    private String theoryScore;

    @ExcelProperty(value = "实操成绩", index = 14)
    @ColumnWidth(10)
    private String practicalScore;

    @ExcelProperty(value = "综合测评", index = 15)
    @ColumnWidth(10)
    private String comprehensiveAssessment;

    @ExcelProperty(value = "证书二维码生成1", index = 16)
    @ColumnWidth(20)
    private String qrUrl1;

    @ExcelProperty(value = "证书二维码生成2", index = 17)
    @ColumnWidth(20)
    private String qrUrl2;

    @ExcelProperty(value = "证书二维码生成3", index = 18)
    @ColumnWidth(20)
    private String qrUrl3;

    @ExcelProperty(value = "学员考试二维码", index = 19)
    @ColumnWidth(20)
    private String examQrUrl;
}
