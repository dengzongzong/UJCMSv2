package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.CourseDTO;
import com.exam.dto.SectionDTO;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.CourseManageService;
import com.exam.vo.AdminCourseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseManageServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseManageService {

    @Autowired
    private CourseSectionMapper courseSectionMapper;
    @Autowired
    private CourseSectionVideoMapper courseSectionVideoMapper;
    @Autowired
    private StudentCourseMapper studentCourseMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private ProfessionMapper professionMapper;
    @Autowired
    private SubjectMapper subjectMapper;
    @Autowired
    private VideoCategoryMapper videoCategoryMapper;
    @Autowired
    private VideoStudyRecordMapper videoStudyRecordMapper;
    @Autowired
    private StudentProfessionMapper studentProfessionMapper;

    @Override
    public PageResult<AdminCourseVO> page(Integer page, Integer size, String name,
                                          String createTimeStart, String createTimeEnd,
                                          Integer status, Integer sectionCount, Long professionId, Long categoryId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .like(StringUtils.hasText(name), Course::getName, name)
                .eq(status != null, Course::getStatus, status)
                .eq(sectionCount != null, Course::getSectionCount, sectionCount)
                .eq(professionId != null, Course::getProfessionId, professionId)
                .eq(categoryId != null, Course::getCategoryId, categoryId)
                .orderByDesc(Course::getIsTop)
                .orderByAsc(Course::getTopSort)
                .orderByAsc(Course::getSort)
                .orderByDesc(Course::getCreateTime);
        if (StringUtils.hasText(createTimeStart)) {
            wrapper.ge(Course::getCreateTime, LocalDate.parse(createTimeStart).atStartOfDay());
        }
        if (StringUtils.hasText(createTimeEnd)) {
            wrapper.le(Course::getCreateTime, LocalDate.parse(createTimeEnd).atTime(23, 59, 59));
        }
        Page<Course> p = new Page<>(page, size);
        Page<Course> result = this.page(p, wrapper);

        // 转 VO + 按页 id 批量填充 professionName / subjectName
        Page<AdminCourseVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<Course> records = result.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(voPage);
        }
        Set<Long> professionIds = records.stream().map(Course::getProfessionId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> subjectIds = records.stream().map(Course::getSubjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> categoryIds = records.stream().map(Course::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> professionMap = professionIds.isEmpty() ? Collections.emptyMap()
                : professionMapper.selectBatchIds(professionIds).stream()
                .collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));
        Map<Long, String> subjectMap = subjectIds.isEmpty() ? Collections.emptyMap()
                : subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName, (a, b) -> a));
        Map<Long, String> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : videoCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(VideoCategory::getId, VideoCategory::getName, (a, b) -> a));

        List<AdminCourseVO> voList = new ArrayList<>(records.size());
        for (Course c : records) {
            AdminCourseVO vo = new AdminCourseVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setCoverUrl(c.getCoverUrl());
            vo.setIntro(c.getIntro());
            vo.setPrice(c.getPrice());
            vo.setTag(c.getTag());
            vo.setTotalDuration(c.getTotalDuration());
            vo.setSectionCount(c.getSectionCount());
            vo.setStatus(c.getStatus());
            vo.setProfessionId(c.getProfessionId());
            vo.setProfessionName(c.getProfessionId() == null ? null : professionMap.get(c.getProfessionId()));
            vo.setSubjectId(c.getSubjectId());
            vo.setSubjectName(c.getSubjectId() == null ? null : subjectMap.get(c.getSubjectId()));
            vo.setCategoryId(c.getCategoryId());
            vo.setCategoryName(c.getCategoryId() == null ? null : categoryMap.get(c.getCategoryId()));
            vo.setBaseStudyCount(c.getBaseStudyCount() == null ? 0 : c.getBaseStudyCount());
            vo.setBaseStudyHours(c.getBaseStudyHours() == null ? 0 : c.getBaseStudyHours());
            vo.setSort(c.getSort() == null ? 0 : c.getSort());
            vo.setIsTop(c.getIsTop() == null ? 0 : c.getIsTop());
            vo.setTopSort(c.getTopSort() == null ? 0 : c.getTopSort());
            vo.setCreateTime(c.getCreateTime());
            vo.setUpdateTime(c.getUpdateTime());
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return new PageResult<>(voPage);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        Course course = this.getById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        // 查询小节
        List<CourseSection> sections = courseSectionMapper.selectList(
                new LambdaQueryWrapper<CourseSection>()
                        .eq(CourseSection::getCourseId, id)
                        .orderByAsc(CourseSection::getSort));
        // 查询所有小节的视频关联
        List<Long> sectionIds = sections.stream().map(CourseSection::getId).collect(Collectors.toList());
        List<CourseSectionVideo> sectionVideos = sectionIds.isEmpty() ? new ArrayList<>() :
                courseSectionVideoMapper.selectList(new LambdaQueryWrapper<CourseSectionVideo>()
                        .in(CourseSectionVideo::getSectionId, sectionIds)
                        .orderByAsc(CourseSectionVideo::getSort));
        // 查询视频信息
        List<Long> videoIds = sectionVideos.stream().map(CourseSectionVideo::getVideoId).distinct().collect(Collectors.toList());
        Map<Long, Video> videoMap = videoIds.isEmpty() ? new HashMap<>() :
                videoMapper.selectBatchIds(videoIds).stream().collect(Collectors.toMap(Video::getId, v -> v));
        // 按小节分组视频
        Map<Long, List<CourseSectionVideo>> svMap = sectionVideos.stream()
                .collect(Collectors.groupingBy(CourseSectionVideo::getSectionId));

        List<Map<String, Object>> sectionList = new ArrayList<>();
        for (CourseSection section : sections) {
            Map<String, Object> sectionItem = new HashMap<>();
            sectionItem.put("id", section.getId());
            sectionItem.put("courseId", section.getCourseId());
            sectionItem.put("name", section.getName());
            sectionItem.put("remark", section.getRemark());
            sectionItem.put("sort", section.getSort());
            List<Map<String, Object>> videos = new ArrayList<>();
            List<CourseSectionVideo> svs = svMap.getOrDefault(section.getId(), new ArrayList<>());
            for (CourseSectionVideo sv : svs) {
                Map<String, Object> videoItem = new HashMap<>();
                videoItem.put("id", sv.getId());
                videoItem.put("sectionId", sv.getSectionId());
                videoItem.put("videoId", sv.getVideoId());
                videoItem.put("sort", sv.getSort());
                videoItem.put("viewPermission", sv.getViewPermission());
                videoItem.put("video", videoMap.get(sv.getVideoId()));
                videos.add(videoItem);
            }
            sectionItem.put("videos", videos);
            sectionList.add(sectionItem);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("course", course);
        result.put("sections", sectionList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CourseDTO dto) {
        // 校验课程名称唯一
        if (StringUtils.hasText(dto.getName())) {
            long count = this.count(new LambdaQueryWrapper<Course>().eq(Course::getName, dto.getName().trim()));
            if (count > 0) {
                throw new BusinessException("课程名称已存在，请勿重复");
            }
        }
        Course course = new Course();
        course.setName(dto.getName());
        course.setCoverUrl(dto.getCoverUrl());
        course.setIntro(dto.getIntro());
        course.setPrice(dto.getPrice());
        course.setTag(dto.getTag());
        course.setProfessionId(dto.getProfessionId());
        course.setSubjectId(dto.getSubjectId());
        course.setCategoryId(dto.getCategoryId());
        course.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        course.setBaseStudyCount(dto.getBaseStudyCount() == null ? 0 : dto.getBaseStudyCount());
        course.setBaseStudyHours(dto.getBaseStudyHours() == null ? 0 : dto.getBaseStudyHours());
        course.setSort(dto.getSort() == null ? 0 : dto.getSort());
        course.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        course.setTopSort(dto.getTopSort() == null ? 0 : dto.getTopSort());
        course.setSectionCount(0);
        course.setTotalDuration(0);
        this.save(course);

        // 创建小节和视频关联
        int totalDuration = 0;
        int sectionCount = 0;
        if (dto.getSections() != null) {
            for (SectionDTO sectionDTO : dto.getSections()) {
                CourseSection section = new CourseSection();
                section.setCourseId(course.getId());
                section.setName(sectionDTO.getName());
                section.setRemark(sectionDTO.getRemark());
                section.setSort(sectionDTO.getSort() == null ? 0 : sectionDTO.getSort());
                courseSectionMapper.insert(section);
                sectionCount++;

                if (sectionDTO.getVideoIds() != null) {
                    int sort = 1;
                    for (Long videoId : sectionDTO.getVideoIds()) {
                        CourseSectionVideo sv = new CourseSectionVideo();
                        sv.setSectionId(section.getId());
                        sv.setVideoId(videoId);
                        sv.setSort(sort++);
                        sv.setViewPermission(sectionDTO.getViewPermission() == null ? 0 : sectionDTO.getViewPermission());
                        courseSectionVideoMapper.insert(sv);
                        // 累加视频时长
                        Video video = videoMapper.selectById(videoId);
                        if (video != null && video.getDuration() != null) {
                            totalDuration += video.getDuration();
                        }
                    }
                }
            }
        }
        // 更新小节数量和总时长
        course.setSectionCount(sectionCount);
        course.setTotalDuration(totalDuration);
        this.updateById(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CourseDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        Course course = this.getById(dto.getId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        // 校验课程名称唯一(排除自身)
        if (StringUtils.hasText(dto.getName())) {
            long count = this.count(new LambdaQueryWrapper<Course>()
                    .eq(Course::getName, dto.getName().trim())
                    .ne(Course::getId, dto.getId()));
            if (count > 0) {
                throw new BusinessException("课程名称已存在，请勿重复");
            }
        }
        course.setName(dto.getName());
        course.setCoverUrl(dto.getCoverUrl());
        course.setIntro(dto.getIntro());
        course.setPrice(dto.getPrice());
        course.setTag(dto.getTag());
        course.setProfessionId(dto.getProfessionId());
        course.setSubjectId(dto.getSubjectId());
        course.setCategoryId(dto.getCategoryId());
        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }
        course.setBaseStudyCount(dto.getBaseStudyCount() == null ? 0 : dto.getBaseStudyCount());
        course.setBaseStudyHours(dto.getBaseStudyHours() == null ? 0 : dto.getBaseStudyHours());
        course.setSort(dto.getSort() == null ? 0 : dto.getSort());
        course.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        course.setTopSort(dto.getTopSort() == null ? 0 : dto.getTopSort());

        // 先删除旧的小节和视频关联
        List<CourseSection> oldSections = courseSectionMapper.selectList(
                new LambdaQueryWrapper<CourseSection>().eq(CourseSection::getCourseId, dto.getId()));
        List<Long> oldSectionIds = oldSections.stream().map(CourseSection::getId).collect(Collectors.toList());
        if (!oldSectionIds.isEmpty()) {
            courseSectionVideoMapper.delete(new LambdaQueryWrapper<CourseSectionVideo>()
                    .in(CourseSectionVideo::getSectionId, oldSectionIds));
            courseSectionMapper.deleteBatchIds(oldSectionIds);
        }

        // 重新创建小节和视频关联
        int totalDuration = 0;
        int sectionCount = 0;
        if (dto.getSections() != null) {
            for (SectionDTO sectionDTO : dto.getSections()) {
                CourseSection section = new CourseSection();
                section.setCourseId(course.getId());
                section.setName(sectionDTO.getName());
                section.setRemark(sectionDTO.getRemark());
                section.setSort(sectionDTO.getSort() == null ? 0 : sectionDTO.getSort());
                courseSectionMapper.insert(section);
                sectionCount++;

                if (sectionDTO.getVideoIds() != null) {
                    int sort = 1;
                    for (Long videoId : sectionDTO.getVideoIds()) {
                        CourseSectionVideo sv = new CourseSectionVideo();
                        sv.setSectionId(section.getId());
                        sv.setVideoId(videoId);
                        sv.setSort(sort++);
                        sv.setViewPermission(sectionDTO.getViewPermission() == null ? 0 : sectionDTO.getViewPermission());
                        courseSectionVideoMapper.insert(sv);
                        Video video = videoMapper.selectById(videoId);
                        if (video != null && video.getDuration() != null) {
                            totalDuration += video.getDuration();
                        }
                    }
                }
            }
        }
        course.setSectionCount(sectionCount);
        course.setTotalDuration(totalDuration);
        this.updateById(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Course course = this.getById(id);
        String courseName = course != null ? course.getName() : null;
        // 删除前回填课程名称到学习记录(防止删除后名称丢失)
        if (courseName != null) {
            studentCourseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<StudentCourse>()
                    .eq(StudentCourse::getCourseId, id)
                    .set(StudentCourse::getCourseName, courseName));
            videoStudyRecordMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<VideoStudyRecord>()
                    .eq(VideoStudyRecord::getCourseId, id)
                    .set(VideoStudyRecord::getCourseName, courseName));
        }
        // 删除关联的小节和视频关联
        List<CourseSection> sections = courseSectionMapper.selectList(
                new LambdaQueryWrapper<CourseSection>().eq(CourseSection::getCourseId, id));
        List<Long> sectionIds = sections.stream().map(CourseSection::getId).collect(Collectors.toList());
        if (!sectionIds.isEmpty()) {
            courseSectionVideoMapper.delete(new LambdaQueryWrapper<CourseSectionVideo>()
                    .in(CourseSectionVideo::getSectionId, sectionIds));
            courseSectionMapper.deleteBatchIds(sectionIds);
        }
        // 保留学生课程学习记录(开通记录和视频观看记录均不删除)
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
    public List<Student> students(Long id) {
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getCourseId, id));
        List<Long> studentIds = studentCourses.stream().map(StudentCourse::getStudentId).collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        students.forEach(s -> s.setPassword(null));
        return students;
    }

    @Override
    public PageResult<Student> studentsPage(Long courseId, Integer page, Integer size, String phone, Integer unopened, String idCard, Integer exactCount, String profession) {
        // 显示最新N条：固定第1页，size = exactCount
        if (exactCount != null && exactCount > 0) {
            page = 1;
            size = exactCount;
        }
        // 查询已开通的学生ID集合
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getCourseId, courseId));
        Set<Long> openedIds = studentCourses.stream().map(StudentCourse::getStudentId).collect(Collectors.toSet());

        // 按专业筛选: 先查 student_profession 关联表获取匹配的 studentId
        Set<Long> professionFilteredIds = null;
        if (StringUtils.hasText(profession)) {
            // profession 可能是专业名称或ID,先查 profession 表
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openStudents(Long courseId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        // 查询已存在的
        List<StudentCourse> existing = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getCourseId, courseId)
                        .in(StudentCourse::getStudentId, studentIds));
        Set<Long> existingIds = existing.stream().map(StudentCourse::getStudentId).collect(Collectors.toSet());
        for (Long studentId : studentIds) {
            if (existingIds.contains(studentId)) {
                continue;
            }
            StudentCourse sc = new StudentCourse();
            sc.setStudentId(studentId);
            sc.setCourseId(courseId);
            studentCourseMapper.insert(sc);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeStudent(Long courseId, Long studentId) {
        studentCourseMapper.delete(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getCourseId, courseId)
                .eq(StudentCourse::getStudentId, studentId));
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
}
