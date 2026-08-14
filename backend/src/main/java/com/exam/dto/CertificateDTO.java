package com.exam.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 证书表单 DTO
 */
@Data
public class CertificateDTO {
    private Long id;
    private String certNo;
    private String studentNo;
    private String name;
    private String idCard;
    private Integer gender;
    /** 出生日期(从身份证号自动提取) */
    private LocalDate birthDate;
    private String profession;
    private String skillLevel;
    private LocalDate issueDate;
    private String certNoPrefix;
    private String certNoMiddle;
    private String studentNoPrefix;
    private String studentNoMiddle;
    private String agency;
    private BigDecimal agencyFee;
    private String qrUrl1;
    private String qrUrl2;
    private String qrUrl3;
    private String examQrUrl;
    private Integer examQrEnabled;
    /** 自定义字段,key 是 fieldKey,value 是对应值 */
    private Map<String, Object> extra;
    private String remark;
    /** 已绑定的证书模板ID */
    private Long templateId;
    /** 证书类型(与certificate_type.name对应,用于自动绑定模板) */
    private String certType;
}
