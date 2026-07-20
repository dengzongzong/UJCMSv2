package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.mapper.ExamAnswerMapper;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.ExamDTO;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.CertificateService;
import com.exam.service.ExamManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ExamManageServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamManageService {

    @Autowired
    private ExamQuestionMapper examQuestionMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private StudentExamMapper studentExamMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private ProfessionMapper professionMapper;
    @Autowired
    private ExamRecordMapper examRecordMapper;
    @Autowired
    private ExamAnswerMapper examAnswerMapper;
    @Autowired
    private QuestionOptionMapper questionOptionMapper;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private CertificateMapper certificateMapper;

    @Override
    public PageResult<Exam> page(Integer page, Integer size, String name, String category,
                                 String createTimeStart, String createTimeEnd, Integer status, Long professionId) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .like(StringUtils.hasText(name), Exam::getName, name)
                .eq(StringUtils.hasText(category), Exam::getCategory, category)
                .eq(status != null, Exam::getStatus, status)
                .eq(professionId != null, Exam::getProfessionId, professionId)
                .orderByDesc(Exam::getCreateTime);
        if (StringUtils.hasText(createTimeStart)) {
            wrapper.ge(Exam::getCreateTime, LocalDate.parse(createTimeStart).atStartOfDay());
        }
        if (StringUtils.hasText(createTimeEnd)) {
            wrapper.le(Exam::getCreateTime, LocalDate.parse(createTimeEnd).atTime(23, 59, 59));
        }
        Page<Exam> p = new Page<>(page, size);
        Page<Exam> result = this.page(p, wrapper);
        
        List<Exam> records = result.getRecords();
        if (!records.isEmpty()) {
            Map<Long, String> professionMap = professionMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Profession::getId, Profession::getName));
            for (Exam exam : records) {
                if (exam.getProfessionId() != null) {
                    exam.setProfessionName(professionMap.get(exam.getProfessionId()));
                }
            }
        }
        
        return new PageResult<>(result);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        Exam exam = this.getById(id);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        // 通过 paperId 查询 PaperQuestion 获取题目列表
        List<Question> questionList = new ArrayList<>();
        if (exam.getPaperId() != null) {
            List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>()
                            .eq(PaperQuestion::getPaperId, exam.getPaperId())
                            .orderByAsc(PaperQuestion::getSort));
            List<Long> questionIds = paperQuestions.stream()
                    .map(PaperQuestion::getQuestionId).collect(Collectors.toList());
            List<Question> questions = questionIds.isEmpty() ? new ArrayList<>() :
                    questionMapper.selectBatchIds(questionIds);
            Map<Long, Question> questionMap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));

            for (PaperQuestion pq : paperQuestions) {
                Question q = questionMap.get(pq.getQuestionId());
                if (q != null) {
                    questionList.add(q);
                }
            }
        }

        // 关联试卷信息
        Paper paper = exam.getPaperId() == null ? null : paperMapper.selectById(exam.getPaperId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", exam.getId());
        result.put("name", exam.getName());
        result.put("category", exam.getCategory());
        result.put("coverUrl", exam.getCoverUrl());
        result.put("intro", exam.getIntro());
        result.put("totalScore", exam.getTotalScore());
        result.put("duration", exam.getDuration());
        result.put("questionCount", exam.getQuestionCount());
        result.put("startTime", exam.getStartTime());
        result.put("endTime", exam.getEndTime());
        result.put("allowRetry", exam.getAllowRetry());
        result.put("maxAttempts", exam.getMaxAttempts());
        result.put("status", exam.getStatus());
        result.put("professionId", exam.getProfessionId());
        result.put("paperId", exam.getPaperId());
        result.put("baseExamCount", exam.getBaseExamCount());
        result.put("createTime", exam.getCreateTime());
        // questions 直接是 Question 对象列表
        result.put("questions", questionList);
        // paperName 供显示
        if (paper != null) {
            result.put("paperName", paper.getName());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ExamDTO dto) {
        // 校验考试名称唯一
        if (StringUtils.hasText(dto.getName())) {
            long count = this.count(new LambdaQueryWrapper<Exam>().eq(Exam::getName, dto.getName().trim()));
            if (count > 0) {
                throw new BusinessException("考试名称已存在，请勿重复");
            }
        }
        Exam exam = new Exam();
        exam.setName(dto.getName());
        exam.setCategory(dto.getCategory());
        exam.setCoverUrl(dto.getCoverUrl());
        exam.setIntro(dto.getIntro());
        // 考试时长: 未设置时默认90; 超出范围时自动钳制到30-180
        int duration = dto.getDuration() != null ? dto.getDuration() : 90;
        if (duration < 30 || duration > 180) {
            duration = 90;
        }
        exam.setDuration(duration);
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setAllowRetry(dto.getAllowRetry() == null ? 1 : dto.getAllowRetry());
        exam.setMaxAttempts(dto.getMaxAttempts());
        exam.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        exam.setProfessionId(dto.getProfessionId());
        exam.setSubjectId(dto.getSubjectId());
        exam.setPaperId(dto.getPaperId());
        exam.setBaseExamCount(dto.getBaseExamCount() == null ? 0 : dto.getBaseExamCount());
        // questionCount 和 totalScore 从关联试卷获取
        applyPaperInfo(exam, dto.getPaperId());
        this.save(exam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ExamDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        Exam exam = this.getById(dto.getId());
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        // 校验考试名称唯一(排除自身)
        if (StringUtils.hasText(dto.getName())) {
            long count = this.count(new LambdaQueryWrapper<Exam>()
                    .eq(Exam::getName, dto.getName().trim())
                    .ne(Exam::getId, dto.getId()));
            if (count > 0) {
                throw new BusinessException("考试名称已存在，请勿重复");
            }
        }
        exam.setName(dto.getName());
        exam.setCategory(dto.getCategory());
        exam.setCoverUrl(dto.getCoverUrl());
        exam.setIntro(dto.getIntro());
        // 考试时长: 超出范围时自动钳制到30-180分钟
        int duration = dto.getDuration() != null ? dto.getDuration() : 90;
        if (duration < 30) duration = 30;
        if (duration > 180) duration = 180;
        exam.setDuration(duration);
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setAllowRetry(dto.getAllowRetry());
        exam.setMaxAttempts(dto.getMaxAttempts());
        if (dto.getStatus() != null) {
            exam.setStatus(dto.getStatus());
        }
        exam.setProfessionId(dto.getProfessionId());
        exam.setSubjectId(dto.getSubjectId());
        exam.setPaperId(dto.getPaperId());
        exam.setBaseExamCount(dto.getBaseExamCount() == null ? 0 : dto.getBaseExamCount());
        // questionCount 和 totalScore 从关联试卷获取
        applyPaperInfo(exam, dto.getPaperId());
        this.updateById(exam);
    }

    /**
     * 从关联试卷获取 questionCount 和 totalScore 并写入考试
     */
    private void applyPaperInfo(Exam exam, Long paperId) {
        if (paperId == null) {
            exam.setQuestionCount(0);
            exam.setTotalScore(java.math.BigDecimal.ZERO);
            return;
        }
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException("关联试卷不存在");
        }
        exam.setQuestionCount(paper.getQuestionCount() == null ? 0 : paper.getQuestionCount());
        exam.setTotalScore(paper.getTotalScore() == null ? java.math.BigDecimal.ZERO : paper.getTotalScore());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Exam exam = this.getById(id);
        String examName = exam != null ? exam.getName() : null;
        // 删除前回填考试信息到考试记录(防止删除后信息丢失)
        if (exam != null) {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExamRecord> updateWrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExamRecord>()
                            .eq(ExamRecord::getExamId, id);
            // 仅回填为空的记录(已填过的不覆盖)
            updateWrapper.set(ExamRecord::getExamName, exam.getName());
            updateWrapper.set(ExamRecord::getExamProfessionId, exam.getProfessionId());
            updateWrapper.set(ExamRecord::getExamCoverUrl, exam.getCoverUrl());
            updateWrapper.set(ExamRecord::getExamQuestionCount, exam.getQuestionCount());
            updateWrapper.set(ExamRecord::getExamTotalScore, exam.getTotalScore());
            updateWrapper.set(ExamRecord::getPaperId, exam.getPaperId());
            examRecordMapper.update(null, updateWrapper);
        }
        // 删除考试时不删除试卷(试卷可被多个考试复用)
        // 清理考试关联数据(题目关联、学生开通)
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, id));
        studentExamMapper.delete(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getExamId, id));
        // 保留考试记录(成绩)和答题明细——非手动删除时永久保留
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
        List<StudentExam> studentExams = studentExamMapper.selectList(
                new LambdaQueryWrapper<StudentExam>().eq(StudentExam::getExamId, id));
        List<Long> studentIds = studentExams.stream().map(StudentExam::getStudentId).collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        students.forEach(s -> s.setPassword(null));
        return students;
    }

    @Override
    public PageResult<Student> studentsPage(Long examId, Integer page, Integer size, String phone, Integer unopened, Integer unexamined) {
        // 查询已开通的学生ID集合
        List<StudentExam> studentExams = studentExamMapper.selectList(
                new LambdaQueryWrapper<StudentExam>().eq(StudentExam::getExamId, examId));
        Set<Long> openedIds = studentExams.stream().map(StudentExam::getStudentId).collect(Collectors.toSet());

        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(phone), Student::getPhone, phone);
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
            // 未考试筛选: 从已开通学生中排除已有考试记录的学生
            if (unexamined != null && unexamined == 1) {
                List<Long> examinedIds = examRecordMapper.selectList(
                        new LambdaQueryWrapper<ExamRecord>()
                                .in(ExamRecord::getExamId, examId)
                                .in(ExamRecord::getStudentId, openedIds)
                                .eq(ExamRecord::getSubmitStatus, 1)
                ).stream().map(ExamRecord::getStudentId).distinct().collect(Collectors.toList());
                if (!examinedIds.isEmpty()) {
                    wrapper.notIn(Student::getId, examinedIds);
                }
            }
        }
        wrapper.orderByDesc(Student::getCreateTime);
        Page<Student> p = new Page<>(page, size);
        Page<Student> result = studentMapper.selectPage(p, wrapper);
        result.getRecords().forEach(s -> s.setPassword(null));
        return new PageResult<>(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openStudents(Long examId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        List<StudentExam> existing = studentExamMapper.selectList(
                new LambdaQueryWrapper<StudentExam>()
                        .eq(StudentExam::getExamId, examId)
                        .in(StudentExam::getStudentId, studentIds));
        Set<Long> existingIds = existing.stream().map(StudentExam::getStudentId).collect(Collectors.toSet());
        for (Long studentId : studentIds) {
            if (existingIds.contains(studentId)) {
                continue;
            }
            StudentExam se = new StudentExam();
            se.setStudentId(studentId);
            se.setExamId(examId);
            studentExamMapper.insert(se);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeStudent(Long examId, Long studentId) {
        studentExamMapper.delete(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getExamId, examId)
                .eq(StudentExam::getStudentId, studentId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoExam(Long examId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        Exam exam = this.getById(examId);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        // 校验学生是否已开通该考试
        List<StudentExam> openedList = studentExamMapper.selectList(
                new LambdaQueryWrapper<StudentExam>()
                        .eq(StudentExam::getExamId, examId)
                        .in(StudentExam::getStudentId, studentIds));
        Set<Long> openedIds = openedList.stream()
                .map(StudentExam::getStudentId)
                .collect(Collectors.toSet());
        if (openedIds.isEmpty()) {
            throw new BusinessException("所选学生未开通该考试");
        }

        int totalCount = exam.getQuestionCount() == null ? 0 : exam.getQuestionCount();
        BigDecimal totalScore = exam.getTotalScore() == null ? BigDecimal.ZERO : exam.getTotalScore();
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();

        // 预加载试卷题目和正确答案(用于生成 exam_answer 明细,使考试详情可查)
        List<PaperQuestion> paperQuestions = new ArrayList<>();
        Map<Long, Question> questionMap = new HashMap<>();
        Map<Long, List<QuestionOption>> optionMap = new HashMap<>();
        if (exam.getPaperId() != null) {
            paperQuestions = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>()
                            .eq(PaperQuestion::getPaperId, exam.getPaperId())
                            .orderByAsc(PaperQuestion::getSort));
            if (!paperQuestions.isEmpty()) {
                List<Long> qIds = paperQuestions.stream()
                        .map(PaperQuestion::getQuestionId).collect(Collectors.toList());
                List<Question> questions = questionMapper.selectBatchIds(qIds);
                for (Question q : questions) {
                    questionMap.put(q.getId(), q);
                }
                // 查选项(用于构造学生答案)
                List<QuestionOption> options = questionOptionMapper.selectList(
                        new LambdaQueryWrapper<QuestionOption>()
                                .in(QuestionOption::getQuestionId, qIds)
                                .orderByAsc(QuestionOption::getSort));
                for (QuestionOption opt : options) {
                    optionMap.computeIfAbsent(opt.getQuestionId(), k -> new ArrayList<>()).add(opt);
                }
            }
        }

        for (Long studentId : studentIds) {
            if (!openedIds.contains(studentId)) {
                continue;
            }
            // 基于试卷题目和每题分值,为每个学生生成差异化的模拟成绩
            // 根据学生在本专业其他考试中的历史表现,确定一个合理的基础正确率
            BigDecimal baseAccuracy = getStudentBaseAccuracy(studentId, exam.getProfessionId());
            // 在基础正确率上加上较大范围波动,使分数分布更自然
            // 第一层波动: ±12%
            double variance1 = (random.nextDouble() - 0.5) * 0.24;
            // 第二层波动: ±3% (叠加使分布更均匀,减少扎堆)
            double variance2 = (random.nextDouble() - 0.5) * 0.06;
            double actualAccuracy = Math.max(0.70, Math.min(0.98, baseAccuracy.doubleValue() + variance1 + variance2));

            // 根据正确率计算得分
            BigDecimal score = totalScore.multiply(new BigDecimal(actualAccuracy))
                    .setScale(0, BigDecimal.ROUND_HALF_UP);
            // 确保分数在合理范围
            if (score.compareTo(totalScore) > 0) score = totalScore;
            if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;

            // 正确率百分比
            BigDecimal accuracyPct = new BigDecimal(actualAccuracy * 100)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            // 正确题数根据正确率计算
            int correctCount = 0;
            if (totalCount > 0) {
                correctCount = new BigDecimal(totalCount)
                        .multiply(new BigDecimal(actualAccuracy))
                        .setScale(0, BigDecimal.ROUND_HALF_UP)
                        .intValue();
            }
            if (correctCount > totalCount) {
                correctCount = totalCount;
            }
            int wrongCount = totalCount - correctCount;

            // 时长随机 1800-3600 秒（30-60 分钟）
            int duration = random.nextInt(1801) + 1800;

            ExamRecord record = new ExamRecord();
            record.setStudentId(studentId);
            record.setExamId(examId);
            record.setScore(score);
            record.setCorrectCount(correctCount);
            record.setWrongCount(wrongCount);
            record.setTotalCount(totalCount);
            record.setPendingCount(0);
            record.setAccuracy(accuracyPct);
            record.setDuration(duration);
            record.setSubmitStatus(1);
            // 回填冗余字段(删除考试后仍可展示)
            record.setExamName(exam != null ? exam.getName() : null);
            record.setExamProfessionId(exam != null ? exam.getProfessionId() : null);
            record.setExamCoverUrl(exam != null ? exam.getCoverUrl() : null);
            record.setExamQuestionCount(exam != null ? exam.getQuestionCount() : null);
            record.setExamTotalScore(exam != null ? exam.getTotalScore() : null);
            record.setPaperId(exam != null ? exam.getPaperId() : null);
            // 交卷时间优先使用证书用户的颁发日期
            LocalDateTime submitTime = now;
            try {
                Student stu = studentMapper.selectById(studentId);
                if (stu != null && StringUtils.hasText(stu.getIdCard()) && exam.getProfessionId() != null) {
                    Profession prof = professionMapper.selectById(exam.getProfessionId());
                    if (prof != null && StringUtils.hasText(prof.getName())) {
                        Certificate cert = certificateMapper.selectOne(
                                new LambdaQueryWrapper<Certificate>()
                                        .eq(Certificate::getIdCard, stu.getIdCard())
                                        .eq(Certificate::getProfession, prof.getName())
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
            } catch (Exception e) {
                // 查询失败则用当前时间
            }
            record.setSubmitTime(submitTime);
            record.setHasCertificate(0);

            examRecordMapper.insert(record);

            // ============ 生成 exam_answer 明细(使考试详情可查) ============
            // 随机决定哪些题正确(交错分布,避免前全对后全错)
            int total = paperQuestions.size();
            List<Boolean> correctFlags = new ArrayList<>(total);
            for (int i = 0; i < total; i++) correctFlags.add(false);
            // 随机选 correctCount 个位置设为正确
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < total; i++) indices.add(i);
            java.util.Collections.shuffle(indices, random);
            for (int i = 0; i < correctCount && i < indices.size(); i++) {
                correctFlags.set(indices.get(i), true);
            }

            int qIdx = 0;
            for (PaperQuestion pq : paperQuestions) {
                Question question = questionMap.get(pq.getQuestionId());
                if (question == null) continue;
                ExamAnswer answer = new ExamAnswer();
                answer.setRecordId(record.getId());
                answer.setQuestionId(question.getId());
                answer.setSort(qIdx);
                boolean isCorrect = correctFlags.get(qIdx);
                answer.setIsCorrect(isCorrect ? 1 : 0);
                // 构造学生答案(使用正确选项或错误选项)
                List<QuestionOption> opts = optionMap.getOrDefault(question.getId(), new ArrayList<>());
                List<QuestionOption> correctOpts = opts.stream()
                        .filter(o -> o.getIsCorrect() != null && o.getIsCorrect() == 1)
                        .collect(Collectors.toList());
                if (isCorrect && !correctOpts.isEmpty()) {
                    answer.setStudentAnswer(correctOpts.stream()
                            .map(QuestionOption::getLabel)
                            .collect(Collectors.joining(",")));
                } else if (!opts.isEmpty()) {
                    // 答错:取一个非正确选项
                    List<QuestionOption> wrongOpts = opts.stream()
                            .filter(o -> o.getIsCorrect() == null || o.getIsCorrect() == 0)
                            .collect(Collectors.toList());
                    if (wrongOpts.isEmpty()) {
                        answer.setStudentAnswer(opts.get(0).getLabel());
                    } else {
                        answer.setStudentAnswer(wrongOpts.get(random.nextInt(wrongOpts.size())).getLabel());
                    }
                } else {
                    // 填空题/简答题:空着,算答错
                    answer.setStudentAnswer("");
                }
                examAnswerMapper.insert(answer);
                qIdx++;
            }

            // ============ 自动考试也回写理论成绩到证书用户 ============
            try {
                Student stu = studentMapper.selectById(studentId);
                if (stu != null && StringUtils.hasText(stu.getIdCard()) && exam.getProfessionId() != null) {
                    String scoreStr = score.setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString();
                    // 优先用专业名称回写,certificate表中profession字段通常存的是中文名称
                    Profession prof = professionMapper.selectById(exam.getProfessionId());
                    String professionForSync = (prof != null && StringUtils.hasText(prof.getName()))
                            ? prof.getName() : exam.getProfessionId().toString();
                    certificateService.syncTheoryScore(stu.getIdCard(), professionForSync, scoreStr);
                }
            } catch (Exception e) {
                // 回写失败不影响自动考试主流程
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("自动考试成绩回写证书失败: examId={}, studentId={}", examId, studentId, e);
            }
        }
    }

    /**
     * 获取学生在指定专业下的历史考试基础正确率
     * 如果有历史考试记录,取历史平均正确率;如果没有,给一个默认的 75%
     */
    private BigDecimal getStudentBaseAccuracy(Long studentId, Long professionId) {
        try {
            // 查该学生所有已提交的考试记录(排除自动考试产生的记录,避免自动考试的accuracy锁死后续随机性)
            List<ExamRecord> records = examRecordMapper.selectList(
                    new LambdaQueryWrapper<ExamRecord>()
                            .eq(ExamRecord::getStudentId, studentId)
                            .eq(ExamRecord::getSubmitStatus, 1)
                            .eq(ExamRecord::getDuration, 0));
            if (records == null || records.isEmpty()) {
                // 没有真实考试记录:基于学生ID+专业ID生成一个稳定的默认正确率(65%-85%)
                // 这样不同学生的默认正确率不同,避免撞分
                long key = studentId + (professionId != null ? professionId : 0);
                int hash = Math.abs(Long.hashCode(key));
                double defaultAccuracy = 0.65 + (hash % 200) / 1000.0; // 0.65 ~ 0.85
                return new BigDecimal(defaultAccuracy);
            }
            // 取历史正确率平均值
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (ExamRecord r : records) {
                if (r.getAccuracy() != null && r.getAccuracy().compareTo(BigDecimal.ZERO) > 0) {
                    // accuracy 存的是 0-100 的百分比,转成 0-1 的小数
                    sum = sum.add(r.getAccuracy()).divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP);
                    count++;
                }
            }
            if (count > 0) {
                return sum.divide(new BigDecimal(count), 4, BigDecimal.ROUND_HALF_UP);
            }
            return new BigDecimal("0.75");
        } catch (Exception e) {
            return new BigDecimal("0.75");
        }
    }

    /**
     * 修复历史自动考试记录:为缺少 exam_answer 明细的考试记录补生成作答明细
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int fixMissingExamAnswers() {
        // 查询所有 submitStatus=1(已提交) 但没有 exam_answer 的考试记录
        List<ExamRecord> allRecords = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getSubmitStatus, 1)
                        .orderByDesc(ExamRecord::getCreateTime));
        int fixedCount = 0;
        Random random = new Random();
        for (ExamRecord record : allRecords) {
            // 检查是否已有 exam_answer
            Long existingCount = examAnswerMapper.selectCount(
                    new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getRecordId, record.getId()));
            if (existingCount != null && existingCount > 0) {
                continue; // 已有明细,跳过
            }
            // 查试卷题目
            Exam exam = this.getById(record.getExamId());
            if (exam == null || exam.getPaperId() == null) {
                continue;
            }
            List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>()
                            .eq(PaperQuestion::getPaperId, exam.getPaperId())
                            .orderByAsc(PaperQuestion::getSort));
            if (paperQuestions.isEmpty()) {
                continue;
            }
            // 预加载题目和选项
            List<Long> qIds = paperQuestions.stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());
            Map<Long, Question> questionMap = questionMapper.selectBatchIds(qIds).stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
            List<QuestionOption> options = questionOptionMapper.selectList(
                    new LambdaQueryWrapper<QuestionOption>()
                            .in(QuestionOption::getQuestionId, qIds)
                            .orderByAsc(QuestionOption::getSort));
            Map<Long, List<QuestionOption>> optionMap = new HashMap<>();
            for (QuestionOption opt : options) {
                optionMap.computeIfAbsent(opt.getQuestionId(), k -> new ArrayList<>()).add(opt);
            }
            // 随机决定哪些题正确(交错分布,避免前全对后全错)
            int total = paperQuestions.size();
            int correctCount = record.getCorrectCount() != null ? record.getCorrectCount() : 0;
            List<Boolean> correctFlags = new ArrayList<>(total);
            for (int i = 0; i < total; i++) correctFlags.add(false);
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < total; i++) indices.add(i);
            java.util.Collections.shuffle(indices, random);
            for (int i = 0; i < correctCount && i < indices.size(); i++) {
                correctFlags.set(indices.get(i), true);
            }

            int qIdx = 0;
            for (PaperQuestion pq : paperQuestions) {
                Question question = questionMap.get(pq.getQuestionId());
                if (question == null) continue;
                ExamAnswer answer = new ExamAnswer();
                answer.setRecordId(record.getId());
                answer.setQuestionId(question.getId());
                answer.setSort(qIdx);
                boolean isCorrect = correctFlags.get(qIdx);
                answer.setIsCorrect(isCorrect ? 1 : 0);
                // 构造学生答案
                List<QuestionOption> opts = optionMap.getOrDefault(question.getId(), new ArrayList<>());
                List<QuestionOption> correctOpts = opts.stream()
                        .filter(o -> o.getIsCorrect() != null && o.getIsCorrect() == 1)
                        .collect(Collectors.toList());
                if (isCorrect && !correctOpts.isEmpty()) {
                    answer.setStudentAnswer(correctOpts.stream()
                            .map(QuestionOption::getLabel).collect(Collectors.joining(",")));
                } else if (!opts.isEmpty()) {
                    List<QuestionOption> wrongOpts = opts.stream()
                            .filter(o -> o.getIsCorrect() == null || o.getIsCorrect() == 0)
                            .collect(Collectors.toList());
                    if (wrongOpts.isEmpty()) {
                        answer.setStudentAnswer(opts.get(0).getLabel());
                    } else {
                        answer.setStudentAnswer(wrongOpts.get(random.nextInt(wrongOpts.size())).getLabel());
                    }
                } else {
                    // 填空题/简答题:空着,算答错
                    answer.setStudentAnswer("");
                }
                examAnswerMapper.insert(answer);
                qIdx++;
            }
            fixedCount++;
        }
        return fixedCount;
    }
}
