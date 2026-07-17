package com.exam.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 合作咨询/投诉建议 留言提交 DTO
 */
@Data
public class FeedbackMessageDTO {

    /** 类型: cooperation / complaint / declaration */
    @NotBlank(message = "类型不能为空")
    private String type;

    /** 单位名称(合作咨询必填) */
    private String orgName;

    /** 联系人 */
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    private String phone;

    /** 邮箱 */
    private String email;

    /** 留言内容/合作意向 */
    @NotBlank(message = "留言内容不能为空")
    @Size(max = 2000, message = "留言内容不能超过 2000 字")
    private String content;
}
