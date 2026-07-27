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
import com.exam.entity.Student;
import com.exam.entity.StudentVideo;
import com.exam.entity.StudentProfession;
import com.exam.entity.Video;
import com.exam.entity.VideoCategory;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.CourseSectionVideoMapper;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.StudentVideoMapper;
import com.exam.mapper.StudentProfessionMapper;
import com.exam.mapper.VideoCategoryMapper;
import com.exam.mapper.VideoMapper;
import com.exam.service.VideoManageService;
import com.exam.vo.AdminVideoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private StudentProfessionMapper studentProfessionMapper;

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

    @Override
    public PageResult<Student> studentsPage(Long videoId, Integer page, Integer size, String phone, String idCard, Integer exactCount, Integer unopened, String profession) {
        // 查询已开通该视频的学生ID集合
        List<StudentVideo> studentVideos = studentVideoMapper.selectList(
                new LambdaQueryWrapper<StudentVideo>().eq(StudentVideo::getVideoId, videoId));
        Set<Long> openedIds = studentVideos.stream().map(StudentVideo::getStudentId).collect(Collectors.toSet());

        // 显示条数逻辑：当传入 exactCount 时，固定返回最新 N 条
        if (exactCount != null && exactCount > 0) {
            page = 1;
            size = exactCount;
        }

        // 按专业筛选: 先查 profession 表按名称匹配,再查 student_profession 关联表获取 studentId
        Set<Long> professionFilteredIds = null;
        if (StringUtils.hasText(profession)) {
            List<Profession> matchedProfessions = professionMapper.selectList(
                    new LambdaQueryWrapper<Profession>().like(Profession::getName, profession));
            if (matchedProfessions.isEmpty()) {
                return new PageResult<>(new Page<>(page, size));
            }
            Set<Long> profIds = matchedProfessions.stream().map(Profession::getId).collect(Collectors.toSet());
            List<StudentProfession> sps = studentProfessionMapper.selectList(
                    new LambdaQueryWrapper<StudentProfession>().in(StudentProfession::getProfessionId, profIds));
            professionFilteredIds = sps.stream().map(StudentProfession::getStudentId).collect(Collectors.toSet());
            if (professionFilteredIds.isEmpty()) {
                return new PageResult<>(new Page<>(page, size));
            }
        }

        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(phone), Student::getPhone, phone);
        wrapper.like(StringUtils.hasText(idCard), Student::getIdCard, idCard);
        if (professionFilteredIds != null) {
            wrapper.in(Student::getId, professionFilteredIds);
        }
        if (unopened != null && unopened == 1) {
            // 未开通：id NOT IN openedIds
            if (!openedIds.isEmpty()) {
                wrapper.notIn(Student::getId, openedIds);
            }
        } else {
            // 已开通：id IN openedIds
            if (openedIds.isEmpty()) {
                return new PageResult<>(new Page<>(page, size)); // 空分页
            }
            wrapper.in(Student::getId, openedIds);
        }
        wrapper.orderByDesc(Student::getCreateTime).orderByDesc(Student::getId);
        Page<Student> p = new Page<>(page, size);
        Page<Student> result = studentMapper.selectPage(p, wrapper);
        result.getRecords().forEach(s -> s.setPassword(null));
        // 填充专业名称
        fillProfessionNames(result.getRecords());
        return new PageResult<>(result);
    }

    /** 批量填充学生的专业名称(通过 student_profession 关联表) */
    private void fillProfessionNames(List<Student> students) {
        if (students == null || students.isEmpty()) return;
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        List<StudentProfession> sps = studentProfessionMapper.selectList(
                new LambdaQueryWrapper<StudentProfession>().in(StudentProfession::getStudentId, studentIds));
        if (sps.isEmpty()) return;
        Set<Long> profIds = sps.stream().map(StudentProfession::getProfessionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> profNameMap = new HashMap<>();
        if (!profIds.isEmpty()) {
            List<Profession> professions = professionMapper.selectBatchIds(profIds);
            for (Profession p : professions) {
                profNameMap.put(p.getId(), p.getName());
            }
        }
        Map<Long, List<String>> studentProfNames = new HashMap<>();
        for (StudentProfession sp : sps) {
            String pname = profNameMap.get(sp.getProfessionId());
            if (pname != null) {
                studentProfNames.computeIfAbsent(sp.getStudentId(), k -> new ArrayList<>()).add(pname);
            }
        }
        for (Student s : students) {
            List<String> names = studentProfNames.get(s.getId());
            if (names != null && !names.isEmpty()) {
                s.setProfessionName(String.join(",", names));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openStudents(Long videoId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        // 查询已存在的开通记录
        List<StudentVideo> existing = studentVideoMapper.selectList(
                new LambdaQueryWrapper<StudentVideo>()
                        .eq(StudentVideo::getVideoId, videoId)
                        .in(StudentVideo::getStudentId, studentIds));
        Set<Long> existingIds = existing.stream().map(StudentVideo::getStudentId).collect(Collectors.toSet());
        for (Long studentId : studentIds) {
            if (existingIds.contains(studentId)) {
                continue;
            }
            StudentVideo sv = new StudentVideo();
            sv.setStudentId(studentId);
            sv.setVideoId(videoId);
            studentVideoMapper.insert(sv);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeStudent(Long videoId, Long studentId) {
        studentVideoMapper.delete(new LambdaQueryWrapper<StudentVideo>()
                .eq(StudentVideo::getVideoId, videoId)
                .eq(StudentVideo::getStudentId, studentId));
    }
}
