package com.exam.vo;

import lombok.Data;

import java.util.List;

/**
 * 课程小节VO
 */
@Data
public class SectionVO {
    private Long id;
    private String name;
    private String remark;
    private Integer videoCount;
    private List<VideoVO> videos;
}
