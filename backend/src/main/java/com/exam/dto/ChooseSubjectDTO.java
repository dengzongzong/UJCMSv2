package com.exam.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 选择专业请求
 * <p>当前业务模型:专业下不再有科目,只选专业即可</p>
 */
@Data
public class ChooseSubjectDTO {

    /**
     * 专业ID
     */
    @NotNull(message = "专业ID不能为空")
    private Long professionId;
}
