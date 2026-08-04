package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程支付订单
 */
@Data
@TableName("course_order")
public class CourseOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务订单号 */
    private String orderNo;
    private Long studentId;
    private Long courseId;
    private String courseName;
    /** 金额(分) */
    private Integer amount;
    /** 支付渠道: wechat/alipay */
    private String channel;
    /** 0-待支付 1-已支付 2-已关闭 */
    private Integer status;
    /** 第三方交易号 */
    private String transactionId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 学生姓名(非数据库字段) */
    @TableField(exist = false)
    private String studentName;
}
