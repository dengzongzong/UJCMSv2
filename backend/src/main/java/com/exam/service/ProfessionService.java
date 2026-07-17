package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.Profession;

import java.util.List;
import java.util.Map;

public interface ProfessionService extends IService<Profession> {

    /**
     * 查询专业列表（含科目）
     */
    List<Map<String, Object>> listWithSubjects();

    /**
     * 删除专业,删除前检查是否被引用
     */
    void deleteWithCheck(Long id);
}
