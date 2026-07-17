package com.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合作咨询/投诉建议 留言表
 */
@Data
@TableName("feedback_message")
public class FeedbackMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 类型: cooperation / complaint / declaration */
    private String type;
    /** 单位名称 */
    private String orgName;
    /** 联系人 */
    private String contactName;
    /** 联系电话 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 留言内容 */
    private String content;
    /** 提交IP */
    private String ip;
    /** 0-未处理 1-已处理 */
    private Integer status;
    /** 备注 */
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
