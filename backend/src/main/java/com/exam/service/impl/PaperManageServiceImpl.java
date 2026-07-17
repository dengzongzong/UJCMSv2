package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.AutoGeneratePaperDTO;
import com.exam.dto.PaperDTO;
import com.exam.entity.Exam;
import com.exam.entity.Paper;
import com.exam.entity.PaperQuestion;
import com.exam.entity.Question;
import com.exam.entity.QuestionCategory;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.PaperMapper;
import com.exam.mapper.PaperQuestionMapper;
import com.exam.mapper.QuestionCategoryMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.service.PaperManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaperManageServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperManageService {

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private QuestionCategoryMapper questionCategoryMapper;

    @Override
    public PageResult<Paper> page(Integer page, Integer size, String name, Integer status, Long professionId) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .like(StringUtils.hasText(name), Paper::getName, name)
                .eq(status != null, Paper::getStatus, status)
                .eq(professionId != null, Paper::getProfessionId, professionId)
                .orderByDesc(Paper::getCreateTime);
        Page<Paper> p = new Page<>(page, size);
        Page<Paper> result = this.page(p, wrapper);
        return new PageResult<>(result);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        Paper paper = this.getById(id);
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        // 查询试卷题目关联（按sort排序）
        List<PaperQuestion> paperQuestions = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<PaperQuestion>()
                        .eq(PaperQuestion::getPaperId, id)
                        .orderByAsc(PaperQuestion::getSort));
        List<Long> questionIds = paperQuestions.stream()
                .map(PaperQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionIds.isEmpty() ? new ArrayList<>() :
                questionMapper.selectBatchIds(questionIds);
        // 批量查询题目分类名
        List<Long> categoryIds = questions.stream()
                .map(Question::getCategoryId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? new HashMap<>() :
                questionCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getName));
        questions.forEach(q -> q.setCategoryName(categoryNameMap.get(q.getCategoryId())));
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 组装题目列表（按sort排序），直接是 Question 对象列表
        List<Question> questionList = new ArrayList<>();
        for (PaperQuestion pq : paperQuestions) {
            Question q = questionMap.get(pq.getQuestionId());
            if (q != null) {
                questionList.add(q);
            }
        }

        Map<String, Object> result = new HashMap<>();
        // 把 paper 的所有属性放到顶层
        result.put("id", paper.getId());
        result.put("name", paper.getName());
        result.put("description", paper.getDescription());
        result.put("totalScore", paper.getTotalScore());
        result.put("questionCount", paper.getQuestionCount());
        result.put("status", paper.getStatus());
        result.put("createTime", paper.getCreateTime());
        // questions 直接是 Question 对象列表（按sort排序）
        result.put("questions", questionList);
        // questionIds 供编辑页回显
        result.put("questionIds", questionIds);
        return result;
    }

    @Override
    public List<Paper> list(Long professionId) {
        return this.list(new LambdaQueryWrapper<Paper>()
                .eq(professionId != null, Paper::getProfessionId, professionId)
                .orderByDesc(Paper::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(PaperDTO dto) {
        Paper paper = new Paper();
        paper.setName(dto.getName());
        paper.setDescription(dto.getDescription());
        paper.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        paper.setTotalScore(BigDecimal.ZERO);
        paper.setQuestionCount(0);
        this.save(paper);

        // 创建题目关联并计算总分与题数
        BigDecimal totalScore = savePaperQuestions(paper.getId(), dto.getQuestionIds());
        paper.setQuestionCount(dto.getQuestionIds() == null ? 0 : dto.getQuestionIds().size());
        paper.setTotalScore(totalScore);
        this.updateById(paper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PaperDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        Paper paper = this.getById(dto.getId());
        if (paper == null) {
            throw new BusinessException("试卷不存在");
        }
        paper.setName(dto.getName());
        paper.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            paper.setStatus(dto.getStatus());
        }

        // 先删除旧题目关联再重新创建
        paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, dto.getId()));
        BigDecimal totalScore = savePaperQuestions(dto.getId(), dto.getQuestionIds());
        paper.setQuestionCount(dto.getQuestionIds() == null ? 0 : dto.getQuestionIds().size());
        paper.setTotalScore(totalScore);
        this.updateById(paper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 保留考试记录中的题目关联(paper_question), 以便删除后考试记录仍可查看完整题目
        // 不删除paper_question, 试卷删除后已考考试的记录仍可正常展示题目
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

    /**
     * 保存试卷题目关联，并返回题目总分之和
     */
    private BigDecimal savePaperQuestions(Long paperId, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 查询题目以累加总分
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, BigDecimal> scoreMap = questions.stream()
                .collect(Collectors.toMap(Question::getId,
                        q -> q.getScore() == null ? BigDecimal.ZERO : q.getScore(), (a, b) -> a));

        BigDecimal totalScore = BigDecimal.ZERO;
        int sort = 1;
        for (Long questionId : questionIds) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paperId);
            pq.setQuestionId(questionId);
            pq.setSort(sort++);
            paperQuestionMapper.insert(pq);
            totalScore = totalScore.add(scoreMap.getOrDefault(questionId, BigDecimal.ZERO));
        }
        return totalScore;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoGenerate(AutoGeneratePaperDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new BusinessException("试卷名称不能为空");
        }

        boolean byScore = "byScore".equalsIgnoreCase(dto.getGenerateMode());
        List<Long> questionIds;

        if (byScore) {
            // ============ 按总分组卷模式 ============
            if (dto.getTargetScore() == null || dto.getTargetScore() <= 0) {
                throw new BusinessException("请输入目标总分");
            }
            int targetScore = dto.getTargetScore();
            // 查询所有可用题目(按筛选条件)
            LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<Question>()
                    .eq(Question::getEnabled, 1);
            if (dto.getProfessionId() != null) {
                qw.and(w -> w.isNull(Question::getProfessionId).or().eq(Question::getProfessionId, dto.getProfessionId()));
            }
            if (dto.getCategoryId() != null) {
                qw.eq(Question::getCategoryId, dto.getCategoryId());
            }
            List<Question> pool = questionMapper.selectList(qw);
            if (pool.isEmpty()) {
                throw new BusinessException("题库中没有可用题目,无法组卷");
            }
            // 使用动态规划求解: 从题目池中选取若干题,使分值之和恰好等于目标总分
            // 题目分值可能重复,需要找到一种组合
            questionIds = solveByTargetScore(pool, targetScore);
            if (questionIds == null || questionIds.isEmpty()) {
                throw new BusinessException("题库中无法组合出恰好等于 " + targetScore + " 分的试卷,请调整目标总分或补充题目");
            }
        } else {
            // ============ 按数量组卷模式(原有逻辑) ============
            List<int[]> specs = new ArrayList<>();
            if (dto.getSingleCount() != null && dto.getSingleCount() != 0) specs.add(new int[]{1, dto.getSingleCount()});
            if (dto.getMultiCount() != null && dto.getMultiCount() != 0) specs.add(new int[]{2, dto.getMultiCount()});
            if (dto.getFillCount() != null && dto.getFillCount() != 0) specs.add(new int[]{3, dto.getFillCount()});
            if (dto.getJudgeCount() != null && dto.getJudgeCount() != 0) specs.add(new int[]{4, dto.getJudgeCount()});
            if (dto.getShortCount() != null && dto.getShortCount() != 0) specs.add(new int[]{5, dto.getShortCount()});

            if (specs.isEmpty()) {
                throw new BusinessException("请至少为一种题型设置抽题数量");
            }

            questionIds = new ArrayList<>();
            for (int[] spec : specs) {
                int type = spec[0];
                int count = spec[1];
                boolean allMode = (count == -1);
                LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<Question>()
                        .eq(Question::getType, type)
                        .eq(Question::getEnabled, 1);
                if (dto.getProfessionId() != null) {
                    qw.and(w -> w.isNull(Question::getProfessionId).or().eq(Question::getProfessionId, dto.getProfessionId()));
                }
                if (dto.getCategoryId() != null) {
                    qw.eq(Question::getCategoryId, dto.getCategoryId());
                }
                List<Question> typePool = questionMapper.selectList(qw);
                if (typePool.isEmpty()) {
                    throw new BusinessException("题型[" + typeName(type) + "]题库中没有可用题目,无法组卷");
                }
                int pick;
                if (allMode) {
                    pick = typePool.size();
                } else {
                    Collections.shuffle(typePool);
                    pick = Math.min(count, typePool.size());
                    if (pick < count) {
                        throw new BusinessException("题型[" + typeName(type) + "]题库仅有 " + pick + " 道可用题目,不足 " + count + " 道");
                    }
                }
                for (int i = 0; i < pick; i++) {
                    questionIds.add(typePool.get(i).getId());
                }
            }
        }

        // 创建试卷
        Paper paper = new Paper();
        paper.setName(dto.getName());
        paper.setDescription(dto.getDescription());
        paper.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        paper.setProfessionId(dto.getProfessionId());
        paper.setTotalScore(BigDecimal.ZERO);
        paper.setQuestionCount(0);
        this.save(paper);

        // 创建题目关联并计算总分与题数
        BigDecimal totalScore = savePaperQuestions(paper.getId(), questionIds);
        paper.setQuestionCount(questionIds.size());
        paper.setTotalScore(totalScore);
        this.updateById(paper);
        return paper.getId();
    }

    /**
     * 动态规划: 从题目池中选取若干题,使分值之和恰好等于目标总分
     * 返回选中的题目ID列表;如果无解返回null
     */
    private List<Long> solveByTargetScore(List<Question> pool, int targetScore) {
        int n = pool.size();
        // 提取每题的分值(取整数部分)
        int[] scores = new int[n];
        for (int i = 0; i < n; i++) {
            BigDecimal sc = pool.get(i).getScore();
            scores[i] = sc != null ? sc.intValue() : 0;
        }
        // dp[j] = 是否可以凑出分值 j
        boolean[] dp = new boolean[targetScore + 1];
        // choice[j] = 凑出分值 j 时选中的最后一题索引
        int[] choice = new int[targetScore + 1];
        // prev[j] = 凑出分值 j 之前的前驱分值
        int[] prev = new int[targetScore + 1];
        java.util.Arrays.fill(choice, -1);
        java.util.Arrays.fill(prev, -1);
        dp[0] = true;

        for (int i = 0; i < n; i++) {
            int s = scores[i];
            if (s <= 0) continue;
            // 逆序遍历,确保每题只用一次(0-1背包)
            for (int j = targetScore; j >= s; j--) {
                if (dp[j - s] && !dp[j]) {
                    dp[j] = true;
                    choice[j] = i;
                    prev[j] = j - s;
                }
            }
        }

        if (!dp[targetScore]) {
            return null; // 无解
        }

        // 回溯: 从 targetScore 反向追踪选中的题目
        List<Long> selected = new ArrayList<>();
        int j = targetScore;
        while (j > 0 && choice[j] >= 0) {
            int idx = choice[j];
            selected.add(pool.get(idx).getId());
            j = prev[j];
        }
        // 打乱顺序,避免题目按分值排列
        Collections.shuffle(selected);
        return selected;
    }

    private String typeName(int type) {
        switch (type) {
            case 1: return "单选";
            case 2: return "多选";
            case 3: return "填空";
            case 4: return "判断";
            case 5: return "简答";
            default: return "未知";
        }
    }
}
