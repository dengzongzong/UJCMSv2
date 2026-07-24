package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.SubmitExamDTO;
import com.exam.entity.Exam;
import com.exam.vo.ExamIntroVO;
import com.exam.vo.ExamListItemVO;
import com.exam.vo.ExamPaperVO;
import com.exam.vo.ExamRecordVO;
import com.exam.vo.ExamResultVO;
import com.exam.vo.ExamStartVO;

import java.util.List;
import java.util.Map;

/**
 * 用户端考试Service
 */
public interface ExamService extends IService<Exam> {

    /**
     * 考试列表（已开通的考试，含上次成绩，可按专业科目筛选，可按关键词搜索名称）
     */
    List<ExamListItemVO> getExamList(Long studentId, Long professionId, Long subjectId, String keyword);

    /**
     * 考试列表(分页版本)
     * <p>用于考试中心 / 我的考试 等列表页无限滚动加载, 每页最多 50 条。</p>
     *
     * @param studentId     学生ID(可选, 登录后用于标注 purchased / 上次成绩)
     * @param professionId  专业ID(保留参数, 兼容旧签名)
     * @param subjectId     科目ID(保留参数, 兼容旧签名)
     * @param keyword       关键词(可选, 按考试名称模糊匹配)
     * @param page          页码(从 1 开始)
     * @param pageSize      每页条数
     * @param purchasedOnly 为 true 且 studentId 不为空时, 仅返回当前学生已开通的考试
     */
    PageResult<ExamListItemVO> getExamListPage(Long studentId, Long professionId, Long subjectId,
                                               String keyword, Integer page, Integer pageSize, Boolean purchasedOnly);

    /**
     * 试卷介绍页
     */
    ExamIntroVO getExamIntro(Long examId);

    /**
     * 开始考试
     */
    ExamStartVO startExam(Long studentId, Long examId);

    /**
     * 取得试卷题目(不创建新 record;如果 recordId 为空则创建)
     *
     * <p>用于 /user/exam/paper 接口: 刷新页面/重连时再次拿到题目。
     * - recordId 为空: 走 startExam 流程,创建 record 并返回题目
     * - recordId 不为空: 校验 record 归属后直接返回题目(不会重复创建 record)</p>
     */
    ExamPaperVO getExamPaper(Long studentId, Long examId, Long recordId);

    /**
     * 提交考试
     */
    ExamResultVO submitExam(Long studentId, SubmitExamDTO dto);

    /**
     * 自动交卷（submitStatus=2）
     */
    ExamResultVO autoSubmitExam(Long studentId, SubmitExamDTO dto);

    /**
     * 保存单题答案（断点续考用）
     * <p>校验 recordId 归属当前 studentId 且 submitStatus=0；对 (recordId, questionId) 执行 upsert。</p>
     */
    void saveAnswer(Long studentId, Long recordId, Long questionId, String studentAnswer);

    /**
     * 查看考试结果
     */
    ExamResultVO getExamResult(Long studentId, Long recordId);

    /**
     * 我的考试记录列表
     */
    List<ExamRecordVO> getExamRecords(Long studentId);

    /**
     * 我的考试记录列表(分页版本, 同时返回统计概览)
     * <p>返回 Map 结构: { total, page, size, records, avgScore, maxScore, passRate }。
     * 统计概览(平均分/最高分/通过率)基于该学生全部考试记录计算, records 仅为当前分页。</p>
     *
     * @param studentId 学生ID
     * @param page      页码(从 1 开始)
     * @param pageSize  每页条数
     */
    Map<String, Object> getExamRecordsPage(Long studentId, Integer page, Integer pageSize);

    /**
     * 考试访问闸门校验(进入介绍页/开始考试前的权限闸门)
     * <ul>
     *   <li>studentId == null: 抛业务异常(由前端拦截跳登录)</li>
     *   <li>已登录但未开通: 抛业务异常(由前端拦截提示)</li>
     *   <li>已开通: 返回 true</li>
     * </ul>
     */
    boolean checkExamAccess(Long examId, Long studentId);

    /**
     * 查看试卷(只读,供证书查询页面使用)
     * <p>返回结构:{ questions: 题目列表(包含options、correctAnswer), answers: {questionId: 用户作答} }</p>
     * <p>answers 从该用户的最新考试记录中获取;若从未参加过则为空 map</p>
     */
    com.exam.vo.PaperViewVO viewExamPaper(Long studentId, Long examId);

    /**
     * 我的考试记录(按专业分组,每个专业只取最高分)
     */
    List<Map<String, Object>> getBestRecordsByProfession(Long studentId);
}
