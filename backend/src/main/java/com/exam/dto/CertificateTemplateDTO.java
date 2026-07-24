package com.exam.dto;

import com.exam.entity.CertificateTemplateField;
import lombok.Data;

import java.util.List;

@Data
public class CertificateTemplateDTO {
    private Long id;
    private String name;
    private String bgImageUrl;
    private Integer bgWidth;
    private Integer bgHeight;
    private Integer isDefault;
    private String remark;
    /** 钢印图片URL(透明背景PNG) */
    private String stampUrl;
    private Integer stampX;
    private Integer stampY;
    private Integer stampWidth;
    private Double stampRotation;
    private Float stampOpacity;
    /** 证书编号前缀字母(从此模板配置,生成证书编号时优先使用) */
    private String certNoPrefix;
    /** 证书编号中段字母(从此模板配置,生成证书编号时优先使用) */
    private String certNoMiddle;
    /** 模板上每个字段的位置 */
    private List<CertificateTemplateField> fields;
}
