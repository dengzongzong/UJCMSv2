package com.exam.vo;

import com.exam.entity.CertificateTemplateField;
import lombok.Data;

import java.util.List;

@Data
public class CertificateTemplateVO {
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
    private String createTime;
    private String updateTime;
    private List<CertificateTemplateField> fields;
}
