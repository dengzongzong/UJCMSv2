package com.exam.vo;

import com.exam.entity.CertificateField;
import lombok.Data;

import java.util.List;

/**
 * 证书详情 VO
 */
@Data
public class CertificateVO {
    private Long id;
    private String certNo;
    private String studentNo;
    private String name;
    private String idCard;
    private Integer gender;
    private String genderName;
    private String profession;
    private String skillLevel;
    private String issueDate;
    private String certNoPrefix;
    private String certNoMiddle;
    private String studentNoPrefix;
    private String studentNoMiddle;
    private String agency;
    private String agencyFee;
    private String qrUrl1;
    private String qrUrl2;
    private String qrUrl3;
    private String examQrUrl;
    private Integer examQrEnabled;
    /** 自定义字段的展示值(扁平化为 {fieldName: value}) */
    private java.util.Map<String, Object> extra;
    private String remark;
    /** 已绑定的证书模板ID(编辑页回显用) */
    private Long templateId;
    /** 模板名(只读,来自 templateId JOIN) */
    private String templateName;
    /** 证书类型 */
    private String certType;
    private String createTime;
    private String updateTime;
    /** 当前证书可用的全部字段定义(用于前端动态渲染) */
    private List<CertificateField> fields;
}
