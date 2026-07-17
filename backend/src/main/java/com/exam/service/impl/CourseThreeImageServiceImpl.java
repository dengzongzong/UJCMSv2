package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.PageResult;
import com.exam.entity.CourseThreeImage;
import com.exam.mapper.CourseThreeImageMapper;
import com.exam.service.CourseThreeImageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseThreeImageServiceImpl extends ServiceImpl<CourseThreeImageMapper, CourseThreeImage>
        implements CourseThreeImageService {

    @Override
    public PageResult<CourseThreeImage> page(Integer page, Integer size, Long courseId, Integer status) {
        LambdaQueryWrapper<CourseThreeImage> w = new LambdaQueryWrapper<CourseThreeImage>()
                .eq(courseId != null, CourseThreeImage::getCourseId, courseId)
                .eq(status != null, CourseThreeImage::getStatus, status)
                .orderByAsc(CourseThreeImage::getSort)
                .orderByDesc(CourseThreeImage::getCreateTime);
        return new PageResult<>(this.page(new Page<>(page, size), w));
    }

    @Override
    public List<CourseThreeImage> listForFrontend(Long courseId) {
        if (courseId == null) {
            // 拉全站通用图
            return this.list(new LambdaQueryWrapper<CourseThreeImage>()
                    .isNull(CourseThreeImage::getCourseId)
                    .eq(CourseThreeImage::getStatus, 1)
                    .orderByAsc(CourseThreeImage::getSort)
                    .last("LIMIT 3"));
        }
        // 先拿该课程下的图
        List<CourseThreeImage> list = this.list(new LambdaQueryWrapper<CourseThreeImage>()
                .eq(CourseThreeImage::getCourseId, courseId)
                .eq(CourseThreeImage::getStatus, 1)
                .orderByAsc(CourseThreeImage::getSort)
                .last("LIMIT 3"));
        if (list.size() < 3) {
            // 不足 3 个时,补全站通用图
            List<CourseThreeImage> common = this.list(new LambdaQueryWrapper<CourseThreeImage>()
                    .isNull(CourseThreeImage::getCourseId)
                    .eq(CourseThreeImage::getStatus, 1)
                    .orderByAsc(CourseThreeImage::getSort)
                    .last("LIMIT " + (3 - list.size())));
            list.addAll(common);
        }
        return list.stream().limit(3).collect(Collectors.toList());
    }

    @Override
    public void saveImage(CourseThreeImage entity) {
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getLinkType() == null) entity.setLinkType(0);
        if (entity.getId() == null) {
            this.save(entity);
        } else {
            this.updateById(entity);
        }
    }

    @Override
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        this.removeByIds(ids);
    }
}
