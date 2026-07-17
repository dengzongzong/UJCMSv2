package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.CourseThreeImage;

import java.util.List;

public interface CourseThreeImageService extends IService<CourseThreeImage> {
    /**
     * 后台分页
     */
    PageResult<CourseThreeImage> page(Integer page, Integer size, Long courseId, Integer status);

    /**
     * 公开查询:某课程的三图(优先返回 courseId 精确匹配,不足时补 courseId=NULL 的全站通用图)
     */
    List<CourseThreeImage> listForFrontend(Long courseId);

    /**
     * 后台保存
     */
    void saveImage(CourseThreeImage entity);

    /**
     * 后台删除
     */
    void delete(List<Long> ids);
}
