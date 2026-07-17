package com.exam.service;

import com.exam.vo.ProfessionVO;

import java.util.List;

/**
 * 专业科目服务
 */
public interface ProfessionSubjectService {

    /**
     * 获取所有启用的专业及科目树
     */
    List<ProfessionVO> listEnabledProfessions();
}
