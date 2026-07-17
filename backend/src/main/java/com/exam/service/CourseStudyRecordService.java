package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.VideoStudyRecord;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface CourseStudyRecordService extends IService<VideoStudyRecord> {

    /**
     * 分页查询学习记录
     */
    PageResult<Map<String, Object>> page(Integer page, Integer size, String courseName,
                                         String studyTimeStart, String studyTimeEnd,
                                         Integer courseStatus, String phone);

    /**
     * 学习记录详情
     */
    Map<String, Object> detail(Long id);

    /**
     * 导出学习记录为Excel
     */
    void export(HttpServletResponse response, String courseName, String studyTimeStart,
                String studyTimeEnd, Integer courseStatus, String phone);
}
