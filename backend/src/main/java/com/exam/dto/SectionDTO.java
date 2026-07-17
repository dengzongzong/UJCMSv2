package com.exam.dto;

import lombok.Data;

import java.util.List;

/**
 * 课程小节DTO
 */
@Data
public class SectionDTO {
    private Long id;
    private String name;
    private String remark;
    private Integer sort;
    /** 小节关联的视频ID列表 */
    private List<Long> videoIds;
    /**
     * 查看权限 0-所有已开通用户可看 1-需指定权限
     * 设置后应用于该小节下所有视频关联
     */
    private Integer viewPermission;
}
