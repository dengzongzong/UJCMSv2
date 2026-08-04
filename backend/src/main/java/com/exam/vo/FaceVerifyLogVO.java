package com.exam.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FaceVerifyLogVO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentPhone;
    private Long examId;
    private String examName;
    private Integer verifyResult;
    private BigDecimal similarity;
    private Integer retryCount;
    private String errorMsg;
    private String deviceInfo;
    private String ipAddress;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
