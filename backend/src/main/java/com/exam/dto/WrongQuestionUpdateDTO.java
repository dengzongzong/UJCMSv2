package com.exam.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 更新错题状态请求(标记已掌握 / 取消掌握)
 */
public class WrongQuestionUpdateDTO {
    /** 错题 ID(由 query string 传入) */
    private Long id;

    /**
     * 状态: 0=未掌握, 1=已掌握
     */
    @NotNull(message = "请指定状态")
    @Min(0) @Max(1)
    private Integer mastered;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getMastered() { return mastered; }
    public void setMastered(Integer mastered) { this.mastered = mastered; }
}
