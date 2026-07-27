package com.exam.controller;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.service.CourseStudyRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 课程学习记录
 */
@RestController
@RequestMapping("/admin/course-record")
public class CourseStudyRecordController {

    @Autowired
    private CourseStudyRecordService courseStudyRecordService;

    /**
     * 分页查询学习记录
     */
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size,
                                                        @RequestParam(required = false) String courseName,
                                                        @RequestParam(required = false) String studyTimeStart,
                                                        @RequestParam(required = false) String studyTimeEnd,
                                                        @RequestParam(required = false) Integer courseStatus,
                                                        @RequestParam(required = false) String phone,
                                                        @RequestParam(required = false) Integer exactCount,
                                                        HttpServletRequest request) {
        // 精确显示条数: 仅返回最新N条(覆盖分页参数)
        if (exactCount != null && exactCount > 0) {
            page = 1;
            size = exactCount;
        }
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Map<String, Object>> result = courseStudyRecordService.page(page, size, courseName,
                studyTimeStart, studyTimeEnd, courseStatus, phone, exactCount);
        return Result.success(result);
    }

    /**
     * 学习记录详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> result = courseStudyRecordService.detail(id);
        return Result.success(result);
    }

    /**
     * 导出学习记录为Excel
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String courseName,
                       @RequestParam(required = false) String studyTimeStart,
                       @RequestParam(required = false) String studyTimeEnd,
                       @RequestParam(required = false) Integer courseStatus,
                       @RequestParam(required = false) String phone,
                       HttpServletResponse response) {
        courseStudyRecordService.export(response, courseName, studyTimeStart, studyTimeEnd, courseStatus, phone);
    }
}
