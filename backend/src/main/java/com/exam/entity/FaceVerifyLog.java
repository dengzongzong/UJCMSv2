package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("face_verify_log")
public class FaceVerifyLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long examId;
    private Long recordId;
    private Integer verifyResult;
    private BigDecimal similarity;
    private Integer retryCount;
    private String idPhotoUrl;
    private String errorMsg;
    private String deviceInfo;
    private String ipAddress;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
