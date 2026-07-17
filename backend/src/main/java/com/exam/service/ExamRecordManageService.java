package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.ExamRecord;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface ExamRecordManageService extends IService<ExamRecord> {

    /**
     * 分页查询考试记录
     */
    PageResult<Map<String, Object>> page(Integer page, Integer size, String examName,
                                         String phone, Integer submitStatus);

    /**
     * 考试记录详情（含学生作答详情）
     */
    Map<String, Object> detail(Long id);

    /**
     * 导出考试记录为Excel
     */
    void export(HttpServletResponse response, String examName, String phone, Integer submitStatus);

    /**
     * 批改简答题(人工评分),批改后刷新考试记录总分
     * @param recordId 考试记录ID
     * @param grades List<Map{answerId, score, isCorrect}> 每题的评分
     */
    void gradeAnswers(Long recordId, List<Map<String, Object>> grades);

    /**
     * 删除考试记录（同时删除关联的答题记录）
     */
    void deleteRecord(Long id);

    /**
     * 批量删除考试记录（同时删除关联的答题记录）
     */
    void batchDeleteRecords(List<Long> ids);
}
