package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.AnswerDTO;
import com.exam.dto.SubmitExamDTO;
import com.exam.entity.Certificate;
import com.exam.entity.Exam;
import com.exam.entity.ExamAnswer;
import com.exam.entity.ExamRecord;
import com.exam.entity.PaperQuestion;
import com.exam.entity.Profession;
import com.exam.entity.Question;
import com.exam.entity.QuestionOption;
import com.exam.entity.Student;
import com.exam.entity.StudentExam;
import com.exam.entity.WrongQuestion;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.ExamAnswerMapper;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.PaperQuestionMapper;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.QuestionOptionMapper;
import com.exam.mapper.StudentExamMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.WrongQuestionMapper;
import com.exam.service.CertificateService;
import com.exam.service.ExamService;
import com.exam.vo.AnswerResultVO;
import com.exam.vo.ExamPaperVO;
import com.exam.vo.ExamIntroVO;
import com.exam.vo.ExamListItemVO;
import com.exam.vo.ExamRecordVO;
import com.exam.vo.ExamResultVO;
import com.exam.vo.ExamStartVO;
import com.exam.vo.OptionVO;
import com.exam.vo.QuestionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户端考试Service实现
 */
@Slf4j
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {

    @Autowired
    private StudentExamMapper studentExamMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private ProfessionMapper professionMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CertificateMapper certificateMapper;

    @Override
    public List<ExamListItemVO> getExamList(Long studentId, Long professionId, Long subjectId, String keyword) {
        LambdaQueryWrapper<Exam> examWrapper = new LambdaQueryWrapper<>();
        examWrapper.eq(Exam::getStatus, 1)
                .like(org.springframework.util.StringUtils.hasText(keyword), Exam::getName, keyword)
                .orderByDesc(Exam::getCreateTime);
        List<Exam> exams = list(examWrapper);

        // 一次性查专业名称: 收集所有不为 null 的 professionId, 批量查询并构建 id -> name 映射
        Set<Long> professionIds = exams.stream()
                .map(Exam::getProfessionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> professionNameMap = new HashMap<>();
        if (!professionIds.isEmpty()) {
            List<Profession> professions = professionMapper.selectBatchIds(professionIds);
            for (Profession p : professions) {
                professionNameMap.put(p.getId(), p.getName());
            }
        }

        // 一次性查学生已开通考试
        Set<Long> purchasedIds = new HashSet<>();
        if (studentId != null) {
            LambdaQueryWrapper<StudentExam> seW = new LambdaQueryWrapper<StudentExam>()
                    .eq(StudentExam::getStudentId, studentId)
                    .select(StudentExam::getExamId);
            studentExamMapper.selectList(seW).forEach(r -> purchasedIds.add(r.getExamId()));
        }

        // 批量统计每个考试的已开通人数(考过人数 = 基数 + 实际开通权限人数)
        Map<Long, Integer> examCountMap = new HashMap<>();
        if (!exams.isEmpty()) {
            Set<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<StudentExam> countWrapper = new LambdaQueryWrapper<StudentExam>()
                    .in(StudentExam::getExamId, examIds)
                    .select(StudentExam::getExamId);
            studentExamMapper.selectList(countWrapper).forEach(r -> examCountMap.merge(r.getExamId(), 1, Integer::sum));
        }
        final Set<Long> _purchased = purchasedIds;

        List<ExamListItemVO> result = new ArrayList<>();
        for (Exam exam : exams) {
            ExamListItemVO vo = new ExamListItemVO();
            vo.setId(exam.getId());
            vo.setName(exam.getName());
            vo.setCategory(exam.getCategory());
            vo.setCoverUrl(exam.getCoverUrl());
            vo.setQuestionCount(exam.getQuestionCount());
            vo.setTotalScore(exam.getTotalScore());
            vo.setDuration(exam.getDuration());
            // 考过人数 = 基础考过人数 + 实际开通权限人数
            int baseExamCount = exam.getBaseExamCount() == null ? 0 : exam.getBaseExamCount();
            vo.setExamCount(baseExamCount + examCountMap.getOrDefault(exam.getId(), 0));
            vo.setPurchased(_purchased.contains(exam.getId()));
            // 设置专业ID和专业名称(professionId 为 null 时记为 "通用考试")
            vo.setProfessionId(exam.getProfessionId());
            if (exam.getProfessionId() == null) {
                vo.setProfessionName("通用考试");
            } else {
                vo.setProfessionName(professionNameMap.getOrDefault(exam.getProfessionId(), "通用考试"));
            }

            // 只有已登录且已开通时, 才查上次考试记录(否则返回 null)
            if (studentId != null && vo.getPurchased()) {
                try {
                    LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
                    recordWrapper.eq(ExamRecord::getStudentId, studentId)
                            .eq(ExamRecord::getExamId, exam.getId())
                            .select(ExamRecord::getScore, ExamRecord::getSubmitTime, ExamRecord::getCreateTime)
                            .orderByDesc(ExamRecord::getCreateTime)
                            .last("LIMIT 1");
                    ExamRecord lastRecord = examRecordMapper.selectOne(recordWrapper);
                    if (lastRecord != null) {
                        vo.setLastScore(lastRecord.getScore());
                        vo.setLastTime(lastRecord.getSubmitTime() != null ? lastRecord.getSubmitTime() : lastRecord.getCreateTime());
                    }
                } catch (Exception e) {
                    // 查询考试记录失败时静默跳过,不影响考试列表展示
                    log.warn("查询考试记录失败: studentId={}, examId={}", studentId, exam.getId(), e);
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public PageResult<ExamListItemVO> getExamListPage(Long studentId, Long professionId, Long subjectId,
                                                       String keyword, Integer page, Integer pageSize, Boolean purchasedOnly) {
        // 参数默认值
        int pageNum = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 50 : Math.min(pageSize, 50);

        LambdaQueryWrapper<Exam> examWrapper = new LambdaQueryWrapper<>();
        examWrapper.eq(Exam::getStatus, 1)
                .like(org.springframework.util.StringUtils.hasText(keyword), Exam::getName, keyword)
                .orderByDesc(Exam::getCreateTime);

        // purchasedOnly=true 且已登录: 仅返回当前学生已开通的考试
        if (Boolean.TRUE.equals(purchasedOnly) && studentId != null) {
            LambdaQueryWrapper<StudentExam> seW = new LambdaQueryWrapper<StudentExam>()
                    .eq(StudentExam::getStudentId, studentId)
                    .select(StudentExam::getExamId);
            Set<Long> purchasedExamIds = studentExamMapper.selectList(seW).stream()
                    .map(StudentExam::getExamId)
                    .collect(Collectors.toSet());
            if (purchasedExamIds.isEmpty()) {
                return new PageResult<>(0, pageNum, size, new ArrayList<>());
            }
            examWrapper.in(Exam::getId, purchasedExamIds);
        }

        // 分页查询
        Page<Exam> pageParam = new Page<>(pageNum, size);
        IPage<Exam> examPage = this.page(pageParam, examWrapper);
        List<Exam> exams = examPage.getRecords();

        // 一次性查专业名称
        Set<Long> professionIds = exams.stream()
                .map(Exam::getProfessionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> professionNameMap = new HashMap<>();
        if (!professionIds.isEmpty()) {
            List<Profession> professions = professionMapper.selectBatchIds(professionIds);
            for (Profession p : professions) {
                professionNameMap.put(p.getId(), p.getName());
            }
        }

        // 一次性查学生已开通考试(当 purchasedOnly=false 时需要标注 purchased)
        Set<Long> purchasedIds = new HashSet<>();
        if (studentId != null && !Boolean.TRUE.equals(purchasedOnly)) {
            LambdaQueryWrapper<StudentExam> seW = new LambdaQueryWrapper<StudentExam>()
                    .eq(StudentExam::getStudentId, studentId)
                    .select(StudentExam::getExamId);
            studentExamMapper.selectList(seW).forEach(r -> purchasedIds.add(r.getExamId()));
        }

        // 批量统计每个考试的已开通人数
        Map<Long, Integer> examCountMap = new HashMap<>();
        if (!exams.isEmpty()) {
            Set<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<StudentExam> countWrapper = new LambdaQueryWrapper<StudentExam>()
                    .in(StudentExam::getExamId, examIds)
                    .select(StudentExam::getExamId);
            studentExamMapper.selectList(countWrapper).forEach(r -> examCountMap.merge(r.getExamId(), 1, Integer::sum));
        }
        final Set<Long> _purchased = Boolean.TRUE.equals(purchasedOnly)
                ? exams.stream().map(Exam::getId).collect(Collectors.toSet())
                : purchasedIds;

        List<ExamListItemVO> result = new ArrayList<>();
        for (Exam exam : exams) {
            ExamListItemVO vo = new ExamListItemVO();
            vo.setId(exam.getId());
            vo.setName(exam.getName());
            vo.setCategory(exam.getCategory());
            vo.setCoverUrl(exam.getCoverUrl());
            vo.setQuestionCount(exam.getQuestionCount());
            vo.setTotalScore(exam.getTotalScore());
            vo.setDuration(exam.getDuration());
            int baseExamCount = exam.getBaseExamCount() == null ? 0 : exam.getBaseExamCount();
            vo.setExamCount(baseExamCount + examCountMap.getOrDefault(exam.getId(), 0));
            vo.setPurchased(_purchased.contains(exam.getId()));
            vo.setProfessionId(exam.getProfessionId());
            if (exam.getProfessionId() == null) {
                vo.setProfessionName("通用考试");
            } else {
                vo.setProfessionName(professionNameMap.getOrDefault(exam.getProfessionId(), "通用考试"));
            }

            // 只有已登录且已开通时, 才查上次考试记录
            if (studentId != null && vo.getPurchased()) {
                try {
                    LambdaQueryWrapper<ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
                    recordWrapper.eq(ExamRecord::getStudentId, studentId)
                            .eq(ExamRecord::getExamId, exam.getId())
                            .select(ExamRecord::getScore, ExamRecord::getSubmitTime, ExamRecord::getCreateTime)
                            .orderByDesc(ExamRecord::getCreateTime)
                            .last("LIMIT 1");
                    ExamRecord lastRecord = examRecordMapper.selectOne(recordWrapper);
                    if (lastRecord != null) {
                        vo.setLastScore(lastRecord.getScore());
                        vo.setLastTime(lastRecord.getSubmitTime() != null ? lastRecord.getSubmitTime() : lastRecord.getCreateTime());
                    }
                } catch (Exception e) {
                    log.warn("查询考试记录失败: studentId={}, examId={}", studentId, exam.getId(), e);
                }
            }
            result.add(vo);
        }
        return new PageResult<>(examPage.getTotal(), examPage.getCurrent(), examPage.getSize(), result);
    }

    @Override
    public ExamIntroVO getExamIntro(Long examId) {
        Exam exam = getById(examId);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        ExamIntroVO vo = new ExamIntroVO();
        vo.setId(exam.getId());
        vo.setName(exam.getName());
        vo.setQuestionCount(exam.getQuestionCount());
        vo.setTotalScore(exam.getTotalScore());
        vo.setDuration(exam.getDuration());
        vo.setIntro(exam.getIntro());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setAllowRetry(exam.getAllowRetry());
        vo.setMaxAttempts(exam.getMaxAttempts());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamStartVO startExam(Long studentId, Long examId) {
        // 验证学生是否已开通该考试
        LambdaQueryWrapper<StudentExam> seWrapper = new LambdaQueryWrapper<>();
        seWrapper.eq(StudentExam::getStudentId, studentId)
                .eq(StudentExam::getExamId, examId);
        if (studentExamMapper.selectCount(seWrapper) == 0) {
            throw new BusinessException("您尚未开通该考试");
        }

        Exam exam = getById(examId);
        if (exam == null || exam.getStatus() != 1) {
            throw new BusinessException("考试不存在或未启用");
        }

        // 时间段校验:只有在时间段内才能考试
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BusinessException("考试尚未开始，开始时间：" + exam.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BusinessException("考试已结束，结束时间：" + exam.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        // 考试次数校验(maxAttempts=0或null表示不限)
        if (exam.getMaxAttempts() != null && exam.getMaxAttempts() > 0) {
            LambdaQueryWrapper<ExamRecord> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(ExamRecord::getStudentId, studentId)
                    .eq(ExamRecord::getExamId, examId)
                    .ne(ExamRecord::getSubmitStatus, 0); // 只统计已提交的
            long completed = examRecordMapper.selectCount(countWrapper);
            if (completed >= exam.getMaxAttempts()) {
                throw new BusinessException("已达到最大考试次数(" + exam.getMaxAttempts() + "次)");
            }
        }

        // 断点续考: 先查询是否存在未完成的记录(submitStatus=0)，存在则复用，不新建
        LambdaQueryWrapper<ExamRecord> unfinishedWrapper = new LambdaQueryWrapper<>();
        unfinishedWrapper.eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getSubmitStatus, 0)
                .orderByDesc(ExamRecord::getCreateTime)
                .last("LIMIT 1");
        ExamRecord record = examRecordMapper.selectOne(unfinishedWrapper);

        boolean isResumed = record != null;
        if (!isResumed) {
            // 不存在未完成记录，才创建新的 ExamRecord
            record = new ExamRecord();
            record.setStudentId(studentId);
            record.setExamId(examId);
            record.setScore(BigDecimal.ZERO);
            record.setCorrectCount(0);
            record.setWrongCount(0);
            record.setTotalCount(0);
            record.setPendingCount(0);
            record.setAccuracy(BigDecimal.ZERO);
            record.setDuration(0);
            record.setSubmitStatus(0);
            examRecordMapper.insert(record);
        }

        // 通过 exam.paperId 查询 PaperQuestion 获取题目（按sort排序）
        // 断点续考时回填已答的 studentAnswer
        Map<Long, String> answeredMap = new HashMap<>();
        if (isResumed) {
            LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
            answerWrapper.eq(ExamAnswer::getRecordId, record.getId());
            List<ExamAnswer> answers = examAnswerMapper.selectList(answerWrapper);
            for (ExamAnswer a : answers) {
                answeredMap.put(a.getQuestionId(), a.getStudentAnswer());
            }
        }
        List<QuestionVO> questionVOs = buildQuestionVOs(exam.getPaperId(), answeredMap);

        ExamStartVO vo = new ExamStartVO();
        vo.setRecordId(record.getId());
        vo.setExamName(exam.getName());
        vo.setTotalScore(exam.getTotalScore());
        vo.setDuration(exam.getDuration());
        vo.setQuestionCount(exam.getQuestionCount());
        vo.setStartTime(record.getCreateTime());
        vo.setQuestions(questionVOs);
        return vo;
    }

    @Override
    public ExamPaperVO getExamPaper(Long studentId, Long examId, Long recordId) {
        Exam exam = getById(examId);
        if (exam == null || exam.getStatus() != 1) {
            throw new BusinessException("考试不存在或未启用");
        }

        // 时间段校验:只有在时间段内才能考试
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BusinessException("考试尚未开始，开始时间：" + exam.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BusinessException("考试已结束，结束时间：" + exam.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        ExamRecord record = null;
        if (recordId != null) {
            // 已有 record: 校验归属,不允许看别人 record 的题
            record = examRecordMapper.selectById(recordId);
            if (record == null || !record.getStudentId().equals(studentId)) {
                throw new BusinessException("考试记录不存在");
            }
            if (record.getSubmitStatus() != null && record.getSubmitStatus() != 0) {
                throw new BusinessException("该试卷已提交，不能再次进入");
            }
        } else {
            // 无 record: 不允许直接在 paper 接口绕过 start
            // 但允许: 学生已开通考试,自动创建一个 record(与 start 等价)
            // 这里我们复用 startExam 的逻辑, 保持接口幂等
            ExamStartVO startVo = startExam(studentId, examId);
            record = examRecordMapper.selectById(startVo.getRecordId());
        }

        List<QuestionVO> questionVOs = buildQuestionVOs(exam.getPaperId(), getAnsweredMap(record.getId()));
        ExamPaperVO vo = new ExamPaperVO();
        vo.setRecordId(record.getId());
        vo.setExamId(examId);
        vo.setExamName(exam.getName());
        vo.setDuration(exam.getDuration());
        vo.setQuestionCount(questionVOs.size());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setQuestions(questionVOs);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswer(Long studentId, Long recordId, Long questionId, String studentAnswer) {
        // 1. 校验 recordId 归属当前 studentId，且 submitStatus=0（进行中）
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !record.getStudentId().equals(studentId)) {
            throw new BusinessException("考试记录不存在");
        }
        if (record.getSubmitStatus() != null && record.getSubmitStatus() != 0) {
            throw new BusinessException("该试卷已提交，不能保存答案");
        }

        // 2. 查询是否已存在该 (recordId, questionId) 的答案，存在则 update，不存在则 insert（upsert）
        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getQuestionId, questionId)
                .last("LIMIT 1");
        ExamAnswer existing = examAnswerMapper.selectOne(answerWrapper);

        if (existing != null) {
            ExamAnswer update = new ExamAnswer();
            update.setId(existing.getId());
            update.setStudentAnswer(studentAnswer);
            examAnswerMapper.updateById(update);
        } else {
            ExamAnswer answer = new ExamAnswer();
            answer.setRecordId(recordId);
            answer.setQuestionId(questionId);
            answer.setStudentAnswer(studentAnswer);
            // isCorrect 在交卷阅卷时再判定，保存阶段置 0；sort 取试卷中题目顺序，默认 0
            answer.setIsCorrect(0);
            answer.setSort(0);
            // 查询该题在试卷中的 sort
            Exam exam = getById(record.getExamId());
            if (exam != null && exam.getPaperId() != null) {
                LambdaQueryWrapper<PaperQuestion> pqWrapper = new LambdaQueryWrapper<>();
                pqWrapper.eq(PaperQuestion::getPaperId, exam.getPaperId())
                        .eq(PaperQuestion::getQuestionId, questionId)
                        .last("LIMIT 1");
                PaperQuestion pq = paperQuestionMapper.selectOne(pqWrapper);
                if (pq != null && pq.getSort() != null) {
                    answer.setSort(pq.getSort());
                }
            }
            examAnswerMapper.insert(answer);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamResultVO submitExam(Long studentId, SubmitExamDTO dto) {
        return doSubmit(studentId, dto, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamResultVO autoSubmitExam(Long studentId, SubmitExamDTO dto) {
        return doSubmit(studentId, dto, 2);
    }

    /**
     * 执行提交考试逻辑
     * @param submitStatus 1手动提交 2自动交卷
     */
    private ExamResultVO doSubmit(Long studentId, SubmitExamDTO dto, int submitStatus) {
        ExamRecord record = examRecordMapper.selectById(dto.getRecordId());
        if (record == null || !record.getStudentId().equals(studentId)) {
            throw new BusinessException("考试记录不存在");
        }
        if (record.getSubmitStatus() != null && record.getSubmitStatus() != 0) {
            throw new BusinessException("该试卷已提交，不能重复提交");
        }

        Long examId = record.getExamId();
        Exam exam = getById(examId);

        // 通过 exam.paperId 查询 PaperQuestion 获取题目（按sort排序）
        LambdaQueryWrapper<PaperQuestion> pqWrapper = new LambdaQueryWrapper<>();
        pqWrapper.eq(PaperQuestion::getPaperId, exam == null ? null : exam.getPaperId())
                .orderByAsc(PaperQuestion::getSort);
        List<PaperQuestion> paperQuestions = (exam == null || exam.getPaperId() == null)
                ? new ArrayList<>() : paperQuestionMapper.selectList(pqWrapper);

        // 构建学生答案映射 questionId -> studentAnswer
        Map<Long, String> answerMap = new HashMap<>();
        if (dto.getAnswers() != null) {
            for (AnswerDTO answer : dto.getAnswers()) {
                answerMap.put(answer.getQuestionId(), answer.getStudentAnswer());
            }
        }

        int correctCount = 0;
        int wrongCount = 0;
        int pendingCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        List<ExamAnswer> examAnswers = new ArrayList<>();
        List<WrongQuestion> wrongQuestions = new ArrayList<>();
        List<AnswerResultVO> answerResultVOs = new ArrayList<>();

        for (PaperQuestion pq : paperQuestions) {
            Long questionId = pq.getQuestionId();
            Integer sort = pq.getSort();

            Question question = questionMapper.selectById(questionId);
            if (question == null) {
                continue;
            }

            Integer type = question.getType();
            String studentAnswer = answerMap.getOrDefault(questionId, "");

            // 查询选项（按sort排序）—— 选择题/判断题需要
            LambdaQueryWrapper<QuestionOption> optWrapper = new LambdaQueryWrapper<>();
            optWrapper.eq(QuestionOption::getQuestionId, questionId)
                    .orderByAsc(QuestionOption::getSort);
            List<QuestionOption> options = questionOptionMapper.selectList(optWrapper);

            int isCorrect;
            String displayCorrectAnswer;

            if (type != null && type == 5) {
                // 简答题：无法自动阅卷，标记为待批改
                isCorrect = 2;
                pendingCount++;
                displayCorrectAnswer = null;
            } else if (type != null && type == 3) {
                // 填空题：将学生答案与 question.correctAnswer 比较
                // correctAnswer 可能是多个答案用逗号分隔（表示多个空），学生答案也用逗号分隔
                // 逐个比较（忽略大小写和首尾空格），全部匹配才算正确
                displayCorrectAnswer = question.getCorrectAnswer();
                isCorrect = gradeFillInQuestion(studentAnswer, question.getCorrectAnswer()) ? 1 : 0;
                if (isCorrect == 1) {
                    correctCount++;
                    totalScore = totalScore.add(question.getScore() == null ? BigDecimal.ZERO : question.getScore());
                } else {
                    wrongCount++;
                    WrongQuestion wq = new WrongQuestion();
                    wq.setStudentId(studentId);
                    wq.setQuestionId(questionId);
                    wq.setExamId(examId);
                    wq.setStudentAnswer(studentAnswer);
                    wrongQuestions.add(wq);
                }
            } else {
                // type=1（单选）/ type=2（多选）/ type=4（判断）：比较选项 label
                displayCorrectAnswer = getCorrectAnswer(options);
                boolean correct = normalizeAnswer(studentAnswer).equals(normalizeAnswer(displayCorrectAnswer));
                isCorrect = correct ? 1 : 0;
                if (correct) {
                    correctCount++;
                    totalScore = totalScore.add(question.getScore() == null ? BigDecimal.ZERO : question.getScore());
                } else {
                    wrongCount++;
                    WrongQuestion wq = new WrongQuestion();
                    wq.setStudentId(studentId);
                    wq.setQuestionId(questionId);
                    wq.setExamId(examId);
                    wq.setStudentAnswer(studentAnswer);
                    wrongQuestions.add(wq);
                }
            }

            // 保存答题记录
            ExamAnswer examAnswer = new ExamAnswer();
            examAnswer.setRecordId(record.getId());
            examAnswer.setQuestionId(questionId);
            examAnswer.setStudentAnswer(studentAnswer);
            examAnswer.setIsCorrect(isCorrect);
            examAnswer.setAnalysis(question.getAnalysis());
            examAnswer.setSort(sort);
            examAnswers.add(examAnswer);

            // 构建答题结果VO（含正确答案和解析）
            AnswerResultVO arVO = new AnswerResultVO();
            arVO.setQuestionId(questionId);
            arVO.setSort(sort);
            arVO.setIsCorrect(isCorrect);
            arVO.setStudentAnswer(studentAnswer);
            arVO.setCorrectAnswer(displayCorrectAnswer);
            arVO.setContent(question.getContent());
            arVO.setAnalysis(question.getAnalysis());
            // 填空题和简答题不返回选项列表
            if (type != null && (type == 3 || type == 5)) {
                arVO.setOptions(new ArrayList<>());
            } else {
                arVO.setOptions(options.stream().map(this::toOptionVO).collect(Collectors.toList()));
            }
            answerResultVOs.add(arVO);
        }

        int totalCount = paperQuestions.size();
        // 正确率基于自动阅卷的题目计算（排除简答题）
        int autoGradedCount = correctCount + wrongCount;
        BigDecimal accuracy = autoGradedCount > 0
                ? BigDecimal.valueOf(correctCount).multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(autoGradedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 更新考试记录
        ExamRecord update = new ExamRecord();
        update.setId(record.getId());
        update.setScore(totalScore);
        update.setCorrectCount(correctCount);
        update.setWrongCount(wrongCount);
        update.setTotalCount(totalCount);
        update.setPendingCount(pendingCount);
        update.setAccuracy(accuracy);
        update.setDuration(dto.getDuration() == null ? 0 : dto.getDuration());
        update.setSubmitStatus(submitStatus);
        // 回填冗余字段(删除考试后仍可展示)
        if (exam != null) {
            update.setExamName(exam.getName());
            update.setExamProfessionId(exam.getProfessionId());
            update.setExamCoverUrl(exam.getCoverUrl());
            update.setExamQuestionCount(exam.getQuestionCount());
            update.setExamTotalScore(exam.getTotalScore());
            update.setPaperId(exam.getPaperId());
        }
        // 如果学生有此专业的证书记录且颁发日期已设定，则交卷日期使用颁发日期
        LocalDateTime submitTime = LocalDateTime.now();
        Student student = studentMapper.selectById(studentId);
        if (student != null && StringUtils.hasText(student.getIdCard()) && exam != null && exam.getProfessionId() != null) {
            Profession profession = professionMapper.selectById(exam.getProfessionId());
            if (profession != null && StringUtils.hasText(profession.getName())) {
                Certificate cert = certificateMapper.selectOne(
                        new LambdaQueryWrapper<Certificate>()
                                .eq(Certificate::getIdCard, student.getIdCard())
                                .eq(Certificate::getProfession, profession.getName())
                                .isNotNull(Certificate::getIssueDate)
                                .orderByDesc(Certificate::getIssueDate)
                                .last("LIMIT 1"));
                if (cert != null && cert.getIssueDate() != null) {
                    submitTime = cert.getIssueDate().atStartOfDay()
                            .plusHours(ThreadLocalRandom.current().nextLong(8, 25))
                            .plusMinutes(ThreadLocalRandom.current().nextLong(60))
                            .plusSeconds(ThreadLocalRandom.current().nextLong(60));
                }
            }
        }
        update.setSubmitTime(submitTime);
        examRecordMapper.updateById(update);

        // 保存答题记录（断点续考：答案可能已由 saveAnswer 写入，执行 upsert 避免重复）
        Map<Long, ExamAnswer> existingAnswerMap = new HashMap<>();
        LambdaQueryWrapper<ExamAnswer> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(ExamAnswer::getRecordId, record.getId());
        List<ExamAnswer> existingAnswers = examAnswerMapper.selectList(existingWrapper);
        for (ExamAnswer ea : existingAnswers) {
            existingAnswerMap.put(ea.getQuestionId(), ea);
        }
        for (ExamAnswer examAnswer : examAnswers) {
            ExamAnswer existing = existingAnswerMap.get(examAnswer.getQuestionId());
            if (existing != null) {
                examAnswer.setId(existing.getId());
                examAnswerMapper.updateById(examAnswer);
            } else {
                examAnswerMapper.insert(examAnswer);
            }
        }
        // 批量保存错题（去重）
        for (WrongQuestion wq : wrongQuestions) {
            LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WrongQuestion::getStudentId, wq.getStudentId())
                    .eq(WrongQuestion::getQuestionId, wq.getQuestionId());
            if (wrongQuestionMapper.selectCount(wrapper) == 0) {
                wq.setCreateTime(LocalDateTime.now());
                try {
                    wrongQuestionMapper.insert(wq);
                } catch (Exception e) {
                    if (e.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException) {
                        // 并发场景下可能重复插入，忽略即可
                    } else {
                        throw e;
                    }
                }
            }
        }

        // 构建返回结果
        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setScore(totalScore);
        vo.setCorrectCount(correctCount);
        vo.setWrongCount(wrongCount);
        vo.setTotalCount(totalCount);
        vo.setPendingCount(pendingCount);
        vo.setAccuracy(accuracy);
        vo.setDuration(update.getDuration());
        // 从考试实体获取是否允许重新作答
        vo.setAllowRetry(exam == null ? null : exam.getAllowRetry());
        vo.setAnswers(answerResultVOs);

        // ============ 考完试后,回写理论成绩到证书用户(按专业匹配) ============
        syncTheoryScoreToCertificate(studentId, exam, totalScore);

        return vo;
    }

    /**
     * 考试交卷后,将理论成绩回写到匹配的证书用户记录
     * 匹配规则: 通过学生身份证号 + 考试专业ID 匹配证书
     * 回写字段: extra_json 中的 theoryScore(理论成绩) 和 comprehensiveEvaluation(综合成绩=理论成绩)
     */
    private void syncTheoryScoreToCertificate(Long studentId, Exam exam, BigDecimal examScore) {
        try {
            if (exam == null || studentId == null) return;
            Long examProfessionId = exam.getProfessionId();
            if (examProfessionId == null) return;
            // 优先用专业名称回写,兼容certificate表中profession存中文名称的情况
            Profession prof = professionMapper.selectById(examProfessionId);
            String examProfession = (prof != null && StringUtils.hasText(prof.getName()))
                    ? prof.getName() : examProfessionId.toString();
            Student student = studentMapper.selectById(studentId);
            if (student == null || !StringUtils.hasText(student.getIdCard())) return;
            String scoreStr = examScore.setScale(0, RoundingMode.HALF_UP).toPlainString();
            certificateService.syncTheoryScore(student.getIdCard(), examProfession, scoreStr);
        } catch (Exception e) {
            // 回写失败不影响考试提交主流程
        }
    }

    @Override
    public ExamResultVO getExamResult(Long studentId, Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !record.getStudentId().equals(studentId)) {
            throw new BusinessException("考试记录不存在");
        }

        // 查询答题记录（按sort排序）
        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.eq(ExamAnswer::getRecordId, recordId)
                .orderByAsc(ExamAnswer::getSort);
        List<ExamAnswer> examAnswers = examAnswerMapper.selectList(answerWrapper);

        List<AnswerResultVO> answerResultVOs = new ArrayList<>();
        for (ExamAnswer examAnswer : examAnswers) {
            Question question = questionMapper.selectById(examAnswer.getQuestionId());
            if (question == null) {
                continue;
            }
            Integer type = question.getType();
            // 填空题和简答题不返回选项列表
            List<QuestionOption> options;
            if (type != null && (type == 3 || type == 5)) {
                options = new ArrayList<>();
            } else {
                LambdaQueryWrapper<QuestionOption> optWrapper = new LambdaQueryWrapper<>();
                optWrapper.eq(QuestionOption::getQuestionId, examAnswer.getQuestionId())
                        .orderByAsc(QuestionOption::getSort);
                options = questionOptionMapper.selectList(optWrapper);
            }

            AnswerResultVO vo = new AnswerResultVO();
            vo.setQuestionId(examAnswer.getQuestionId());
            vo.setSort(examAnswer.getSort());
            vo.setIsCorrect(examAnswer.getIsCorrect());
            vo.setScore(examAnswer.getScore());
            vo.setStudentAnswer(examAnswer.getStudentAnswer());
            // 填空题的正确答案取自 question.correctAnswer 字段；其他题型取选项 label
            if (type != null && type == 3) {
                vo.setCorrectAnswer(question.getCorrectAnswer());
            } else if (type != null && type == 5) {
                vo.setCorrectAnswer(null);
            } else {
                vo.setCorrectAnswer(getCorrectAnswer(options));
            }
            vo.setContent(question.getContent());
            vo.setAnalysis(question.getAnalysis());
            vo.setOptions(options.stream().map(this::toOptionVO).collect(Collectors.toList()));
            answerResultVOs.add(vo);
        }

        ExamResultVO vo = new ExamResultVO();
        vo.setRecordId(record.getId());
        vo.setScore(record.getScore());
        vo.setCorrectCount(record.getCorrectCount());
        vo.setWrongCount(record.getWrongCount());
        vo.setTotalCount(record.getTotalCount());
        vo.setPendingCount(record.getPendingCount());
        vo.setAccuracy(record.getAccuracy());
        vo.setDuration(record.getDuration());
        vo.setSubmitTime(record.getSubmitTime());
        // 从考试实体获取是否允许重新作答、总分、及格分
        Exam exam = getById(record.getExamId());
        vo.setExamName(exam == null ? null : exam.getName());
        // 查学生姓名
        Student student = studentMapper.selectById(studentId);
        vo.setStudentName(student == null ? null : (student.getNickname() != null ? student.getNickname() : student.getPhone()));
        vo.setAllowRetry(exam == null ? null : exam.getAllowRetry());
        vo.setTotalScore(exam == null ? null : exam.getTotalScore());
        vo.setPassScore(exam == null ? null : BigDecimal.valueOf(60));
        vo.setAnswers(answerResultVOs);
        return vo;
    }

    @Override
    public List<ExamRecordVO> getExamRecords(Long studentId) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, studentId)
                .orderByDesc(ExamRecord::getCreateTime);
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);

        List<ExamRecordVO> result = new ArrayList<>();
        for (ExamRecord record : records) {
            ExamRecordVO vo = new ExamRecordVO();
            vo.setId(record.getId());
            vo.setExamId(record.getExamId());
            vo.setScore(record.getScore());
            vo.setDuration(record.getDuration());
            vo.setSubmitStatus(record.getSubmitStatus());
            vo.setSubmitTime(record.getSubmitTime());

            // 查询考试名称
            Exam exam = getById(record.getExamId());
            if (exam != null) {
                vo.setExamName(exam.getName());
            } else {
                // 考试已删除，使用冗余字段
                vo.setExamName(record.getExamName() != null ? record.getExamName() : "已删除考试");
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public Map<String, Object> getExamRecordsPage(Long studentId, Integer page, Integer pageSize) {
        // 参数默认值
        int pageNum = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 50 : Math.min(pageSize, 50);

        // 统计概览基于该学生全部考试记录(不分页)
        LambdaQueryWrapper<ExamRecord> statWrapper = new LambdaQueryWrapper<>();
        statWrapper.eq(ExamRecord::getStudentId, studentId)
                .ne(ExamRecord::getSubmitStatus, 0); // 只统计已提交的
        List<ExamRecord> allRecords = examRecordMapper.selectList(statWrapper);

        int totalAll = allRecords.size();
        BigDecimal avgScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        int passCount = 0;
        if (totalAll > 0) {
            BigDecimal sumScore = BigDecimal.ZERO;
            for (ExamRecord r : allRecords) {
                BigDecimal score = r.getScore() == null ? BigDecimal.ZERO : r.getScore();
                sumScore = sumScore.add(score);
                if (score.compareTo(maxScore) > 0) {
                    maxScore = score;
                }
                if (score.compareTo(new BigDecimal("60")) >= 0) {
                    passCount++;
                }
            }
            avgScore = sumScore.divide(BigDecimal.valueOf(totalAll), 2, RoundingMode.HALF_UP);
        }
        BigDecimal passRate = totalAll > 0
                ? BigDecimal.valueOf(passCount).multiply(new BigDecimal("100"))
                        .divide(BigDecimal.valueOf(totalAll), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 分页查询考试记录(包含未提交的记录)
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, studentId)
                .orderByDesc(ExamRecord::getCreateTime);
        Page<ExamRecord> pageParam = new Page<>(pageNum, size);
        IPage<ExamRecord> recordPage = examRecordMapper.selectPage(pageParam, wrapper);
        List<ExamRecord> records = recordPage.getRecords();

        List<ExamRecordVO> result = new ArrayList<>();
        for (ExamRecord record : records) {
            ExamRecordVO vo = new ExamRecordVO();
            vo.setId(record.getId());
            vo.setExamId(record.getExamId());
            vo.setScore(record.getScore());
            vo.setDuration(record.getDuration());
            vo.setSubmitStatus(record.getSubmitStatus());
            vo.setSubmitTime(record.getSubmitTime());

            Exam exam = getById(record.getExamId());
            if (exam != null) {
                vo.setExamName(exam.getName());
            } else {
                vo.setExamName(record.getExamName() != null ? record.getExamName() : "已删除考试");
            }
            result.add(vo);
        }

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("total", recordPage.getTotal());
        resultMap.put("page", recordPage.getCurrent());
        resultMap.put("size", recordPage.getSize());
        resultMap.put("records", result);
        resultMap.put("totalCount", totalAll);
        resultMap.put("avgScore", avgScore);
        resultMap.put("maxScore", maxScore);
        resultMap.put("passRate", passRate);
        return resultMap;
    }

    /**
     * 构建试卷题目VO列表（不含正确答案），题目来源为 PaperQuestion
     */
    private List<QuestionVO> buildQuestionVOs(Long paperId) {
        return buildQuestionVOs(paperId, new HashMap<>());
    }

    /**
     * 构建试卷题目VO列表（不含正确答案），题目来源为 PaperQuestion
     *
     * @param answeredMap 断点续考时已答答案映射 questionId -> studentAnswer，可为空 map
     */
    private List<QuestionVO> buildQuestionVOs(Long paperId, Map<Long, String> answeredMap) {
        List<QuestionVO> questionVOs = new ArrayList<>();
        if (paperId == null) {
            return questionVOs;
        }
        LambdaQueryWrapper<PaperQuestion> pqWrapper = new LambdaQueryWrapper<>();
        pqWrapper.eq(PaperQuestion::getPaperId, paperId)
                .orderByAsc(PaperQuestion::getSort);
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);

        for (PaperQuestion pq : paperQuestions) {
            Question question = questionMapper.selectById(pq.getQuestionId());
            if (question == null) {
                continue;
            }
            QuestionVO qvo = new QuestionVO();
            qvo.setId(question.getId());
            qvo.setType(question.getType());
            qvo.setContent(question.getContent());
            qvo.setScore(question.getScore());
            // 回填断点续考的已答答案
            if (answeredMap != null) {
                qvo.setUserAnswer(answeredMap.get(question.getId()));
            }
            // 填空题和简答题不返回选项列表（返回空列表）
            Integer type = question.getType();
            if (type != null && (type == 3 || type == 5)) {
                qvo.setOptions(new ArrayList<>());
            } else {
                LambdaQueryWrapper<QuestionOption> optWrapper = new LambdaQueryWrapper<>();
                optWrapper.eq(QuestionOption::getQuestionId, question.getId())
                        .orderByAsc(QuestionOption::getSort);
                List<QuestionOption> options = questionOptionMapper.selectList(optWrapper);
                qvo.setOptions(options.stream().map(this::toOptionVO).collect(Collectors.toList()));
            }
            questionVOs.add(qvo);
        }
        return questionVOs;
    }

    /**
     * 查询某考试记录已答答案映射 questionId -> studentAnswer（断点续考回填用）
     */
    private Map<Long, String> getAnsweredMap(Long recordId) {
        Map<Long, String> answeredMap = new HashMap<>();
        if (recordId == null) {
            return answeredMap;
        }
        LambdaQueryWrapper<ExamAnswer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.eq(ExamAnswer::getRecordId, recordId);
        List<ExamAnswer> answers = examAnswerMapper.selectList(answerWrapper);
        for (ExamAnswer a : answers) {
            answeredMap.put(a.getQuestionId(), a.getStudentAnswer());
        }
        return answeredMap;
    }

    /**
     * 获取正确答案（选项label逗号分隔）
     */
    private String getCorrectAnswer(List<QuestionOption> options) {
        return options.stream()
                .filter(o -> o.getIsCorrect() != null && o.getIsCorrect() == 1)
                .map(QuestionOption::getLabel)
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * 规范化答案用于比较（按字母排序，逗号分隔）
     */
    private String normalizeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "";
        }
        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * 填空题阅卷：
     * correctAnswer 可能是多个答案用逗号分隔（表示多个空），学生答案也用逗号分隔。
     * 逐个比较（忽略大小写和首尾空格），全部匹配才算正确。
     */
    private boolean gradeFillInQuestion(String studentAnswer, String correctAnswer) {
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return false;
        }
        String[] correctParts = correctAnswer.split(",");
        String[] studentParts = studentAnswer == null ? new String[0] : studentAnswer.split(",");
        if (correctParts.length != studentParts.length) {
            return false;
        }
        for (int i = 0; i < correctParts.length; i++) {
            String c = correctParts[i] == null ? "" : correctParts[i].trim();
            String s = studentParts[i] == null ? "" : studentParts[i].trim();
            if (!c.equalsIgnoreCase(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将选项实体转换为OptionVO
     */
    private OptionVO toOptionVO(QuestionOption option) {
        OptionVO vo = new OptionVO();
        vo.setId(option.getId());
        vo.setLabel(option.getLabel());
        vo.setContent(option.getContent());
        return vo;
    }

    @Override
    public boolean checkExamAccess(Long examId, Long studentId) {
        if (studentId == null) {
            throw new BusinessException(1001, "请先登录后再访问考试");
        }
        Exam exam = getById(examId);
        if (exam == null || exam.getStatus() != 1) {
            throw new BusinessException("考试不存在或未启用");
        }
        LambdaQueryWrapper<StudentExam> seWrapper = new LambdaQueryWrapper<>();
        seWrapper.eq(StudentExam::getStudentId, studentId)
                .eq(StudentExam::getExamId, examId);
        if (studentExamMapper.selectCount(seWrapper) == 0) {
            throw new BusinessException(1002, "您尚未开通该考试,请联系管理员先开通");
        }
        return true;
    }

    @Override
    public com.exam.vo.PaperViewVO viewExamPaper(Long studentId, Long examId) {
        Exam exam = getById(examId);
        if (studentId == null) {
            throw new BusinessException(1001, "请先登录");
        }
        // 必须已开通(考试已删除时通过exam_record判断)
        if (exam != null) {
            LambdaQueryWrapper<StudentExam> seWrapper = new LambdaQueryWrapper<>();
            seWrapper.eq(StudentExam::getStudentId, studentId)
                    .eq(StudentExam::getExamId, examId);
            if (studentExamMapper.selectCount(seWrapper) == 0) {
                throw new BusinessException(1002, "您尚未开通该考试");
            }
        } else {
            // 考试已删除,通过exam_record确认学生考过该考试
            long recordCount = examRecordMapper.selectCount(
                    new LambdaQueryWrapper<ExamRecord>()
                            .eq(ExamRecord::getExamId, examId)
                            .eq(ExamRecord::getStudentId, studentId));
            if (recordCount == 0) {
                throw new BusinessException(1002, "您尚未开通该考试");
            }
        }

        // 取考试名称(删除的考试从 exam_record 冗余字段获取)
        String examName;
        if (exam != null) {
            examName = exam.getName();
        } else {
            ExamRecord record = examRecordMapper.selectOne(
                    new LambdaQueryWrapper<ExamRecord>()
                            .eq(ExamRecord::getExamId, examId)
                            .eq(ExamRecord::getStudentId, studentId)
                            .last("LIMIT 1"));
            examName = record != null && record.getExamName() != null ? record.getExamName() : "已删除考试";
        }

        com.exam.vo.PaperViewVO view = new com.exam.vo.PaperViewVO();
        view.setExamId(examId);
        view.setExamName(examName);

        // 题目列表
        List<com.exam.vo.QuestionViewVO> questions = new ArrayList<>();
        // 优先从考试实体获取paperId,删除的考试从exam_record冗余字段获取
        Long paperId = exam != null ? exam.getPaperId() : null;
        if (paperId == null) {
            // 从考试记录获取冗余的paperId
            ExamRecord record = examRecordMapper.selectOne(
                    new LambdaQueryWrapper<ExamRecord>()
                            .eq(ExamRecord::getExamId, examId)
                            .eq(ExamRecord::getStudentId, studentId)
                            .last("LIMIT 1"));
            if (record != null) {
                paperId = record.getPaperId();
            }
        }
        if (paperId != null) {
            LambdaQueryWrapper<PaperQuestion> pqWrapper = new LambdaQueryWrapper<>();
            pqWrapper.eq(PaperQuestion::getPaperId, paperId)
                    .orderByAsc(PaperQuestion::getSort);
            List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(pqWrapper);
            for (PaperQuestion pq : paperQuestions) {
                Question question = questionMapper.selectById(pq.getQuestionId());
                if (question == null) continue;
                com.exam.vo.QuestionViewVO qvo = new com.exam.vo.QuestionViewVO();
                qvo.setId(question.getId());
                qvo.setType(question.getType());
                qvo.setContent(question.getContent());
                qvo.setScore(question.getScore());
                qvo.setAnalysis(question.getAnalysis());
                Integer type = question.getType();
                if (type != null && (type == 3 || type == 5)) {
                    // 填空/简答: 参考答案为 question.correctAnswer
                    qvo.setCorrectAnswer(question.getCorrectAnswer());
                    qvo.setOptions(new ArrayList<>());
                } else {
                    // 选择题/判断题: 选项含 isCorrect 标记,correctAnswer 为 label 串
                    LambdaQueryWrapper<QuestionOption> optWrapper = new LambdaQueryWrapper<>();
                    optWrapper.eq(QuestionOption::getQuestionId, question.getId())
                            .orderByAsc(QuestionOption::getSort);
                    List<QuestionOption> options = questionOptionMapper.selectList(optWrapper);
                    List<com.exam.vo.OptionViewVO> optVOs = new ArrayList<>();
                    StringBuilder correctLabel = new StringBuilder();
                    for (QuestionOption opt : options) {
                        com.exam.vo.OptionViewVO ovo = new com.exam.vo.OptionViewVO();
                        ovo.setId(opt.getId());
                        ovo.setLabel(opt.getLabel());
                        ovo.setContent(opt.getContent());
                        boolean isCorrect = opt.getIsCorrect() != null && opt.getIsCorrect() == 1;
                        ovo.setIsCorrect(isCorrect);
                        if (isCorrect) {
                            if (correctLabel.length() > 0) correctLabel.append(',');
                            correctLabel.append(opt.getLabel());
                        }
                        optVOs.add(ovo);
                    }
                    qvo.setOptions(optVOs);
                    qvo.setCorrectAnswer(correctLabel.toString());
                }
                questions.add(qvo);
            }
        }
        view.setQuestions(questions);

        // 用户最近一次作答
        Map<Long, String> answerMap = new HashMap<>();
        LambdaQueryWrapper<ExamRecord> recWrapper = new LambdaQueryWrapper<>();
        recWrapper.eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getExamId, examId)
                .orderByDesc(ExamRecord::getCreateTime)
                .last("LIMIT 1");
        ExamRecord lastRecord = examRecordMapper.selectOne(recWrapper);
        if (lastRecord != null) {
            LambdaQueryWrapper<ExamAnswer> ansWrapper = new LambdaQueryWrapper<>();
            ansWrapper.eq(ExamAnswer::getRecordId, lastRecord.getId());
            List<ExamAnswer> answers = examAnswerMapper.selectList(ansWrapper);
            for (ExamAnswer a : answers) {
                answerMap.put(a.getQuestionId(), a.getStudentAnswer());
            }
        }
        view.setAnswers(answerMap);
        return view;
    }

    @Override
    public List<Map<String, Object>> getBestRecordsByProfession(Long studentId) {
        // 查所有已提交的考试记录
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getStudentId, studentId)
               .ne(ExamRecord::getSubmitStatus, 0)
               .orderByDesc(ExamRecord::getScore);
        List<ExamRecord> records = examRecordMapper.selectList(wrapper);
        // 按 examId 分组取最高分
        Map<Long, ExamRecord> bestByExam = new LinkedHashMap<>();
        for (ExamRecord r : records) {
            ExamRecord existing = bestByExam.get(r.getExamId());
            if (existing == null || (r.getScore() != null && existing.getScore() != null && r.getScore().compareTo(existing.getScore()) > 0)) {
                bestByExam.put(r.getExamId(), r);
            }
        }
        // 查考试信息获取 professionId(过滤null避免selectBatchIds抛异常)
        List<Long> examIds = bestByExam.keySet().stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Exam> examMap = examIds.isEmpty() ? new HashMap<>() :
            examMapper.selectBatchIds(examIds).stream().collect(Collectors.toMap(Exam::getId, e -> e));
        // 批量查询专业名称(exam.professionName 是 @TableField(exist=false), 需手动填充)
        Set<Long> professionIds = examMap.values().stream()
                .map(Exam::getProfessionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> professionNameMap = new HashMap<>();
        if (!professionIds.isEmpty()) {
            List<Profession> professions = professionMapper.selectBatchIds(professionIds);
            for (Profession p : professions) {
                professionNameMap.put(p.getId(), p.getName());
            }
        }
        // 按 professionId 分组取最高分
        Map<Long, Map<String, Object>> bestByProfession = new LinkedHashMap<>();
        for (ExamRecord r : bestByExam.values()) {
            Exam exam = examMap.get(r.getExamId());
            Long profId = exam != null ? exam.getProfessionId() : (r.getExamProfessionId() != null ? r.getExamProfessionId() : null);
            Map<String, Object> existing = bestByProfession.get(profId);
            BigDecimal existingScore = existing != null ? (BigDecimal) existing.get("score") : null;
            if (existing == null || (r.getScore() != null && existingScore != null && r.getScore().compareTo(existingScore) > 0)) {
                Map<String, Object> item = new HashMap<>();
                item.put("recordId", r.getId());
                item.put("examId", r.getExamId());
                item.put("examName", exam != null ? exam.getName() : (r.getExamName() != null ? r.getExamName() : "已删除考试"));
                item.put("score", r.getScore());
                item.put("professionId", profId);
                item.put("professionName", profId == null ? "通用考试" : professionNameMap.getOrDefault(profId, "通用考试"));
                item.put("submitTime", r.getSubmitTime());
                item.put("duration", r.getDuration());
                // 封面图优先使用考试实体，否则使用冗余字段
                item.put("coverUrl", exam != null ? exam.getCoverUrl() : (r.getExamCoverUrl() != null ? r.getExamCoverUrl() : ""));
                bestByProfession.put(profId, item);
            }
        }
        return new ArrayList<>(bestByProfession.values());
    }
}
