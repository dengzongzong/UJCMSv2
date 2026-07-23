package com.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Excel 导入时单行映射,与 Excel 列严格对应(统一20列模板)
 */
@Data
public class CertificateImportRow {
    private String name;
    private String idCard;
    private String profession;
    private String skillLevel;
    /** 完整证书编号(如 ZGZH20260515M1000) */
    private String certNo;
    private LocalDate issueDate;
    private String certNoPrefix;
    private String certNoMiddle;
    private String studentNoPrefix;
    private String studentNoMiddle;
    private String agency;
    private BigDecimal agencyFee;
    /** 培训专业 */
    private String trainingMajor;
    /** 培训学时 */
    private String trainingHours;
    /** 培训日期 */
    private String trainingDate;
    /** 考试时间 */
    private String examTime;
    /** 理论成绩 */
    private String theoryScore;
    /** 实操成绩(文本,如"合格") */
    private String practicalScore;
    /** 综合测评 */
    private String comprehensiveEvaluation;
    /** 手机号码 */
    private String phone;
    /** 性别(Excel原文"男"/"女") */
    private String genderStr;
    private String qr1;
    private String qr2;
    private String qr3;
    private String examQr;
    /** 证书类型(专业技能证书/专项职业技能证书/人才数据入库证书) */
    private String certType;
    private String remark;
    /** 行号(用于前端错误回显) */
    private Integer rowIndex;
    /** 行级错误(校验失败原因) */
    private String error;
}
