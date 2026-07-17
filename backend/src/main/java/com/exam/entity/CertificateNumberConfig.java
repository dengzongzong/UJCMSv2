package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("certificate_number_config")
public class CertificateNumberConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String certNoPrefix;
    private String certNoMiddle;
    private String studentNoPrefix;
    private String studentNoMiddle;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
