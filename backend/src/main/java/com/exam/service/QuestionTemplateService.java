package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.QuestionTemplate;
import java.util.List;
import java.util.Map;

public interface QuestionTemplateService extends IService<QuestionTemplate> {
    PageResult<QuestionTemplate> page(Integer page, Integer size, String name, Long categoryId, Long professionId);
    List<Map<String, Object>> detail(Long id);
    void create(String name, String description, Long categoryId, Long professionId, List<Long> questionIds);
    void update(Long id, String name, String description, Long categoryId, Long professionId, List<Long> questionIds);
    void delete(Long id);
    List<QuestionTemplate> listAll();
}
