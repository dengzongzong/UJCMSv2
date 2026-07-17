package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.entity.Profession;
import com.exam.entity.Question;
import com.exam.entity.QuestionCategory;
import com.exam.entity.QuestionOption;
import com.exam.entity.QuestionTemplate;
import com.exam.entity.QuestionTemplateItem;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.QuestionCategoryMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.QuestionOptionMapper;
import com.exam.mapper.QuestionTemplateItemMapper;
import com.exam.mapper.QuestionTemplateMapper;
import com.exam.service.QuestionTemplateService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 试题模板管理服务实现。
 * <p>试题模板是一组题目的集合,可保存起来供后续快速组卷使用。</p>
 */
@Service
public class QuestionTemplateServiceImpl
        extends ServiceImpl<QuestionTemplateMapper, QuestionTemplate>
        implements QuestionTemplateService {

    @Autowired
    private QuestionTemplateItemMapper questionTemplateItemMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionOptionMapper questionOptionMapper;
    @Autowired
    private QuestionCategoryMapper questionCategoryMapper;
    @Autowired
    private ProfessionMapper professionMapper;

    @Override
    public PageResult<QuestionTemplate> page(Integer page, Integer size, String name,
                                             Long categoryId, Long professionId) {
        LambdaQueryWrapper<QuestionTemplate> wrapper = new LambdaQueryWrapper<QuestionTemplate>()
                .like(StringUtils.hasText(name), QuestionTemplate::getName, name)
                .eq(categoryId != null, QuestionTemplate::getCategoryId, categoryId)
                .eq(professionId != null, QuestionTemplate::getProfessionId, professionId)
                .orderByDesc(QuestionTemplate::getCreateTime);
        Page<QuestionTemplate> p = new Page<>(page, size);
        Page<QuestionTemplate> result = this.page(p, wrapper);

        fillCategoryAndProfessionName(result.getRecords());

        PageResult<QuestionTemplate> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setRecords(result.getRecords());
        return pageResult;
    }

    /**
     * 模板详情:返回包含一个元素的列表,该元素为模板详情 Map,
     * 含模板基本信息以及 questions 题目列表(每个题目含基本信息与选项列表)。
     */
    @Override
    public List<Map<String, Object>> detail(Long id) {
        QuestionTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 查询关联题目(按 sort 排序)
        List<QuestionTemplateItem> items = questionTemplateItemMapper.selectList(
                new LambdaQueryWrapper<QuestionTemplateItem>()
                        .eq(QuestionTemplateItem::getTemplateId, id)
                        .orderByAsc(QuestionTemplateItem::getSort));
        List<Long> questionIds = items.stream()
                .map(QuestionTemplateItem::getQuestionId)
                .collect(Collectors.toList());

        // 模板自身的分类/专业名
        String categoryName = null;
        if (template.getCategoryId() != null) {
            QuestionCategory category = questionCategoryMapper.selectById(template.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        String professionName = null;
        if (template.getProfessionId() != null) {
            Profession profession = professionMapper.selectById(template.getProfessionId());
            if (profession != null) {
                professionName = profession.getName();
            }
        }

        List<Map<String, Object>> questionList = buildQuestionList(items, questionIds);

        Map<String, Object> templateInfo = new HashMap<>();
        templateInfo.put("id", template.getId());
        templateInfo.put("name", template.getName());
        templateInfo.put("description", template.getDescription());
        templateInfo.put("categoryId", template.getCategoryId());
        templateInfo.put("categoryName", categoryName);
        templateInfo.put("professionId", template.getProfessionId());
        templateInfo.put("professionName", professionName);
        templateInfo.put("questionCount", template.getQuestionCount());
        templateInfo.put("totalScore", template.getTotalScore());
        templateInfo.put("createTime", template.getCreateTime());
        templateInfo.put("updateTime", template.getUpdateTime());
        templateInfo.put("questions", questionList);

        List<Map<String, Object>> result = new ArrayList<>();
        result.add(templateInfo);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(String name, String description, Long categoryId, Long professionId,
                       List<Long> questionIds) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("模板名称不能为空");
        }
        TemplateStat stat = prepareStat(questionIds);
        QuestionTemplate template = new QuestionTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setCategoryId(categoryId);
        template.setProfessionId(professionId);
        template.setQuestionCount(stat.questionIds.size());
        template.setTotalScore(stat.totalScore);
        this.save(template);
        insertItems(template.getId(), stat.questionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, String name, String description, Long categoryId, Long professionId,
                       List<Long> questionIds) {
        QuestionTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("模板名称不能为空");
        }
        TemplateStat stat = prepareStat(questionIds);
        template.setName(name);
        template.setDescription(description);
        template.setCategoryId(categoryId);
        template.setProfessionId(professionId);
        template.setQuestionCount(stat.questionIds.size());
        template.setTotalScore(stat.totalScore);
        this.updateById(template);

        // 先删除旧关联再插入新关联
        questionTemplateItemMapper.delete(new LambdaQueryWrapper<QuestionTemplateItem>()
                .eq(QuestionTemplateItem::getTemplateId, id));
        insertItems(id, stat.questionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        QuestionTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        // 模板目前不被试卷/考试直接引用,直接删除关联题目与模板本身
        questionTemplateItemMapper.delete(new LambdaQueryWrapper<QuestionTemplateItem>()
                .eq(QuestionTemplateItem::getTemplateId, id));
        this.removeById(id);
    }

    @Override
    public List<QuestionTemplate> listAll() {
        List<QuestionTemplate> list = this.list(new LambdaQueryWrapper<QuestionTemplate>()
                .orderByDesc(QuestionTemplate::getCreateTime));
        fillCategoryAndProfessionName(list);
        return list;
    }

    // ============================ 私有辅助方法 ============================

    /**
     * 批量填充模板的分类名与专业名(非持久化字段)。
     */
    private void fillCategoryAndProfessionName(List<QuestionTemplate> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> categoryIds = list.stream().map(QuestionTemplate::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> professionIds = list.stream().map(QuestionTemplate::getProfessionId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? new HashMap<>() :
                questionCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getName, (a, b) -> a));
        Map<Long, String> professionNameMap = professionIds.isEmpty() ? new HashMap<>() :
                professionMapper.selectBatchIds(professionIds).stream()
                        .collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));
        for (QuestionTemplate t : list) {
            t.setCategoryName(categoryNameMap.get(t.getCategoryId()));
            t.setProfessionName(professionNameMap.get(t.getProfessionId()));
        }
    }

    /**
     * 根据选中题目ID计算题目数量与总分,并返回(按原顺序过滤后)存在的题目ID列表。
     * totalScore 计算方式: 查询所有选中题目的 score 字段求和。
     */
    private TemplateStat prepareStat(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return new TemplateStat(new ArrayList<>(), BigDecimal.ZERO);
        }
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, BigDecimal> scoreMap = questions.stream().collect(Collectors.toMap(
                Question::getId,
                q -> q.getScore() == null ? BigDecimal.ZERO : q.getScore(),
                (a, b) -> a));
        List<Long> ordered = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        for (Long qid : questionIds) {
            if (scoreMap.containsKey(qid)) {
                ordered.add(qid);
                totalScore = totalScore.add(scoreMap.get(qid));
            }
        }
        return new TemplateStat(ordered, totalScore);
    }

    /**
     * 批量插入模板题目关联(按传入顺序设置 sort)。
     */
    private void insertItems(Long templateId, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }
        int sort = 1;
        for (Long qid : questionIds) {
            QuestionTemplateItem item = new QuestionTemplateItem();
            item.setTemplateId(templateId);
            item.setQuestionId(qid);
            item.setSort(sort++);
            questionTemplateItemMapper.insert(item);
        }
    }

    /**
     * 构建模板包含的题目列表(含选项),顺序与 items 一致。参考 QuestionManageServiceImpl.detail() 实现。
     */
    private List<Map<String, Object>> buildQuestionList(List<QuestionTemplateItem> items,
                                                        List<Long> questionIds) {
        List<Map<String, Object>> questionList = new ArrayList<>();
        if (questionIds.isEmpty()) {
            return questionList;
        }
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        Map<Long, List<QuestionOption>> optionMap = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, questionIds)
                        .orderByAsc(QuestionOption::getSort))
                .stream().collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        // 批量查询题目分类名/专业名
        List<Long> catIds = questionMap.values().stream().map(Question::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> catNameMap = catIds.isEmpty() ? new HashMap<>() :
                questionCategoryMapper.selectBatchIds(catIds).stream()
                        .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getName, (a, b) -> a));
        List<Long> profIds = questionMap.values().stream().map(Question::getProfessionId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> profNameMap = profIds.isEmpty() ? new HashMap<>() :
                professionMapper.selectBatchIds(profIds).stream()
                        .collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));

        for (QuestionTemplateItem item : items) {
            Question q = questionMap.get(item.getQuestionId());
            if (q == null) {
                continue;
            }
            Map<String, Object> qMap = new HashMap<>();
            qMap.put("id", q.getId());
            qMap.put("type", q.getType());
            qMap.put("categoryId", q.getCategoryId());
            qMap.put("categoryName", catNameMap.get(q.getCategoryId()));
            qMap.put("professionId", q.getProfessionId());
            qMap.put("professionName", profNameMap.get(q.getProfessionId()));
            qMap.put("content", q.getContent());
            qMap.put("analysis", q.getAnalysis());
            qMap.put("correctAnswer", q.getCorrectAnswer());
            qMap.put("score", q.getScore());
            qMap.put("hasImage", q.getHasImage());
            qMap.put("enabled", q.getEnabled());
            qMap.put("createTime", q.getCreateTime());
            qMap.put("sort", item.getSort());
            qMap.put("options", optionMap.getOrDefault(q.getId(), new ArrayList<>()));
            questionList.add(qMap);
        }
        return questionList;
    }

    /**
     * 选中题目的统计结果:存在的题目ID列表(按原顺序)与总分。
     */
    @Data
    @AllArgsConstructor
    private static class TemplateStat {
        private List<Long> questionIds;
        private BigDecimal totalScore;
    }
}
