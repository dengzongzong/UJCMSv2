package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.Subject;

public interface SubjectService extends IService<Subject> {

    /**
     * 删除科目（检查引用，有引用则抛 BusinessException）
     */
    void deleteWithCheck(Long id);
}
