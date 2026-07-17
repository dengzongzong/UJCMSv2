package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合作咨询配置(单条记录)
 */
@Data
@TableName("cooperation_setting")
public class CooperationSetting {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 联系电话1 */
    private String phone1;
    /** 联系电话2 */
    private String phone2;
    /** 联系邮箱1 */
    private String email1;
    /** 联系邮箱2 */
    private String email2;
    /** 合作流程说明 */
    private String processDesc;
    /** 左栏-单位背景介绍 */
    private String intro;
    /** 意向表附件名 */
    private String attachmentName;
    /** 意向表附件URL */
    private String attachmentUrl;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
