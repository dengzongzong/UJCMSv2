package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("course_section_video")
public class CourseSectionVideo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sectionId;
    private Long videoId;
    private Integer sort;
    /**
     * 查看权限 0-所有已开通用户可看 1-需指定权限
     */
    private Integer viewPermission;
}
