package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.QuestionCategory;

import java.util.List;

public interface QuestionCategoryService extends IService<QuestionCategory> {

    /**
     * 查询全部题目分类（按sort排序）
     */
    List<QuestionCategory> listAll();

    /**
     * 删除题目分类（检查引用，有引用则抛 BusinessException）
     */
    void deleteWithCheck(Long id);
}
