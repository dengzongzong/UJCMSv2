package com.exam.vo;

import lombok.Data;

@Data
public class FaceVerifyConfigVO {
    private boolean enabled;
    private double threshold;
    private int maxRetries;
}
