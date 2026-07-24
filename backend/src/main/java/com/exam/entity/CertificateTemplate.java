package com.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 证书模板
 */
@Data
@TableName("certificate_template")
public class CertificateTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 模板名称 */
    private String name;
    /** 背景图 URL */
    private String bgImageUrl;
    /** 背景图宽度(像素) */
    private Integer bgWidth;
    /** 背景图高度(像素) */
    private Integer bgHeight;
    /** 是否默认 */
    private Integer isDefault;
    /** 钢印图片URL(透明背景PNG,叠加在证书上方) */
    private String stampUrl;
    /** 钢印X坐标(像素) */
    private Integer stampX;
    /** 钢印Y坐标(像素) */
    private Integer stampY;
    /** 钢印宽度(像素,0或null=按原尺寸) */
    private Integer stampWidth;
    /** 钢印旋转角度(0-360,null=0) */
    private Double stampRotation;
    /** 钢印透明度(0.0-1.0,null=0.8) */
    private Float stampOpacity;
    /** 备注 */
    private String remark;
    /** 证书编号前缀字母(从此模板配置,生成证书编号时优先使用) */
    private String certNoPrefix;
    /** 证书编号中段字母(从此模板配置,生成证书编号时优先使用) */
    private String certNoMiddle;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
