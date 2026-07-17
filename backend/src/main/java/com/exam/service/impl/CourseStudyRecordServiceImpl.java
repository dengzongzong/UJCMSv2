package com.exam.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.CourseStudyRecordService;
import com.exam.vo.CourseStudyRecordExportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseStudyRecordServiceImpl extends ServiceImpl<VideoStudyRecordMapper, VideoStudyRecord> implements CourseStudyRecordService {

    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private VideoMapper videoMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<Map<String, Object>> page(Integer page, Integer size, String courseName,
                                                String studyTimeStart, String studyTimeEnd,
                                                Integer courseStatus, String phone) {
        // 根据课程名称/状态筛选课程ID
        Set<Long> courseIds = null;
        if (StringUtils.hasText(courseName) || courseStatus != null) {
            LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
            courseWrapper.like(StringUtils.hasText(courseName), Course::getName, courseName);
            courseWrapper.eq(courseStatus != null, Course::getStatus, courseStatus);
            List<Course> courses = courseMapper.selectList(courseWrapper);
            courseIds = courses.stream().map(Course::getId).collect(Collectors.toSet());
            if (courseIds.isEmpty()) {
                PageResult<Map<String, Object>> empty = new PageResult<>();
                empty.setTotal(0);
                empty.setPage(page);
                empty.setSize(size);
                empty.setRecords(new ArrayList<>());
                return empty;
            }
        }
        // 根据手机号筛选学生ID
        Set<Long> studentIds = null;
        if (StringUtils.hasText(phone)) {
            List<Student> students = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().like(Student::getPhone, phone));
            studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
            if (studentIds.isEmpty()) {
                PageResult<Map<String, Object>> empty = new PageResult<>();
                empty.setTotal(0);
                empty.setPage(page);
                empty.setSize(size);
                empty.setRecords(new ArrayList<>());
                return empty;
            }
        }

        LambdaQueryWrapper<VideoStudyRecord> wrapper = new LambdaQueryWrapper<VideoStudyRecord>()
                .in(courseIds != null, VideoStudyRecord::getCourseId, courseIds)
                .in(studentIds != null, VideoStudyRecord::getStudentId, studentIds)
                .orderByDesc(VideoStudyRecord::getCreateTime);
        if (StringUtils.hasText(studyTimeStart)) {
            wrapper.ge(VideoStudyRecord::getCreateTime, LocalDate.parse(studyTimeStart).atStartOfDay());
        }
        if (StringUtils.hasText(studyTimeEnd)) {
            wrapper.le(VideoStudyRecord::getCreateTime, LocalDate.parse(studyTimeEnd).atTime(23, 59, 59));
        }

        Page<VideoStudyRecord> p = new Page<>(page, size);
        Page<VideoStudyRecord> result = this.page(p, wrapper);

        // 批量查询课程和学生信息
        List<Long> rcCourseIds = result.getRecords().stream().map(VideoStudyRecord::getCourseId).distinct().collect(Collectors.toList());
        List<Long> rcStudentIds = result.getRecords().stream().map(VideoStudyRecord::getStudentId).distinct().collect(Collectors.toList());
        List<Long> rcVideoIds = result.getRecords().stream().map(VideoStudyRecord::getVideoId).distinct().collect(Collectors.toList());

        Map<Long, Course> courseMap = rcCourseIds.isEmpty() ? new HashMap<>() :
                courseMapper.selectBatchIds(rcCourseIds).stream().collect(Collectors.toMap(Course::getId, c -> c));
        Map<Long, Student> studentMap = rcStudentIds.isEmpty() ? new HashMap<>() :
                studentMapper.selectBatchIds(rcStudentIds).stream().collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, Video> videoMap = rcVideoIds.isEmpty() ? new HashMap<>() :
                videoMapper.selectBatchIds(rcVideoIds).stream().collect(Collectors.toMap(Video::getId, v -> v));

        List<Map<String, Object>> records = new ArrayList<>();
        int index = 1;
        for (VideoStudyRecord vsr : result.getRecords()) {
            Map<String, Object> item = buildRecordMap(vsr, courseMap, studentMap, videoMap);
            item.put("index", (int) ((result.getCurrent() - 1) * result.getSize() + index));
            index++;
            records.add(item);
        }

        PageResult<Map<String, Object>> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setRecords(records);
        return pageResult;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        VideoStudyRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException("学习记录不存在");
        }
        Map<Long, Course> courseMap = new HashMap<>();
        Map<Long, Student> studentMap = new HashMap<>();
        Map<Long, Video> videoMap = new HashMap<>();
        Course course = courseMapper.selectById(record.getCourseId());
        if (course != null) {
            courseMap.put(course.getId(), course);
        }
        Student student = studentMapper.selectById(record.getStudentId());
        if (student != null) {
            studentMap.put(student.getId(), student);
        }
        Video video = videoMapper.selectById(record.getVideoId());
        if (video != null) {
            videoMap.put(video.getId(), video);
        }
        return buildRecordMap(record, courseMap, studentMap, videoMap);
    }

    @Override
    public void export(HttpServletResponse response, String courseName, String studyTimeStart,
                       String studyTimeEnd, Integer courseStatus, String phone) {
        // 查询全部数据（不分页）
        Set<Long> courseIds = null;
        if (StringUtils.hasText(courseName) || courseStatus != null) {
            LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
            courseWrapper.like(StringUtils.hasText(courseName), Course::getName, courseName);
            courseWrapper.eq(courseStatus != null, Course::getStatus, courseStatus);
            List<Course> courses = courseMapper.selectList(courseWrapper);
            courseIds = courses.stream().map(Course::getId).collect(Collectors.toSet());
            if (courseIds.isEmpty()) {
                writeExcel(response, new ArrayList<>());
                return;
            }
        }
        Set<Long> studentIds = null;
        if (StringUtils.hasText(phone)) {
            List<Student> students = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().like(Student::getPhone, phone));
            studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
            if (studentIds.isEmpty()) {
                writeExcel(response, new ArrayList<>());
                return;
            }
        }

        LambdaQueryWrapper<VideoStudyRecord> wrapper = new LambdaQueryWrapper<VideoStudyRecord>()
                .in(courseIds != null, VideoStudyRecord::getCourseId, courseIds)
                .in(studentIds != null, VideoStudyRecord::getStudentId, studentIds)
                .orderByDesc(VideoStudyRecord::getCreateTime);
        if (StringUtils.hasText(studyTimeStart)) {
            wrapper.ge(VideoStudyRecord::getCreateTime, LocalDate.parse(studyTimeStart).atStartOfDay());
        }
        if (StringUtils.hasText(studyTimeEnd)) {
            wrapper.le(VideoStudyRecord::getCreateTime, LocalDate.parse(studyTimeEnd).atTime(23, 59, 59));
        }
        List<VideoStudyRecord> list = this.list(wrapper);

        List<Long> rcCourseIds = list.stream().map(VideoStudyRecord::getCourseId).distinct().collect(Collectors.toList());
        List<Long> rcStudentIds = list.stream().map(VideoStudyRecord::getStudentId).distinct().collect(Collectors.toList());
        List<Long> rcVideoIds = list.stream().map(VideoStudyRecord::getVideoId).distinct().collect(Collectors.toList());
        Map<Long, Course> courseMap = rcCourseIds.isEmpty() ? new HashMap<>() :
                courseMapper.selectBatchIds(rcCourseIds).stream().collect(Collectors.toMap(Course::getId, c -> c));
        Map<Long, Student> studentMap = rcStudentIds.isEmpty() ? new HashMap<>() :
                studentMapper.selectBatchIds(rcStudentIds).stream().collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, Video> videoMap = rcVideoIds.isEmpty() ? new HashMap<>() :
                videoMapper.selectBatchIds(rcVideoIds).stream().collect(Collectors.toMap(Video::getId, v -> v));

        List<CourseStudyRecordExportVO> exportList = new ArrayList<>();
        for (VideoStudyRecord vsr : list) {
            Course course = courseMap.get(vsr.getCourseId());
            Student student = studentMap.get(vsr.getStudentId());
            Video video = videoMap.get(vsr.getVideoId());
            CourseStudyRecordExportVO vo = new CourseStudyRecordExportVO();
            vo.setCourseName(course != null ? course.getName() : "");
            vo.setTag(course != null ? course.getTag() : "");
            vo.setPrice(course != null ? course.getPrice().toPlainString() : "");
            vo.setSectionCount(course != null ? course.getSectionCount() : 0);
            vo.setStatus(course != null && course.getStatus() == 1 ? "已上架" : "未上架");
            vo.setPhone(student != null ? student.getPhone() : "");
            vo.setStudyStartTime(vsr.getCreateTime() != null ? vsr.getCreateTime().format(FMT) : "");
            vo.setProgress(calcProgressValue(vsr, video) + "%");
            exportList.add(vo);
        }
        writeExcel(response, exportList);
    }

    private void writeExcel(HttpServletResponse response, List<CourseStudyRecordExportVO> data) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("课程学习记录", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), CourseStudyRecordExportVO.class)
                    .sheet("课程学习记录")
                    .doWrite(data);
        } catch (IOException e) {
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }

    private Map<String, Object> buildRecordMap(VideoStudyRecord vsr, Map<Long, Course> courseMap,
                                                Map<Long, Student> studentMap, Map<Long, Video> videoMap) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", vsr.getId());
        item.put("studentId", vsr.getStudentId());
        item.put("courseId", vsr.getCourseId());
        item.put("videoId", vsr.getVideoId());
        item.put("watchedDuration", vsr.getWatchedDuration());

        Course course = courseMap.get(vsr.getCourseId());
        if (course != null) {
            item.put("courseName", course.getName());
            item.put("tag", course.getTag());
            item.put("price", course.getPrice());
            item.put("sectionCount", course.getSectionCount());
            item.put("courseStatus", course.getStatus());
        } else {
            // 课程已删除，使用冗余字段
            item.put("courseName", vsr.getCourseName());
        }
        Student student = studentMap.get(vsr.getStudentId());
        if (student != null) {
            item.put("phone", student.getPhone());
            item.put("nickname", student.getNickname());
        }
        Video video = videoMap.get(vsr.getVideoId());
        if (video != null) {
            item.put("videoName", video.getName());
        } else {
            item.put("videoName", vsr.getVideoName());
        }
        int progress = calcProgressValue(vsr, video);
        item.put("progress", progress);
        item.put("status", progress >= 100 ? 2 : 1);
        item.put("startTime", vsr.getCreateTime());
        item.put("lastStudyTime", vsr.getLastWatchTime());
        return item;
    }

    private int calcProgressValue(VideoStudyRecord vsr, Video video) {
        // progress 字段存的是百分比(0-100)，直接取值即可
        int progress = vsr.getProgress() == null ? 0 : vsr.getProgress();
        return Math.min(progress, 100);
    }
}
