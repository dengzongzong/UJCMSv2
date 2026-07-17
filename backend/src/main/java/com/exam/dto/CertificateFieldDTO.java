package com.exam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateFieldDTO {
    private Long id;
    private String fieldKey;
    private String fieldName;
    /** 1-文本 2-数字 3-日期 4-选择项 5-图片 */
    private Integer fieldType;
    private Integer required;
    private Integer sort;
    private String defaultValue;
    private String options;
    /** 1-系统内置 0-自定义 */
    private Integer isSystem;
}
