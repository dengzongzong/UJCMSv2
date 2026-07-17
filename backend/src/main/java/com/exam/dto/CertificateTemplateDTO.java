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
    /** 模板上每个字段的位置 */
    private List<CertificateTemplateField> fields;
}
