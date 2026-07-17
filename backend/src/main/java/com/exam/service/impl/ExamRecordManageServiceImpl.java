package com.exam.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.ExamRecordManageService;
import com.exam.vo.ExamRecordExportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamRecordManageServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordManageService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private ExamAnswerMapper examAnswerMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionOptionMapper questionOptionMapper;
    @Autowired
    private ProfessionMapper professionMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<Map<String, Object>> page(Integer page, Integer size, String examName,
                                                String phone, Integer submitStatus) {
        // 根据考试名称筛选考试ID
        Set<Long> examIds = null;
        if (StringUtils.hasText(examName)) {
            List<Exam> exams = examMapper.selectList(
                    new LambdaQueryWrapper<Exam>().like(Exam::getName, examName));
            examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());
            if (examIds.isEmpty()) {
                return emptyResult(page, size);
            }
        }
        // 根据手机号筛选学生ID
        Set<Long> studentIds = null;
        if (StringUtils.hasText(phone)) {
            List<Student> students = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().like(Student::getPhone, phone));
            studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
            if (studentIds.isEmpty()) {
                return emptyResult(page, size);
            }
        }

        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<ExamRecord>()
                .in(examIds != null, ExamRecord::getExamId, examIds)
                .in(studentIds != null, ExamRecord::getStudentId, studentIds)
                .eq(submitStatus != null, ExamRecord::getSubmitStatus, submitStatus)
                .orderByDesc(ExamRecord::getCreateTime);
        Page<ExamRecord> p = new Page<>(page, size);
        Page<ExamRecord> result = this.page(p, wrapper);

        List<Map<String, Object>> records = buildRecordMaps(result.getRecords());
        PageResult<Map<String, Object>> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setRecords(records);
        return pageResult;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        ExamRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException("考试记录不存在");
        }
        Map<String, Object> result = buildRecordMaps(Collections.singletonList(record)).get(0);
        // 查询作答详情
        List<ExamAnswer> answers = examAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getRecordId, id)
                        .orderByAsc(ExamAnswer::getSort));
        List<Long> questionIds = answers.stream().map(ExamAnswer::getQuestionId).distinct().collect(Collectors.toList());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? new HashMap<>() :
                questionMapper.selectBatchIds(questionIds).stream().collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, List<QuestionOption>> optionMap = questionIds.isEmpty() ? new HashMap<>() :
                questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, questionIds)
                        .orderByAsc(QuestionOption::getSort))
                        .stream().collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        List<Map<String, Object>> answerList = new ArrayList<>();
        for (ExamAnswer answer : answers) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", answer.getId());
            item.put("questionId", answer.getQuestionId());
            item.put("studentAnswer", answer.getStudentAnswer());
            item.put("isCorrect", answer.getIsCorrect());
            item.put("sort", answer.getSort());
            item.put("score", answer.getScore());
            Question question = questionMap.get(answer.getQuestionId());
            item.put("type", question != null ? question.getType() : null);
            item.put("content", question != null ? question.getContent() : null);
            // 按题型获取正确答案
            Integer qType = question != null ? question.getType() : null;
            String correctAnswer = null;
            List<QuestionOption> qOpts = optionMap.getOrDefault(answer.getQuestionId(), new ArrayList<>());
            if (qType != null && qType == 3) {
                // 填空题：取 correctAnswer 字段
                correctAnswer = question.getCorrectAnswer();
            } else if (qType != null && qType == 5) {
                // 简答题：不显示标准答案
                correctAnswer = null;
            } else if (qType != null) {
                // 选择题/判断题：从选项中取 isCorrect=1 的 label 拼接
                StringBuilder sb = new StringBuilder();
                for (QuestionOption opt : qOpts) {
                    if (opt.getIsCorrect() != null && opt.getIsCorrect() == 1) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(opt.getLabel());
                    }
                }
                correctAnswer = sb.toString();
            }
            item.put("correctAnswer", correctAnswer);
            item.put("question", question);
            item.put("options", qOpts);
            answerList.add(item);
        }
        result.put("answers", answerList);
        return result;
    }

    @Override
    public void export(HttpServletResponse response, String examName, String phone, Integer submitStatus) {
        Set<Long> examIds = null;
        if (StringUtils.hasText(examName)) {
            List<Exam> exams = examMapper.selectList(
                    new LambdaQueryWrapper<Exam>().like(Exam::getName, examName));
            examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());
            if (examIds.isEmpty()) {
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
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<ExamRecord>()
                .in(examIds != null, ExamRecord::getExamId, examIds)
                .in(studentIds != null, ExamRecord::getStudentId, studentIds)
                .eq(submitStatus != null, ExamRecord::getSubmitStatus, submitStatus)
                .orderByDesc(ExamRecord::getCreateTime);
        List<ExamRecord> records = this.list(wrapper);

        // 批量查询考试和学生
        List<Long> rcExamIds = records.stream().map(ExamRecord::getExamId).distinct().collect(Collectors.toList());
        List<Long> rcStudentIds = records.stream().map(ExamRecord::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Exam> examMap = rcExamIds.isEmpty() ? new HashMap<>() :
                examMapper.selectBatchIds(rcExamIds).stream().collect(Collectors.toMap(Exam::getId, e -> e));
        Map<Long, Student> studentMap = rcStudentIds.isEmpty() ? new HashMap<>() :
                studentMapper.selectBatchIds(rcStudentIds).stream().collect(Collectors.toMap(Student::getId, s -> s));

        List<ExamRecordExportVO> exportList = new ArrayList<>();
        for (ExamRecord record : records) {
            Exam exam = examMap.get(record.getExamId());
            Student student = studentMap.get(record.getStudentId());
            ExamRecordExportVO vo = new ExamRecordExportVO();
            vo.setExamName(exam != null ? exam.getName() : "");
            vo.setQuestionCount(exam != null ? exam.getQuestionCount() : 0);
            vo.setTotalScore(exam != null && exam.getTotalScore() != null ? exam.getTotalScore().toPlainString() : "");
            vo.setDuration(exam != null ? exam.getDuration() : 0);
            vo.setStudentName(student != null ? student.getNickname() : "");
            vo.setPhone(student != null ? student.getPhone() : "");
            vo.setScore(record.getScore() != null ? record.getScore().toPlainString() : "");
            vo.setSubmitTime(record.getSubmitTime() != null ? record.getSubmitTime().format(FMT) : "");
            vo.setCertificate(record.getHasCertificate() != null && record.getHasCertificate() == 1 ? "有" : "无");
            exportList.add(vo);
        }
        writeExcel(response, exportList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeAnswers(Long recordId, List<Map<String, Object>> grades) {
        ExamRecord record = this.getById(recordId);
        if (record == null) {
            throw new BusinessException("考试记录不存在");
        }
        // 1. 遍历 grades, 更新每道题的 score 和 isCorrect(1=正确,0=错误)
        if (grades != null && !grades.isEmpty()) {
            for (Map<String, Object> g : grades) {
                Object answerIdObj = g.get("answerId");
                if (answerIdObj == null) {
                    continue;
                }
                Long answerId = Long.valueOf(answerIdObj.toString());
                ExamAnswer answer = examAnswerMapper.selectById(answerId);
                // 答案必须属于该考试记录(防越权)
                if (answer == null || !recordId.equals(answer.getRecordId())) {
                    continue;
                }
                // 解析 score
                BigDecimal score = null;
                Object scoreObj = g.get("score");
                if (scoreObj != null && StringUtils.hasText(scoreObj.toString())) {
                    score = new BigDecimal(scoreObj.toString());
                }
                // 解析 isCorrect(1=正确, 0=错误)
                Integer isCorrect = null;
                Object isCorrectObj = g.get("isCorrect");
                if (isCorrectObj != null && StringUtils.hasText(isCorrectObj.toString())) {
                    isCorrect = Integer.valueOf(isCorrectObj.toString());
                }
                UpdateWrapper<ExamAnswer> uw = new UpdateWrapper<>();
                uw.eq("id", answerId)
                        .set("score", score);
                if (isCorrect != null) {
                    uw.set("is_correct", isCorrect);
                }
                examAnswerMapper.update(null, uw);
            }
        }
        // 2. 重新计算考试记录总分: 查该 record 下所有 answer
        LambdaQueryWrapper<ExamAnswer> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(ExamAnswer::getRecordId, recordId);
        List<ExamAnswer> allAnswers = examAnswerMapper.selectList(allWrapper);
        // 批量查询题目获取每题分值(自动阅卷正确的题用题目分值计入总分)
        List<Long> questionIds = allAnswers.stream()
                .map(ExamAnswer::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? new HashMap<>() :
                questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));
        BigDecimal totalScore = BigDecimal.ZERO;
        int correctCount = 0;
        int wrongCount = 0;
        int pendingCount = 0;
        for (ExamAnswer a : allAnswers) {
            Integer isCorrect = a.getIsCorrect();
            if (isCorrect != null && isCorrect == 2) {
                pendingCount++;
            } else if (isCorrect != null && isCorrect == 1) {
                correctCount++;
            } else if (isCorrect != null && isCorrect == 0) {
                wrongCount++;
            }
            // 该题得分: 优先用人工批改设置的 score, 否则自动阅卷正确的题用题目分值
            BigDecimal qScore = null;
            if (a.getScore() != null) {
                qScore = a.getScore();
            } else if (isCorrect != null && isCorrect == 1) {
                Question q = questionMap.get(a.getQuestionId());
                qScore = q != null && q.getScore() != null ? q.getScore() : BigDecimal.ZERO;
            }
            if (qScore != null) {
                totalScore = totalScore.add(qScore);
            }
        }
        // 3. 更新 exam_record 的 score、correctCount、wrongCount、pendingCount
        ExamRecord update = new ExamRecord();
        update.setId(recordId);
        update.setScore(totalScore);
        update.setCorrectCount(correctCount);
        update.setWrongCount(wrongCount);
        update.setPendingCount(pendingCount);
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        // 先删除关联的答题记录
        examAnswerMapper.delete(new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getRecordId, id));
        // 再删除考试记录
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteRecords(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 批量删除关联答题
        examAnswerMapper.delete(new LambdaQueryWrapper<ExamAnswer>().in(ExamAnswer::getRecordId, ids));
        // 批量删除记录
        this.removeByIds(ids);
    }

    private List<Map<String, Object>> buildRecordMaps(List<ExamRecord> records) {
        if (records.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> examIds = records.stream().map(ExamRecord::getExamId).distinct().collect(Collectors.toList());
        List<Long> studentIds = records.stream().map(ExamRecord::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Exam> examMap = examMapper.selectBatchIds(examIds).stream().collect(Collectors.toMap(Exam::getId, e -> e));
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(studentIds).stream().collect(Collectors.toMap(Student::getId, s -> s));
        // 批量查询专业信息
        List<Long> professionIds = examMap.values().stream()
                .map(Exam::getProfessionId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Profession> professionMap = professionIds.isEmpty() ? new HashMap<>() :
                professionMapper.selectBatchIds(professionIds).stream().collect(Collectors.toMap(Profession::getId, p -> p));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("studentId", record.getStudentId());
            item.put("examId", record.getExamId());
            item.put("score", record.getScore());
            item.put("correctCount", record.getCorrectCount());
            item.put("wrongCount", record.getWrongCount());
            item.put("totalCount", record.getTotalCount());
            item.put("pendingCount", record.getPendingCount());
            item.put("accuracy", record.getAccuracy());
            item.put("duration", record.getDuration());
            item.put("submitStatus", record.getSubmitStatus());
            item.put("submitTime", record.getSubmitTime());
            item.put("hasCertificate", record.getHasCertificate());
            item.put("createTime", record.getCreateTime());

            Exam exam = examMap.get(record.getExamId());
            if (exam != null) {
                item.put("examName", exam.getName());
                item.put("coverUrl", exam.getCoverUrl());
                item.put("questionCount", exam.getQuestionCount());
                item.put("totalScore", exam.getTotalScore());
                item.put("examDuration", exam.getDuration());
                // 返回专业信息
                if (exam.getProfessionId() != null) {
                    Profession prof = professionMap.get(exam.getProfessionId());
                    item.put("professionName", prof != null ? prof.getName() : null);
                }
            } else {
                // 考试已删除，使用冗余字段
                item.put("examName", record.getExamName());
                item.put("questionCount", record.getExamQuestionCount());
                item.put("totalScore", record.getExamTotalScore());
            }
            Student student = studentMap.get(record.getStudentId());
            if (student != null) {
                item.put("studentName", student.getNickname());
                item.put("phone", student.getPhone());
            }
            result.add(item);
        }
        return result;
    }

    private PageResult<Map<String, Object>> emptyResult(Integer page, Integer size) {
        PageResult<Map<String, Object>> empty = new PageResult<>();
        empty.setTotal(0);
        empty.setPage(page);
        empty.setSize(size);
        empty.setRecords(new ArrayList<>());
        return empty;
    }

    private void writeExcel(HttpServletResponse response, List<ExamRecordExportVO> data) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("考试记录", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), ExamRecordExportVO.class)
                    .sheet("考试记录")
                    .doWrite(data);
        } catch (IOException e) {
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }
}
