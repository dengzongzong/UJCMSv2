package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.entity.Question;
import com.exam.entity.QuestionCategory;
import com.exam.mapper.QuestionCategoryMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.service.QuestionCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionCategoryServiceImpl extends ServiceImpl<QuestionCategoryMapper, QuestionCategory> implements QuestionCategoryService {

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public List<QuestionCategory> listAll() {
        return this.list(new LambdaQueryWrapper<QuestionCategory>()
                .orderByAsc(QuestionCategory::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithCheck(Long id) {
        List<String> refs = new ArrayList<>();
        if (questionMapper.selectCount(new LambdaQueryWrapper<Question>().eq(Question::getCategoryId, id)) > 0)
            refs.add("题目");
        if (!refs.isEmpty()) {
            throw new BusinessException("该题目分类已被引用,不能删除，引用方: " + String.join("、", refs));
        }
        this.removeById(id);
    }
}
