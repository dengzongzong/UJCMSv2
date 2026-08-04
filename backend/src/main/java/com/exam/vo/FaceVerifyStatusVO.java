package com.exam.vo;

import lombok.Data;

@Data
public class FaceVerifyStatusVO {
    private boolean enabled;
    private boolean verified;
    private Double similarity;
    private String message;
}
