package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.VideoDTO;
import com.exam.entity.Course;
import com.exam.entity.CourseSectionVideo;
import com.exam.entity.Profession;
import com.exam.entity.StudentVideo;
import com.exam.entity.Video;
import com.exam.entity.VideoCategory;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.CourseSectionVideoMapper;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.StudentVideoMapper;
import com.exam.mapper.VideoCategoryMapper;
import com.exam.mapper.VideoMapper;
import com.exam.service.VideoManageService;
import com.exam.vo.AdminVideoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VideoManageServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoManageService {

    @Autowired
    private CourseSectionVideoMapper courseSectionVideoMapper;

    @Autowired
    private VideoCategoryMapper videoCategoryMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ProfessionMapper professionMapper;

    @Autowired
    private StudentVideoMapper studentVideoMapper;

    @Override
    public PageResult<AdminVideoVO> page(Integer page, Integer size, String name, Long categoryId, Long professionId) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .like(StringUtils.hasText(name), Video::getName, name)
                .eq(categoryId != null, Video::getCategoryId, categoryId)
                .eq(professionId != null, Video::getProfessionId, professionId)
                .orderByDesc(Video::getCreateTime);
        Page<Video> p = new Page<>(page, size);
        Page<Video> result = this.page(p, wrapper);

        // 实体 -> AdminVideoVO,并按页 id 批量填充 categoryName / courseName / professionName
        Page<AdminVideoVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<Video> records = result.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(voPage);
        }
        List<AdminVideoVO> voList = fillJoinNames(records);
        voPage.setRecords(voList);
        return new PageResult<>(voPage);
    }

    /**
     * 实体 -> AdminVideoVO,并按 id 批量填充分类/课程/专业名称,避免全表 load
     */
    private List<AdminVideoVO> fillJoinNames(List<Video> videos) {
        Set<Long> categoryIds = videos.stream().map(Video::getCategoryId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> courseIds = videos.stream().map(Video::getCourseId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> professionIds = videos.stream().map(Video::getProfessionId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> categoryMap = categoryIds.isEmpty() ? java.util.Collections.emptyMap()
                : videoCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(VideoCategory::getId, VideoCategory::getName, (a, b) -> a));
        Map<Long, String> courseMap = courseIds.isEmpty() ? java.util.Collections.emptyMap()
                : courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName, (a, b) -> a));
        Map<Long, String> professionMap = professionIds.isEmpty() ? java.util.Collections.emptyMap()
                : professionMapper.selectBatchIds(professionIds).stream()
                .collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));

        return videos.stream().map(video -> {
            AdminVideoVO vo = new AdminVideoVO();
            vo.setId(video.getId());
            vo.setName(video.getName());
            vo.setCategoryId(video.getCategoryId());
            vo.setCategoryName(video.getCategoryId() == null ? null : categoryMap.get(video.getCategoryId()));
            vo.setCourseId(video.getCourseId());
            vo.setCourseName(video.getCourseId() == null ? null : courseMap.get(video.getCourseId()));
            vo.setProfessionId(video.getProfessionId());
            vo.setProfessionName(video.getProfessionId() == null ? null : professionMap.get(video.getProfessionId()));
            vo.setUrl(video.getUrl());
            vo.setCoverUrl(video.getCoverUrl());
            vo.setDuration(video.getDuration());
            vo.setSize(video.getSize());
            vo.setPlayCount(video.getPlayCount());
            vo.setBaseStudyCount(video.getBaseStudyCount());
            // 展示用学习人数 = 基础学习人数 + 播放量
            int base = video.getBaseStudyCount() == null ? 0 : video.getBaseStudyCount();
            int play = video.getPlayCount() == null ? 0 : video.getPlayCount();
            vo.setStudyCount(base + play);
            vo.setRemark(video.getRemark());
            vo.setCreateTime(video.getCreateTime());
            vo.setUpdateTime(video.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public AdminVideoVO detail(Long id) {
        Video video = this.getById(id);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        return fillJoinNames(java.util.Collections.singletonList(video)).get(0);
    }

    @Override
    public void add(VideoDTO dto) {
        Video video = new Video();
        video.setName(dto.getName());
        video.setCategoryId(dto.getCategoryId());
        video.setCourseId(dto.getCourseId());
        video.setProfessionId(dto.getProfessionId());
        video.setUrl(dto.getUrl());
        video.setCoverUrl(dto.getCoverUrl());
        video.setDuration(dto.getDuration() == null ? 0 : dto.getDuration());
        video.setSize(dto.getSize() == null ? 0L : dto.getSize());
        video.setPlayCount(0);
        video.setBaseStudyCount(dto.getBaseStudyCount() == null ? 0 : dto.getBaseStudyCount());
        video.setRemark(dto.getRemark());
        this.save(video);
    }

    @Override
    public void update(VideoDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        Video video = this.getById(dto.getId());
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        video.setName(dto.getName());
        video.setCategoryId(dto.getCategoryId());
        video.setCourseId(dto.getCourseId());
        video.setProfessionId(dto.getProfessionId());
        video.setUrl(dto.getUrl());
        video.setCoverUrl(dto.getCoverUrl());
        video.setDuration(dto.getDuration());
        video.setSize(dto.getSize());
        video.setBaseStudyCount(dto.getBaseStudyCount() == null ? 0 : dto.getBaseStudyCount());
        video.setRemark(dto.getRemark());
        this.updateById(video);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查是否被课程小节视频引用
        long count = courseSectionVideoMapper.selectCount(
                new LambdaQueryWrapper<CourseSectionVideo>().eq(CourseSectionVideo::getVideoId, id));
        if (count > 0) {
            throw new BusinessException("该视频已被引用，不可删除");
        }
        // 清理学生开通记录
        studentVideoMapper.delete(new LambdaQueryWrapper<StudentVideo>()
                .eq(StudentVideo::getVideoId, id));
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            this.delete(id);
        }
    }

    @Override
    public List<AdminVideoVO> sortByPlayCount() {
        List<Video> videos = this.list(new LambdaQueryWrapper<Video>()
                .orderByDesc(Video::getPlayCount));
        return fillJoinNames(videos);
    }
}
