package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.dto.VideoProgressDTO;
import com.exam.entity.Course;
import com.exam.entity.CourseSection;
import com.exam.entity.CourseSectionVideo;
import com.exam.entity.StudentCourse;
import com.exam.entity.Video;
import com.exam.entity.VideoCategory;
import com.exam.entity.VideoStudyRecord;
import com.exam.mapper.CourseSectionMapper;
import com.exam.mapper.CourseSectionVideoMapper;
import com.exam.mapper.CourseMapper;
import com.exam.mapper.StudentCourseMapper;
import com.exam.mapper.VideoCategoryMapper;
import com.exam.mapper.VideoMapper;
import com.exam.mapper.VideoStudyRecordMapper;
import com.exam.service.CourseService;
import com.exam.vo.CourseDetailVO;
import com.exam.vo.CourseListItemVO;
import com.exam.vo.MyCourseVO;
import com.exam.vo.SectionVO;
import com.exam.vo.VideoInfoVO;
import com.exam.vo.VideoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户端课程Service实现
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Autowired
    private CourseSectionMapper courseSectionMapper;

    @Autowired
    private CourseSectionVideoMapper courseSectionVideoMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private StudentCourseMapper studentCourseMapper;

    @Autowired
    private VideoStudyRecordMapper videoStudyRecordMapper;

    @Autowired
    private VideoCategoryMapper videoCategoryMapper;

    @Override
    public List<CourseListItemVO> getCourseList(Long professionId, Long subjectId, Long categoryId, Long studentId, String keyword) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getStatus, 1);
        wrapper.like(org.springframework.util.StringUtils.hasText(keyword), Course::getName, keyword);
        if (categoryId != null) {
            wrapper.eq(Course::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Course::getIsTop)
                .orderByAsc(Course::getTopSort)
                .orderByAsc(Course::getSort)
                .orderByDesc(Course::getCreateTime);
        List<Course> courses = list(wrapper);

        // 一次性查学生已开通课程, 用于标注每条 purchased 状态
        Set<Long> purchasedIds = new HashSet<>();
        if (studentId != null) {
            LambdaQueryWrapper<StudentCourse> sc = new LambdaQueryWrapper<StudentCourse>()
                    .eq(StudentCourse::getStudentId, studentId)
                    .select(StudentCourse::getCourseId);
            studentCourseMapper.selectList(sc).forEach(r -> purchasedIds.add(r.getCourseId()));
        }

        // 批量统计每个课程的已开通人数(学过人数)
        Map<Long, Integer> studyCountMap = new HashMap<>();
        Set<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toSet());
        if (!courseIds.isEmpty()) {
            LambdaQueryWrapper<StudentCourse> countWrapper = new LambdaQueryWrapper<StudentCourse>()
                    .in(StudentCourse::getCourseId, courseIds)
                    .select(StudentCourse::getCourseId);
            List<StudentCourse> all = studentCourseMapper.selectList(countWrapper);
            for (StudentCourse sc : all) {
                studyCountMap.merge(sc.getCourseId(), 1, Integer::sum);
            }
        }

        // 批量查询课程分类(课程分类直接使用 course.category_id,不再从视频推断)
        // 收集所有课程自身的 categoryId, 查询分类名称
        Set<Long> categoryIds = courses.stream()
                .map(Course::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<VideoCategory> categories = videoCategoryMapper.selectBatchIds(categoryIds);
            for (VideoCategory vc : categories) {
                categoryNameMap.put(vc.getId(), vc.getName());
            }
        }

        // 批量计算每个课程的视频总时长(秒): 通过 course_section -> course_section_video -> video.duration 求和
        Map<Long, Integer> videoDurationMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            // 1. 查所有课程的小节
            LambdaQueryWrapper<CourseSection> sectionWrapper = new LambdaQueryWrapper<>();
            sectionWrapper.in(CourseSection::getCourseId, courseIds)
                    .select(CourseSection::getId, CourseSection::getCourseId);
            List<CourseSection> sections = courseSectionMapper.selectList(sectionWrapper);
            if (!sections.isEmpty()) {
                Map<Long, Long> sectionCourseMap = new HashMap<>();
                for (CourseSection s : sections) {
                    sectionCourseMap.put(s.getId(), s.getCourseId());
                }
                // 2. 查小节-视频关联
                Set<Long> sectionIds = sections.stream().map(CourseSection::getId).collect(Collectors.toSet());
                LambdaQueryWrapper<CourseSectionVideo> csvWrapper = new LambdaQueryWrapper<>();
                csvWrapper.in(CourseSectionVideo::getSectionId, sectionIds)
                        .select(CourseSectionVideo::getSectionId, CourseSectionVideo::getVideoId);
                List<CourseSectionVideo> csvList = courseSectionVideoMapper.selectList(csvWrapper);
                if (!csvList.isEmpty()) {
                    // 3. 查视频时长
                    Set<Long> videoIds = csvList.stream().map(CourseSectionVideo::getVideoId).collect(Collectors.toSet());
                    LambdaQueryWrapper<Video> videoWrapper = new LambdaQueryWrapper<>();
                    videoWrapper.in(Video::getId, videoIds)
                            .select(Video::getId, Video::getDuration);
                    List<Video> videos = videoMapper.selectList(videoWrapper);
                    Map<Long, Integer> videoDurMap = new HashMap<>();
                    for (Video v : videos) {
                        videoDurMap.put(v.getId(), v.getDuration() == null ? 0 : v.getDuration());
                    }
                    // 4. 按课程汇总
                    for (CourseSectionVideo csv : csvList) {
                        Long courseId = sectionCourseMap.get(csv.getSectionId());
                        if (courseId != null) {
                            Integer dur = videoDurMap.get(csv.getVideoId());
                            if (dur != null) {
                                videoDurationMap.merge(courseId, dur, Integer::sum);
                            }
                        }
                    }
                }
            }
        }

        final Set<Long> _purchased = purchasedIds;
        return courses.stream().map(course -> {
            CourseListItemVO vo = new CourseListItemVO();
            vo.setId(course.getId());
            vo.setName(course.getName());
            vo.setCoverUrl(course.getCoverUrl());
            vo.setTag(course.getTag());
            vo.setPrice(course.getPrice());
            vo.setSectionCount(course.getSectionCount());
            // 学时 = 该课程下所有视频时长之和(秒), 动态计算
            vo.setTotalDuration(videoDurationMap.getOrDefault(course.getId(), 0));
            // 学过人数 = 基础学过人数 + 实际开通人数
            int baseStudyCount = course.getBaseStudyCount() == null ? 0 : course.getBaseStudyCount();
            vo.setStudyCount(baseStudyCount + studyCountMap.getOrDefault(course.getId(), 0));
            // 学时(小时) = 后台设置的基数学时, 无单位
            vo.setStudyHours(course.getBaseStudyHours() == null ? 0 : course.getBaseStudyHours());
            // 课程分类: 直接使用课程自身绑定的 categoryId
            Long catId = course.getCategoryId();
            vo.setCategoryId(catId);
            vo.setCategoryName(catId == null ? null : categoryNameMap.get(catId));
            // 未登录时 progress=0(没观看记录), 已登录则按已观看视频数计算
            vo.setProgress(studentId == null ? 0 : calcCourseProgress(course.getId(), studentId));
            // 课程中心：所有上架课程都展示, 标记是否已开通
            vo.setPurchased(_purchased.contains(course.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 计算学生在某课程的学习进度百分比（已观看视频数/总视频数 * 100）
     */
    private Integer calcCourseProgress(Long courseId, Long studentId) {
        // 查询该课程所有小节ID
        LambdaQueryWrapper<CourseSection> sectionWrapper = new LambdaQueryWrapper<>();
        sectionWrapper.eq(CourseSection::getCourseId, courseId);
        List<CourseSection> sections = courseSectionMapper.selectList(sectionWrapper);
        if (sections.isEmpty()) {
            return 0;
        }
        List<Long> sectionIds = sections.stream().map(CourseSection::getId).collect(Collectors.toList());

        // 查询该课程所有视频关联
        LambdaQueryWrapper<CourseSectionVideo> csvWrapper = new LambdaQueryWrapper<>();
        csvWrapper.in(CourseSectionVideo::getSectionId, sectionIds);
        List<CourseSectionVideo> csvList = courseSectionVideoMapper.selectList(csvWrapper);
        if (csvList.isEmpty()) {
            return 0;
        }
        int totalVideoCount = csvList.size();
        List<Long> videoIds = csvList.stream().map(CourseSectionVideo::getVideoId).distinct().collect(Collectors.toList());

        // 查询学生已观看的视频记录
        LambdaQueryWrapper<VideoStudyRecord> vsrWrapper = new LambdaQueryWrapper<>();
        vsrWrapper.eq(VideoStudyRecord::getStudentId, studentId)
                .eq(VideoStudyRecord::getCourseId, courseId)
                .in(VideoStudyRecord::getVideoId, videoIds);
        long watchedCount = videoStudyRecordMapper.selectCount(vsrWrapper);

        return (int) (watchedCount * 100 / totalVideoCount);
    }

    @Override
    public CourseDetailVO getCourseDetail(Long courseId, Long studentId) {
        Course course = getById(courseId);
        if (course == null || course.getStatus() != 1) {
            throw new BusinessException("课程不存在或已下架");
        }

        // 未登录: 仅给基本信息, 不返回任何视频URL
        if (studentId == null) {
            CourseDetailVO vo = new CourseDetailVO();
            vo.setId(course.getId());
            vo.setName(course.getName());
            vo.setCoverUrl(course.getCoverUrl());
            vo.setIntro(course.getIntro());
            vo.setPrice(course.getPrice());
            vo.setTag(course.getTag());
            vo.setTotalDuration(course.getTotalDuration());
            vo.setSectionCount(course.getSectionCount());
            vo.setPurchased(false);
            vo.setNeedLogin(true);
            return vo;
        }

        // 检查学生是否已开通该课程
        boolean purchased = isPurchased(studentId, courseId);

        CourseDetailVO vo = new CourseDetailVO();
        vo.setId(course.getId());
        vo.setName(course.getName());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setIntro(course.getIntro());
        vo.setPrice(course.getPrice());
        vo.setTag(course.getTag());
        vo.setTotalDuration(course.getTotalDuration());
        vo.setSectionCount(course.getSectionCount());
        vo.setPurchased(purchased);
        vo.setNeedLogin(false);

        // 查询小节列表
        LambdaQueryWrapper<CourseSection> sectionWrapper = new LambdaQueryWrapper<>();
        sectionWrapper.eq(CourseSection::getCourseId, courseId)
                .orderByAsc(CourseSection::getSort);
        List<CourseSection> sections = courseSectionMapper.selectList(sectionWrapper);

        List<SectionVO> sectionVOs = new ArrayList<>();
        for (CourseSection section : sections) {
            SectionVO sectionVO = new SectionVO();
            sectionVO.setId(section.getId());
            sectionVO.setName(section.getName());
            sectionVO.setRemark(section.getRemark());

            // 查询小节下的视频
            LambdaQueryWrapper<CourseSectionVideo> csvWrapper = new LambdaQueryWrapper<>();
            csvWrapper.eq(CourseSectionVideo::getSectionId, section.getId())
                    .orderByAsc(CourseSectionVideo::getSort);
            List<CourseSectionVideo> csvList = courseSectionVideoMapper.selectList(csvWrapper);

            List<VideoVO> videoVOs = new ArrayList<>();
            for (CourseSectionVideo csv : csvList) {
                Video video = videoMapper.selectById(csv.getVideoId());
                if (video == null) {
                    continue;
                }
                VideoVO videoVO = new VideoVO();
                videoVO.setId(video.getId());
                videoVO.setName(video.getName());
                videoVO.setCoverUrl(video.getCoverUrl());
                videoVO.setDuration(video.getDuration());
                videoVO.setUpdateTime(video.getUpdateTime());
                // 视频查看权限：viewPermission=1 时需指定权限，未单独授权则不返回URL
                Integer viewPermission = csv.getViewPermission();
                videoVO.setViewPermission(viewPermission);
                // 未开通课程不返回URL；viewPermission=1（需指定权限）也不返回URL
                boolean canView = purchased && (viewPermission == null || viewPermission == 0);
                videoVO.setUrl(canView ? video.getUrl() : null);
                videoVOs.add(videoVO);
            }
            sectionVO.setVideoCount(videoVOs.size());
            sectionVO.setVideos(videoVOs);
            sectionVOs.add(sectionVO);
        }
        vo.setSections(sectionVOs);
        return vo;
    }

    @Override
    public List<MyCourseVO> getMyCourses(Long studentId) {
        // 查询学生已开通的课程
        LambdaQueryWrapper<StudentCourse> scWrapper = new LambdaQueryWrapper<>();
        scWrapper.eq(StudentCourse::getStudentId, studentId);
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(scWrapper);

        if (CollectionUtils.isEmpty(studentCourses)) {
            return new ArrayList<>();
        }

        List<Long> courseIds = studentCourses.stream()
                .map(StudentCourse::getCourseId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.in(Course::getId, courseIds)
                .orderByDesc(Course::getCreateTime);
        List<Course> courses = list(courseWrapper);

        // 找出已被删除的课程ID(仍保留在student_course中)
        Set<Long> foundIds = courses.stream().map(Course::getId).collect(Collectors.toSet());
        Map<Long, String> deletedCourseNames = studentCourses.stream()
                .filter(sc -> !foundIds.contains(sc.getCourseId()))
                .collect(Collectors.toMap(StudentCourse::getCourseId,
                        sc -> sc.getCourseName() != null ? sc.getCourseName() : "已删除课程",
                        (a, b) -> a));

        List<MyCourseVO> result = new ArrayList<>();
        for (Course course : courses) {
            MyCourseVO vo = new MyCourseVO();
            vo.setId(course.getId());
            vo.setName(course.getName());
            vo.setCoverUrl(course.getCoverUrl());
            vo.setTag(course.getTag());
            vo.setPrice(course.getPrice());
            vo.setTotalDuration(course.getTotalDuration());
            vo.setSectionCount(course.getSectionCount());

            // 汇总该课程已学习时长
            LambdaQueryWrapper<VideoStudyRecord> vsrWrapper = new LambdaQueryWrapper<>();
            vsrWrapper.eq(VideoStudyRecord::getStudentId, studentId)
                    .eq(VideoStudyRecord::getCourseId, course.getId());
            List<VideoStudyRecord> records = videoStudyRecordMapper.selectList(vsrWrapper);

            int watchedDuration = records.stream()
                    .mapToInt(r -> r.getWatchedDuration() == null ? 0 : r.getWatchedDuration())
                    .sum();
            vo.setWatchedDuration(watchedDuration);

            // 计算学习进度百分比
            BigDecimal studyProgress = BigDecimal.ZERO;
            if (course.getTotalDuration() != null && course.getTotalDuration() > 0) {
                studyProgress = BigDecimal.valueOf(watchedDuration)
                        .multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(course.getTotalDuration()), 2, RoundingMode.HALF_UP);
            }
            vo.setStudyProgress(studyProgress);
            result.add(vo);
        }
        // 追加已删除的课程(仍展示给学生看历史记录)
        for (Map.Entry<Long, String> entry : deletedCourseNames.entrySet()) {
            MyCourseVO vo = new MyCourseVO();
            vo.setId(entry.getKey());
            vo.setName(entry.getValue());
            vo.setSectionCount(0);
            vo.setTotalDuration(0);
            // 汇总已删除课程的学习时长
            LambdaQueryWrapper<VideoStudyRecord> vsrWrapper = new LambdaQueryWrapper<>();
            vsrWrapper.eq(VideoStudyRecord::getStudentId, studentId)
                    .eq(VideoStudyRecord::getCourseId, entry.getKey());
            List<VideoStudyRecord> records = videoStudyRecordMapper.selectList(vsrWrapper);
            int watchedDuration = records.stream()
                    .mapToInt(r -> r.getWatchedDuration() == null ? 0 : r.getWatchedDuration())
                    .sum();
            vo.setWatchedDuration(watchedDuration);
            vo.setStudyProgress(BigDecimal.ZERO);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportVideoProgress(Long studentId, VideoProgressDTO dto) {
        Long videoId = dto.getVideoId();
        Long courseId = dto.getCourseId();

        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }

        // 查询是否已有学习记录
        LambdaQueryWrapper<VideoStudyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoStudyRecord::getStudentId, studentId)
                .eq(VideoStudyRecord::getCourseId, courseId)
                .eq(VideoStudyRecord::getVideoId, videoId);
        VideoStudyRecord existRecord = videoStudyRecordMapper.selectOne(wrapper);

        boolean firstWatch = (existRecord == null);
        if (firstWatch) {
            // 首次观看，创建学习记录
            VideoStudyRecord record = new VideoStudyRecord();
            record.setStudentId(studentId);
            record.setCourseId(courseId);
            record.setVideoId(videoId);
            record.setProgress(dto.getProgress());
            record.setWatchedDuration(dto.getWatchedDuration());
            record.setLastWatchTime(LocalDateTime.now());
            videoStudyRecordMapper.insert(record);

            // 首次观看增加视频播放次数
            Video update = new Video();
            update.setId(video.getId());
            Integer playCount = video.getPlayCount() == null ? 0 : video.getPlayCount();
            update.setPlayCount(playCount + 1);
            videoMapper.updateById(update);
        } else {
            // 更新学习记录
            VideoStudyRecord update = new VideoStudyRecord();
            update.setId(existRecord.getId());
            update.setProgress(dto.getProgress());
            // 累加观看时长
            int existWatched = existRecord.getWatchedDuration() == null ? 0 : existRecord.getWatchedDuration();
            int addWatched = dto.getWatchedDuration() == null ? 0 : dto.getWatchedDuration();
            update.setWatchedDuration(existWatched + addWatched);
            update.setLastWatchTime(LocalDateTime.now());
            videoStudyRecordMapper.updateById(update);
        }
    }

    @Override
    public VideoInfoVO getVideoInfo(Long studentId, Long videoId, Long courseId) {
        // 验证学生是否已开通该课程
        if (!isPurchased(studentId, courseId)) {
            throw new BusinessException("请先开通该课程");
        }

        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }

        VideoInfoVO vo = new VideoInfoVO();
        vo.setId(video.getId());
        vo.setName(video.getName());
        vo.setUrl(video.getUrl());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setDuration(video.getDuration());

        // 查询学习进度
        LambdaQueryWrapper<VideoStudyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoStudyRecord::getStudentId, studentId)
                .eq(VideoStudyRecord::getCourseId, courseId)
                .eq(VideoStudyRecord::getVideoId, videoId);
        VideoStudyRecord record = videoStudyRecordMapper.selectOne(wrapper);
        vo.setProgress(record == null || record.getProgress() == null ? 0 : record.getProgress());
        return vo;
    }

    /**
     * 判断学生是否已开通课程
     */
    private boolean isPurchased(Long studentId, Long courseId) {
        LambdaQueryWrapper<StudentCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getCourseId, courseId);
        return studentCourseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean checkCourseAccess(Long courseId, Long studentId) {
        if (studentId == null) {
            // code=1001 用于前端识别"未登录"场景
            throw new BusinessException(1001, "请先登录后再访问课程");
        }
        Course course = getById(courseId);
        if (course == null || course.getStatus() != 1) {
            throw new BusinessException("课程不存在或已下架");
        }
        if (!isPurchased(studentId, courseId)) {
            // code=1002 用于前端识别"未开通"场景
            throw new BusinessException(1002, "您尚未开通该课程,请联系管理员先开通");
        }
        return true;
    }
}
