package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.entity.Course;
import com.exam.entity.Video;
import com.exam.entity.VideoCategory;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.VideoCategoryMapper;
import com.exam.mapper.VideoMapper;
import com.exam.service.VideoCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VideoCategoryServiceImpl extends ServiceImpl<VideoCategoryMapper, VideoCategory> implements VideoCategoryService {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Override
    public List<VideoCategory> listAll() {
        return this.list(new LambdaQueryWrapper<VideoCategory>()
                .orderByAsc(VideoCategory::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithCheck(Long id) {
        List<String> refs = new ArrayList<>();
        if (videoMapper.selectCount(new LambdaQueryWrapper<Video>().eq(Video::getCategoryId, id)) > 0)
            refs.add("视频");
        if (courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getCategoryId, id)) > 0)
            refs.add("课程");
        if (!refs.isEmpty()) {
            throw new BusinessException("该视频分类已被引用,不能删除，引用方: " + String.join("、", refs));
        }
        this.removeById(id);
    }
}
