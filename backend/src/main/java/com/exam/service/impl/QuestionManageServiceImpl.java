package com.exam.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.OptionDTO;
import com.exam.dto.QuestionDTO;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.QuestionManageService;
import com.exam.vo.QuestionExportVO;
import com.exam.vo.QuestionImportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionManageServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionManageService {

    @Autowired
    private QuestionOptionMapper questionOptionMapper;
    @Autowired
    private QuestionCategoryMapper questionCategoryMapper;
    @Autowired
    private ExamQuestionMapper examQuestionMapper;
    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;
    @Autowired
    private ProfessionMapper professionMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<Map<String, Object>> page(Integer page, Integer size, Integer type, Long categoryId,
                                                String keyword, String createTimeStart, String createTimeEnd,
                                                Integer enabled, Long professionId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(type != null, Question::getType, type)
                .eq(categoryId != null, Question::getCategoryId, categoryId)
                .eq(professionId != null, Question::getProfessionId, professionId)
                .like(StringUtils.hasText(keyword), Question::getContent, keyword)
                .eq(enabled != null, Question::getEnabled, enabled)
                .orderByDesc(Question::getCreateTime);
        if (StringUtils.hasText(createTimeStart)) {
            wrapper.ge(Question::getCreateTime, LocalDate.parse(createTimeStart).atStartOfDay());
        }
        if (StringUtils.hasText(createTimeEnd)) {
            wrapper.le(Question::getCreateTime, LocalDate.parse(createTimeEnd).atTime(23, 59, 59));
        }
        Page<Question> p = new Page<>(page, size);
        Page<Question> result = this.page(p, wrapper);

        // 批量查询选项和分类
        List<Long> questionIds = result.getRecords().stream().map(Question::getId).collect(Collectors.toList());
        List<Long> categoryIds = result.getRecords().stream().map(Question::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> professionIds = result.getRecords().stream().map(Question::getProfessionId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, List<QuestionOption>> optionMap = questionIds.isEmpty() ? new HashMap<>() :
                questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, questionIds)
                        .orderByAsc(QuestionOption::getSort))
                        .stream().collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? new HashMap<>() :
                questionCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getName));
        Map<Long, String> professionNameMap = professionIds.isEmpty() ? new HashMap<>() :
                professionMapper.selectBatchIds(professionIds).stream()
                        .collect(Collectors.toMap(Profession::getId, Profession::getName));

        List<Map<String, Object>> records = new ArrayList<>();
        for (Question q : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", q.getId());
            item.put("type", q.getType());
            item.put("categoryId", q.getCategoryId());
            item.put("categoryName", categoryNameMap.get(q.getCategoryId()));
            item.put("professionId", q.getProfessionId());
            item.put("professionName", professionNameMap.get(q.getProfessionId()));
            item.put("content", q.getContent());
            item.put("analysis", q.getAnalysis());
            item.put("correctAnswer", q.getCorrectAnswer());
            item.put("score", q.getScore());
            item.put("hasImage", q.getHasImage());
            item.put("enabled", q.getEnabled());
            item.put("createTime", q.getCreateTime());
            item.put("options", optionMap.getOrDefault(q.getId(), new ArrayList<>()));
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
        Question question = this.getById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        List<QuestionOption> options = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, id)
                        .orderByAsc(QuestionOption::getSort));
        String categoryName = null;
        if (question.getCategoryId() != null) {
            QuestionCategory category = questionCategoryMapper.selectById(question.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", question.getId());
        result.put("type", question.getType());
        result.put("categoryId", question.getCategoryId());
        result.put("categoryName", categoryName);
        result.put("professionId", question.getProfessionId());
        result.put("content", question.getContent());
        result.put("analysis", question.getAnalysis());
        result.put("correctAnswer", question.getCorrectAnswer());
        result.put("score", question.getScore());
        result.put("hasImage", question.getHasImage());
        result.put("enabled", question.getEnabled());
        result.put("createTime", question.getCreateTime());
        result.put("options", options);
        return result;
    }

    /**
     * 题干归一化：去除首尾空白、合并内部连续空白(含全角空格/换行/制表符)为单个空格，
     * 用于重复题干比对，避免仅空白差异导致的漏判。
     */
    private String normalizeContent(String content) {
        if (content == null) return "";
        // 全角空格转半角，再合并所有连续空白
        return content.replace('\u3000', ' ').trim().replaceAll("\\s+", " ");
    }

    @Override
    public List<Map<String, Object>> checkDuplicate(String content, Long professionId, Long excludeId) {
        return checkDuplicate(content, null, professionId, excludeId, null);
    }

    @Override
    public List<Map<String, Object>> checkDuplicate(String content, Long categoryId, Long professionId, Long excludeId, Integer type) {
        if (!StringUtils.hasText(content)) {
            return new ArrayList<>();
        }
        // 先按原始内容 like 查询缩小范围，再在内存中归一化精确比对
        String keyword = content.trim();
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .like(Question::getContent, keyword)
                .eq(categoryId != null, Question::getCategoryId, categoryId)
                .eq(professionId != null, Question::getProfessionId, professionId)
                .eq(type != null, Question::getType, type)
                .ne(excludeId != null, Question::getId, excludeId)
                .orderByDesc(Question::getCreateTime)
                .last("LIMIT 20");
        List<Question> candidates = this.list(wrapper);
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        // 内存中归一化精确匹配：题目分类+专业+题干都相同才视为重复
        String normalized = normalizeContent(content);
        List<Question> matched = candidates.stream()
                .filter(q -> normalizeContent(q.getContent()).equals(normalized))
                .filter(q -> categoryId == null || categoryId.equals(q.getCategoryId()))
                .filter(q -> professionId == null || professionId.equals(q.getProfessionId()))
                .filter(q -> type == null || type.equals(q.getType()))
                .collect(Collectors.toList());
        if (matched.isEmpty()) {
            return new ArrayList<>();
        }
        return buildQuestionDetailList(matched);
    }

    /**
     * 批量构建题目详情(含选项、分类名、专业名)，供重复对比展示复用
     */
    private List<Map<String, Object>> buildQuestionDetailList(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        List<Long> categoryIds = questions.stream().map(Question::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> professionIds = questions.stream().map(Question::getProfessionId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, List<QuestionOption>> optionMap = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, questionIds)
                        .orderByAsc(QuestionOption::getSort))
                .stream().collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? new HashMap<>() :
                questionCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getName, (a, b) -> a));
        Map<Long, String> professionNameMap = professionIds.isEmpty() ? new HashMap<>() :
                professionMapper.selectBatchIds(professionIds).stream()
                        .collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", q.getId());
            item.put("type", q.getType());
            item.put("categoryId", q.getCategoryId());
            item.put("categoryName", categoryNameMap.get(q.getCategoryId()));
            item.put("professionId", q.getProfessionId());
            item.put("professionName", professionNameMap.get(q.getProfessionId()));
            item.put("content", q.getContent());
            item.put("analysis", q.getAnalysis());
            item.put("correctAnswer", q.getCorrectAnswer());
            item.put("score", q.getScore());
            item.put("hasImage", q.getHasImage());
            item.put("enabled", q.getEnabled());
            item.put("createTime", q.getCreateTime() != null ? q.getCreateTime().format(FMT) : null);
            item.put("options", optionMap.getOrDefault(q.getId(), new ArrayList<>()));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(QuestionDTO dto) {
        // 题目分类+专业+题干重复检测：未显式要求强制创建时，若库中已有相同记录则抛出业务异常
        if (!Boolean.TRUE.equals(dto.getForce()) && StringUtils.hasText(dto.getContent())) {
            List<Map<String, Object>> duplicates = checkDuplicate(dto.getContent(), dto.getCategoryId(), dto.getProfessionId(), null, dto.getType());
            if (!duplicates.isEmpty()) {
                throw new BusinessException(409, "题目分类+专业+题干已存在 " + duplicates.size() + " 条重复记录，请确认是否继续创建");
            }
        }
        Question question = new Question();
        question.setType(dto.getType());
        question.setCategoryId(dto.getCategoryId());
        question.setProfessionId(dto.getProfessionId());
        question.setContent(dto.getContent());
        question.setAnalysis(dto.getAnalysis());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setScore(dto.getScore());
        // 如果content包含<img则hasImage=1
        question.setHasImage(StringUtils.hasText(dto.getContent()) && dto.getContent().contains("<img") ? 1 : 0);
        question.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        this.save(question);

        // 创建选项（填空题/简答题不需要选项）
        saveOptions(question.getId(), dto.getType(), dto.getOptions());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(QuestionDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        Question question = this.getById(dto.getId());
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        question.setType(dto.getType());
        question.setCategoryId(dto.getCategoryId());
        question.setProfessionId(dto.getProfessionId());
        question.setContent(dto.getContent());
        question.setAnalysis(dto.getAnalysis());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setScore(dto.getScore());
        question.setHasImage(StringUtils.hasText(dto.getContent()) && dto.getContent().contains("<img") ? 1 : 0);
        if (dto.getEnabled() != null) {
            question.setEnabled(dto.getEnabled());
        }
        this.updateById(question);

        // 先删除旧选项再重新创建（填空题/简答题不需要选项）
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, dto.getId()));
        saveOptions(dto.getId(), dto.getType(), dto.getOptions());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查是否被试卷引用,有引用则禁止删除
        long paperCount = paperQuestionMapper.selectCount(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getQuestionId, id));
        if (paperCount > 0) {
            throw new BusinessException("该题目已被 " + paperCount + " 张试卷引用,不能删除");
        }
        // 检查是否被考试直接引用(考试题目关联表)
        long examCount = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getQuestionId, id));
        if (examCount > 0) {
            throw new BusinessException("该题目已被 " + examCount + " 个考试引用,不能删除");
        }
        // 删除选项
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, id));
        // 删除错题本
        wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getQuestionId, id));
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
    public void export(HttpServletResponse response, Integer type, Long categoryId, String keyword,
                       String createTimeStart, String createTimeEnd, Integer enabled, Long professionId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(type != null, Question::getType, type)
                .eq(categoryId != null, Question::getCategoryId, categoryId)
                .eq(professionId != null, Question::getProfessionId, professionId)
                .like(StringUtils.hasText(keyword), Question::getContent, keyword)
                .eq(enabled != null, Question::getEnabled, enabled)
                .orderByDesc(Question::getCreateTime);
        if (StringUtils.hasText(createTimeStart)) {
            wrapper.ge(Question::getCreateTime, LocalDate.parse(createTimeStart).atStartOfDay());
        }
        if (StringUtils.hasText(createTimeEnd)) {
            wrapper.le(Question::getCreateTime, LocalDate.parse(createTimeEnd).atTime(23, 59, 59));
        }
        List<Question> questions = this.list(wrapper);

        List<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        List<Long> catIds = questions.stream().map(Question::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> profIds = questions.stream().map(Question::getProfessionId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, List<QuestionOption>> optionMap = questionIds.isEmpty() ? new HashMap<>() :
                questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, questionIds)
                        .orderByAsc(QuestionOption::getSort))
                        .stream().collect(Collectors.groupingBy(QuestionOption::getQuestionId));
        Map<Long, String> categoryNameMap = catIds.isEmpty() ? new HashMap<>() :
                questionCategoryMapper.selectBatchIds(catIds).stream()
                        .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getName));
        Map<Long, String> professionNameMap = profIds.isEmpty() ? new HashMap<>() :
                professionMapper.selectBatchIds(profIds).stream()
                        .collect(Collectors.toMap(Profession::getId, Profession::getName));

        List<QuestionExportVO> exportList = new ArrayList<>();
        for (Question q : questions) {
            QuestionExportVO vo = new QuestionExportVO();
            vo.setType(typeName(q.getType()));
            vo.setCategoryName(categoryNameMap.get(q.getCategoryId()));
            vo.setProfessionName(professionNameMap.get(q.getProfessionId()));
            vo.setContent(q.getContent());
            vo.setAnalysis(q.getAnalysis());
            vo.setScore(q.getScore() != null ? q.getScore().toPlainString() : "");
            vo.setEnabled(q.getEnabled() == 1 ? "可用" : "不可用");
            List<QuestionOption> options = optionMap.getOrDefault(q.getId(), new ArrayList<>());
            StringBuilder correctAnswer = new StringBuilder();
            for (QuestionOption opt : options) {
                if ("A".equals(opt.getLabel())) vo.setOptionA(opt.getContent());
                else if ("B".equals(opt.getLabel())) vo.setOptionB(opt.getContent());
                else if ("C".equals(opt.getLabel())) vo.setOptionC(opt.getContent());
                else if ("D".equals(opt.getLabel())) vo.setOptionD(opt.getContent());
                if (opt.getIsCorrect() != null && opt.getIsCorrect() == 1) {
                    if (correctAnswer.length() > 0) correctAnswer.append(",");
                    correctAnswer.append(opt.getLabel());
                }
            }
            // 填空题的正确答案取自 correctAnswer 字段
            if (q.getType() != null && q.getType() == 3 && StringUtils.hasText(q.getCorrectAnswer())) {
                correctAnswer = new StringBuilder(q.getCorrectAnswer());
            }
            vo.setCorrectAnswer(correctAnswer.toString());
            exportList.add(vo);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("题目列表", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), QuestionExportVO.class)
                    .sheet("题目列表")
                    .doWrite(exportList);
        } catch (IOException e) {
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        // 构造各题型示例数据,帮助用户理解导入格式
        List<QuestionImportVO> sampleList = new ArrayList<>();
        QuestionImportVO single = new QuestionImportVO();
        single.setType("单选");
        single.setCategoryName("基础题");
        single.setProfessionName("电工");
        single.setContent("Java 中哪个关键字用于继承？");
        single.setOptionA("implements");
        single.setOptionB("extends");
        single.setOptionC("inherits");
        single.setOptionD("super");
        single.setCorrectAnswer("B");
        single.setAnalysis("extends 用于类的继承,implements 用于实现接口");
        single.setScore("2");
        sampleList.add(single);

        QuestionImportVO multi = new QuestionImportVO();
        multi.setType("多选");
        multi.setCategoryName("基础题");
        multi.setContent("以下哪些是 Java 的基本数据类型？");
        multi.setOptionA("int");
        multi.setOptionB("String");
        multi.setOptionC("boolean");
        multi.setOptionD("double");
        multi.setCorrectAnswer("A,C,D");
        multi.setAnalysis("String 是引用类型,不是基本数据类型");
        multi.setScore("3");
        sampleList.add(multi);

        QuestionImportVO judge = new QuestionImportVO();
        judge.setType("判断");
        judge.setCategoryName("基础题");
        judge.setContent("Java 是一门面向对象的编程语言");
        judge.setOptionA("正确");
        judge.setOptionB("错误");
        judge.setCorrectAnswer("A");
        judge.setAnalysis("Java 是面向对象语言");
        judge.setScore("2");
        sampleList.add(judge);

        QuestionImportVO fill = new QuestionImportVO();
        fill.setType("填空");
        fill.setCategoryName("基础题");
        fill.setContent("Java 中定义常量使用的关键字是____");
        fill.setCorrectAnswer("final");
        fill.setAnalysis("final 关键字用于定义常量");
        fill.setScore("2");
        sampleList.add(fill);

        QuestionImportVO shortAns = new QuestionImportVO();
        shortAns.setType("简答");
        shortAns.setCategoryName("基础题");
        shortAns.setContent("简述面向对象的三大特征");
        shortAns.setCorrectAnswer("封装、继承、多态");
        shortAns.setAnalysis("封装、继承、多态是面向对象的三大基本特征");
        shortAns.setScore("5");
        sampleList.add(shortAns);

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("题库导入模板", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), QuestionImportVO.class)
                    .sheet("题库导入模板")
                    .doWrite(sampleList);
        } catch (IOException e) {
            throw new BusinessException("模板下载失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importQuestions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        List<QuestionImportVO> list;
        try {
            list = EasyExcel.read(file.getInputStream()).head(QuestionImportVO.class).sheet().doReadSync();
        } catch (IOException e) {
            throw new BusinessException("读取文件失败：" + e.getMessage());
        }
        if (list == null || list.isEmpty()) {
            throw new BusinessException("文件中没有数据");
        }
        // 预加载所有分类和专业
        Map<String, Long> categoryNameMap = questionCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(QuestionCategory::getName, QuestionCategory::getId, (a, b) -> a));
        Map<String, Long> professionNameMap = professionMapper.selectList(null).stream()
                .collect(Collectors.toMap(Profession::getName, Profession::getId, (a, b) -> a));

        // 预加载库中已有题目，用于导入时检测重复并对比
        // 重复标准:题目分类+专业+题干 三个完全一样才视为重复
        List<Question> allExisting = this.list(new LambdaQueryWrapper<Question>()
                .select(Question::getId, Question::getContent, Question::getCategoryId, Question::getProfessionId));
        Map<String, List<Question>> existingByContent = new HashMap<>();
        for (Question q : allExisting) {
            String key = (q.getCategoryId() != null ? q.getCategoryId() : "null") + "|prof="
                    + (q.getProfessionId() != null ? q.getProfessionId() : "null") + "|"
                    + normalizeContent(q.getContent());
            if (key.isEmpty()) continue;
            existingByContent.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
        }
        // 记录本次导入已处理(已入库)的题目分类+专业+题干，避免同一文件内重复被重复创建
        Set<String> importedKeys = new HashSet<>();

        int successCount = 0;
        List<Map<String, Object>> failList = new ArrayList<>();
        List<Map<String, Object>> duplicateList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            QuestionImportVO vo = list.get(i);
            int rowNum = i + 2; // Excel行号(第1行是表头)
            if (!StringUtils.hasText(vo.getContent())) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("row", rowNum);
                fail.put("content", vo.getContent());
                fail.put("reason", "题干为空");
                failList.add(fail);
                continue;
            }
            try {
                Question question = new Question();
                Integer type = parseType(vo.getType());
                if (type == null) {
                    throw new RuntimeException("题型未识别: " + (vo.getType() == null ? "空" : vo.getType()) + "(请填写: 单选/多选/填空/判断/简答 或 1-5)");
                }
                question.setType(type);
                if (StringUtils.hasText(vo.getCategoryName())) {
                    question.setCategoryId(categoryNameMap.get(vo.getCategoryName()));
                }
                // 支持专业字段导入
                if (StringUtils.hasText(vo.getProfessionName())) {
                    Long profId = professionNameMap.get(vo.getProfessionName());
                    if (profId == null) {
                        throw new RuntimeException("专业不存在: " + vo.getProfessionName());
                    }
                    question.setProfessionId(profId);
                }
                // 题目分类+专业+题干 重复检测
                String dupKey = (question.getCategoryId() != null ? question.getCategoryId() : "null") + "|prof="
                        + (question.getProfessionId() != null ? question.getProfessionId() : "null") + "|"
                        + normalizeContent(vo.getContent());
                List<Question> dbDups = existingByContent.getOrDefault(dupKey, new ArrayList<>());
                if (!dbDups.isEmpty()) {
                    List<Map<String, Object>> dupDetails = buildQuestionDetailList(dbDups);
                    Map<String, Object> dup = new HashMap<>();
                    dup.put("row", rowNum);
                    dup.put("content", vo.getContent());
                    dup.put("reason", "题目分类+专业+题干与库中已有题目重复");
                    dup.put("existingQuestions", dupDetails);
                    duplicateList.add(dup);
                    continue;
                }
                if (importedKeys.contains(dupKey)) {
                    Map<String, Object> dup = new HashMap<>();
                    dup.put("row", rowNum);
                    dup.put("content", vo.getContent());
                    dup.put("reason", "题目分类+专业+题干与本文件内已导入题目重复");
                    duplicateList.add(dup);
                    continue;
                }
                question.setContent(vo.getContent());
                question.setAnalysis(vo.getAnalysis());
                if (type != null && type == 3) {
                    question.setCorrectAnswer(vo.getCorrectAnswer());
                }
                try {
                    question.setScore(StringUtils.hasText(vo.getScore()) ? new BigDecimal(vo.getScore()) : new BigDecimal("2.0"));
                } catch (NumberFormatException e) {
                    question.setScore(new BigDecimal("2.0"));
                }
                question.setHasImage(vo.getContent().contains("<img") ? 1 : 0);
                question.setEnabled(1);
                this.save(question);

                // 创建选项
                if (type == 1 || type == 2) {
                    String correctAnswer = vo.getCorrectAnswer() == null ? "" : vo.getCorrectAnswer().toUpperCase();
                    addOption(question.getId(), "A", vo.getOptionA(), correctAnswer.contains("A"), 1);
                    addOption(question.getId(), "B", vo.getOptionB(), correctAnswer.contains("B"), 2);
                    addOption(question.getId(), "C", vo.getOptionC(), correctAnswer.contains("C"), 3);
                    addOption(question.getId(), "D", vo.getOptionD(), correctAnswer.contains("D"), 4);
                } else if (type == 4) {
                    String correctAnswer = vo.getCorrectAnswer() == null ? "" : vo.getCorrectAnswer().toUpperCase();
                    boolean isACorrect = correctAnswer.contains("A") || correctAnswer.contains("正确");
                    addOption(question.getId(), "A", "正确", isACorrect, 1);
                    addOption(question.getId(), "B", "错误", !isACorrect, 2);
                }
                // 记录本批次已导入题干+类型，防止同文件内重复被重复创建
                importedKeys.add(dupKey);
                successCount++;
            } catch (Exception e) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("row", rowNum);
                fail.put("content", vo.getContent());
                fail.put("reason", e.getMessage() != null ? e.getMessage() : "导入失败");
                failList.add(fail);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failList.size());
        result.put("failList", failList);
        // 题干重复明细(含库中已有题目详情，供前端对比)，重复的不计入成功也不计入失败
        result.put("duplicateCount", duplicateList.size());
        result.put("duplicateList", duplicateList);
        return result;
    }

    /**
     * 解析题型名称为数字
     */
    private Integer parseType(String typeName) {
        if (!StringUtils.hasText(typeName)) {
            return null; // 未指定类型时返回 null,不默认为单选
        }
        String normalized = typeName.trim();
        // 支持"单选题"和"单选"两种写法: 去除尾部"题"字后统一匹配
        if (normalized.endsWith("题")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        switch (normalized) {
            case "单选": case "1": return 1;
            case "多选": case "2": return 2;
            case "填空": case "3": return 3;
            case "判断": case "4": return 4;
            case "简答": case "5": return 5;
            default: return null; // 无法识别的类型返回 null
        }
    }

    /**
     * 保存选项
     * type=3（填空）/ type=5（简答）：不需要选项
     * type=4（判断）：选项固定为"正确"和"错误"
     * type=1（单选）/ type=2（多选）：使用传入的选项
     */
    private void saveOptions(Long questionId, Integer type, List<OptionDTO> options) {
        // 填空题和简答题不需要选项
        if (type != null && (type == 3 || type == 5)) {
            return;
        }
        // 判断题选项固定为"正确"和"错误"
        if (type != null && type == 4) {
            // 如果前端传入了选项，则使用前端传入的；否则使用默认的"正确"/"错误"
            if (options == null || options.isEmpty()) {
                QuestionOption correct = new QuestionOption();
                correct.setQuestionId(questionId);
                correct.setLabel("A");
                correct.setContent("正确");
                correct.setIsCorrect(1);
                correct.setSort(1);
                questionOptionMapper.insert(correct);

                QuestionOption wrong = new QuestionOption();
                wrong.setQuestionId(questionId);
                wrong.setLabel("B");
                wrong.setContent("错误");
                wrong.setIsCorrect(0);
                wrong.setSort(2);
                questionOptionMapper.insert(wrong);
                return;
            }
        }
        if (options == null || options.isEmpty()) {
            return;
        }
        int sort = 1;
        for (OptionDTO opt : options) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setLabel(opt.getLabel());
            option.setContent(opt.getContent());
            option.setIsCorrect(opt.getIsCorrect() == null ? 0 : opt.getIsCorrect());
            option.setSort(opt.getSort() == null ? sort : opt.getSort());
            questionOptionMapper.insert(option);
            sort++;
        }
    }

    private void addOption(Long questionId, String label, String content, boolean isCorrect, int sort) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        QuestionOption option = new QuestionOption();
        option.setQuestionId(questionId);
        option.setLabel(label);
        option.setContent(content);
        option.setIsCorrect(isCorrect ? 1 : 0);
        option.setSort(sort);
        questionOptionMapper.insert(option);
    }

    /**
     * 题型名称转换
     */
    private String typeName(Integer type) {
        if (type == null) return "单选";
        switch (type) {
            case 1: return "单选";
            case 2: return "多选";
            case 3: return "填空";
            case 4: return "判断";
            case 5: return "简答";
            default: return "单选";
        }
    }
}
