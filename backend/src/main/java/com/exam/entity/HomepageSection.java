package com.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 首页内容板块(政策法规/信息公开)
 * type: 1-政策法规 2-信息公开
 */
@Data
@TableName("homepage_section")
public class HomepageSection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;       // 富文本内容
    private Integer type;         // 1-政策法规 2-信息公开
    private Integer status;       // 0-隐藏 1-显示
    private Integer sort;         // 排序(升序)
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
