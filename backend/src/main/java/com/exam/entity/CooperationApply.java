package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合作申请管理
 */
@Data
@TableName("cooperation_apply")
public class CooperationApply {

    @TableId(type = IdType.AUTO)
    private Long id;

    // ========== 一、单位基本信息 ==========

    /** 单位名称 */
    private String unitName;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 法人姓名 */
    private String legalPerson;

    /** 法人联系电话 */
    private String legalPersonPhone;

    /** 法人身份证号 */
    private String legalPersonIdCard;

    /** 法人身份证正面图片 */
    private String legalIdFrontImg;

    /** 法人身份证反面图片 */
    private String legalIdBackImg;

    /** 营业执照图片 */
    private String businessLicenseImg;

    /** 注册资金 */
    private String registeredCapital;

    /** 实缴资金 */
    private String paidCapital;

    /** 成立日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate establishedDate;

    /** 单位地址 */
    private String unitAddress;

    /** 备案情况 */
    private String filingStatus;

    // ========== 二、主营业务信息 ==========

    /** 主营业务 */
    private String mainBusiness;

    /** 员工人数 */
    private Integer empCount;

    /** 培训经验年数 */
    private Integer trainingYears;

    /** 培训场地面积 */
    private String trainingArea;

    /** 培训设施设备 */
    private String trainingFacilities;

    /** 经验介绍 */
    private String expIntro;

    /** 招生资源介绍 */
    private String recruitResource;

    /** 其他主营业务 */
    private String otherBusiness;

    /** 授权管理编号 */
    private String authCode;

    // ========== 三、合作意向 ==========

    /** 合作意向(多选存储,逗号分隔) */
    private String cooperationIntent;

    // ========== 额外字段 ==========

    /** 联系人姓名 */
    private String contactName;

    /** 联系人电话 */
    private String contactPhone;

    /** 备注 */
    private String remark;

    /** 状态: 0-待审核 1-已通过 2-已拒绝 */
    private Integer status;

    /** 授权开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate authStartDate;

    /** 授权有效期截止日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate authExpireDate;

    // ========== 证书相关字段(每个合作单位各自维护) ==========

    /** 证书背景图片URL */
    private String certImageUrl;

    /** 覆盖在证书图片上的富文本HTML */
    private String certRichText;

    /** 证书背景图缩放比例(30-100),用户端按此比例等比渲染 */
    private Integer certBgScale;

    /** 编辑证书时编辑区文本宽度(像素),用户端按此宽度等比缩放渲染 */
    private Integer certEditorWidth;

    // ========== 审计字段 ==========

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
