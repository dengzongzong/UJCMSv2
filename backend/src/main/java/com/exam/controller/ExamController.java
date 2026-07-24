package com.exam.controller;

import com.exam.common.Result;
import com.exam.common.PageResult;
import com.exam.dto.SaveAnswerDTO;
import com.exam.dto.SubmitExamDTO;
import com.exam.service.ExamService;
import com.exam.vo.ExamIntroVO;
import com.exam.vo.ExamListItemVO;
import com.exam.vo.ExamRecordVO;
import com.exam.vo.ExamResultVO;
import com.exam.vo.ExamStartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户端考试Controller
 */
@RestController
@RequestMapping("/user/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    /**
     * 考试中心(列表): 所有启用考试, 未登录也能浏览; 已登录则标记 purchased
     * <p>支持分页: 传入 page/pageSize 时返回 PageResult; 不传时返回全量 List(向后兼容)</p>
     * <p>purchasedOnly=true 且已登录时, 仅返回当前学生已开通的考试(用于"我的考试"页)</p>
     */
    @GetMapping("/list")
    public Result<Object> list(@RequestAttribute(value = "userId", required = false) Long userId,
                               @RequestParam(required = false) Long professionId,
                               @RequestParam(required = false) Long subjectId,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer page,
                               @RequestParam(required = false) Integer pageSize,
                               @RequestParam(required = false, defaultValue = "false") Boolean purchasedOnly) {
        if (page != null) {
            return Result.success(examService.getExamListPage(userId, professionId, subjectId, keyword, page, pageSize, purchasedOnly));
        }
        return Result.success(examService.getExamList(userId, professionId, subjectId, keyword));
    }

    /**
     * 考试中心(公开): 允许未登录访问
     * <p>等同 /user/exam/list,放在 /public/** 下以便 JwtInterceptor 不强制要求登录</p>
     * <p>支持分页: 传入 page/pageSize 时返回 PageResult; 不传时返回全量 List(向后兼容)</p>
     */
    @GetMapping("/public/list")
    public Result<Object> publicList(@RequestAttribute(value = "userId", required = false) Long userId,
                                     @RequestParam(required = false) Long professionId,
                                     @RequestParam(required = false) Long subjectId,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer pageSize) {
        if (page != null) {
            return Result.success(examService.getExamListPage(userId, professionId, subjectId, keyword, page, pageSize, false));
        }
        return Result.success(examService.getExamList(userId, professionId, subjectId, keyword));
    }

    /**
     * 考试访问闸门校验(进入详情/开始考试前)
     */
    @GetMapping("/check-access")
    public Result<Boolean> checkAccess(@RequestParam Long examId,
                                       @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(examService.checkExamAccess(examId, userId));
    }

    /**
     * 试卷介绍页
     */
    @GetMapping("/intro")
    public Result<ExamIntroVO> intro(@RequestParam Long examId) {
        return Result.success(examService.getExamIntro(examId));
    }

    /**
     * 开始考试
     */
    @PostMapping("/start")
    public Result<ExamStartVO> start(@RequestParam Long examId,
                                     @RequestAttribute("userId") Long userId) {
        return Result.success(examService.startExam(userId, examId));
    }

    /**
     * 取得考试题目(刷新页面/重连时使用)
     *
     * <p>请求:{ examId, recordId? }</p>
     * <ul>
     *   <li>recordId 为空: 等价于 startExam,会创建新 record</li>
     *   <li>recordId 不为空: 校验归属后直接返回题目,不创建新 record</li>
     * </ul>
     */
    @GetMapping("/paper")
    public Result<com.exam.vo.ExamPaperVO> paper(@RequestParam Long examId,
                                                  @RequestParam(required = false) Long recordId,
                                                  @RequestAttribute("userId") Long userId) {
        return Result.success(examService.getExamPaper(userId, examId, recordId));
    }

    /**
     * 保存单题答案（断点续考用）
     * <p>考试过程中每答一题即保存，避免中途退出丢失已答内容。</p>
     */
    @PostMapping("/answer")
    public Result<Void> saveAnswer(@RequestAttribute("userId") Long userId,
                                   @RequestBody SaveAnswerDTO dto) {
        examService.saveAnswer(userId, dto.getRecordId(), dto.getQuestionId(), dto.getStudentAnswer());
        return Result.success(null);
    }

    /**
     * 提交考试
     */
    @PostMapping("/submit")
    public Result<ExamResultVO> submit(@RequestAttribute("userId") Long userId,
                                       @RequestBody SubmitExamDTO dto) {
        return Result.success(examService.submitExam(userId, dto));
    }

    /**
     * 自动交卷（中途退出/关闭页面/倒计时结束）
     */
    @PostMapping("/auto-submit")
    public Result<ExamResultVO> autoSubmit(@RequestAttribute("userId") Long userId,
                                           @RequestBody SubmitExamDTO dto) {
        return Result.success(examService.autoSubmitExam(userId, dto));
    }

    /**
     * 查看考试结果
     */
    @GetMapping("/result")
    public Result<ExamResultVO> result(@RequestParam Long recordId,
                                       @RequestAttribute("userId") Long userId) {
        return Result.success(examService.getExamResult(userId, recordId));
    }

    /**
     * 查看试卷(只读,题目+选项+参考答案+用户最近一次作答)
     * <p>从已通过的考试结果记录或最新一条 record 中读取作答,用于证书查询页的试卷预览</p>
     */
    @GetMapping("/paper/view")
    public Result<com.exam.vo.PaperViewVO> viewPaper(@RequestParam Long examId,
                                                      @RequestAttribute("userId") Long userId) {
        return Result.success(examService.viewExamPaper(userId, examId));
    }

    /**
     * 我的考试记录列表
     * <p>支持分页: 传入 page/pageSize 时返回含统计概览的 Map; 不传时返回全量 List(向后兼容)</p>
     */
    @GetMapping("/records")
    public Result<Object> records(@RequestAttribute("userId") Long userId,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer pageSize) {
        if (page != null) {
            return Result.success(examService.getExamRecordsPage(userId, page, pageSize));
        }
        return Result.success(examService.getExamRecords(userId));
    }

    /**
     * 我的考试记录(按专业分组,每个专业只取最高分)
     */
    @GetMapping("/best-records")
    public Result<List<Map<String, Object>>> bestRecords(@RequestAttribute("userId") Long userId) {
        return Result.success(examService.getBestRecordsByProfession(userId));
    }
}
