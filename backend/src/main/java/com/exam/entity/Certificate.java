package com.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 证书主表
 */
@Data
@TableName("certificate")
public class Certificate {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 证书编号(系统生成,唯一) */
    private String certNo;
    /** 学员编号(系统生成,唯一) */
    private String studentNo;
    /** 姓名 */
    private String name;
    /** 身份证号 */
    private String idCard;
    /** 1-男 2-女 */
    private Integer gender;
    /** 职业名称 */
    private String profession;
    /** 技能等级 */
    private String skillLevel;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;
    /** 证书编号前缀字母 */
    private String certNoPrefix;
    /** 证书编号中段字母 */
    private String certNoMiddle;
    /** 学员编号前缀字母 */
    private String studentNoPrefix;
    /** 学员编号中段字母 */
    private String studentNoMiddle;
    /** 报单机构 */
    private String agency;
    /** 报单机构费用 */
    private BigDecimal agencyFee;
    /** 照片 URL(非数据库字段) */
    @TableField(exist = false)
    private String photoUrl;
    /** 证书二维码1/2/3 URL */
    private String qrUrl1;
    private String qrUrl2;
    private String qrUrl3;
    /** 学员考试二维码 URL */
    private String examQrUrl;
    /** 0-关 1-开 */
    private Integer examQrEnabled;
    /** 理论成绩 */
    private String theoryScore;
    /** 实操成绩 */
    private String practicalScore;
    /** 综合测评 */
    private String comprehensiveEvaluation;
    /** 自定义字段值,JSON格式 */
    private String extraJson;
    /** 备注 */
    private String remark;
    /** 已绑定的证书模板 ID(走『模板绑定』时写入;certificates 列表用此字段查模板名) */
    private Long templateId;
    /** 证书导入/上传时间(由导入或单条上传动作写入;与 createTime 区分场景) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
