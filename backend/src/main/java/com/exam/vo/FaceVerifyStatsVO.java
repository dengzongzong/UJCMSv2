package com.exam.vo;

import lombok.Data;

@Data
public class FaceVerifyStatsVO {
    private Integer total;
    private Integer success;
    private Integer fail;
    private Integer passRate;
}
