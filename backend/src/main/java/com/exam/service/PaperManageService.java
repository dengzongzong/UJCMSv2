package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.AutoGeneratePaperDTO;
import com.exam.dto.PaperDTO;
import com.exam.entity.Paper;

import java.util.List;
import java.util.Map;

public interface PaperManageService extends IService<Paper> {

    /**
     * 分页查询试卷（支持按名称、状态、专业搜索）
     */
    PageResult<Paper> page(Integer page, Integer size, String name, Integer status, Long professionId);

    /**
     * 试卷详情（含题目列表）
     */
    Map<String, Object> detail(Long id);

    /**
     * 获取所有已发布试卷（供考试创建时选择，支持按专业过滤）
     */
    List<Paper> list(Long professionId);

    /**
     * 新增试卷（同时创建 PaperQuestion 关联，更新 questionCount 和 totalScore）
     */
    void add(PaperDTO dto);

    /**
     * 编辑试卷（先删除旧关联再重新创建）
     */
    void update(PaperDTO dto);

    /**
     * 删除试卷（同时删除 PaperQuestion 关联）
     */
    void delete(Long id);

    /**
     * 批量删除试卷（级联删除题目关联）
     */
    void batchDelete(List<Long> ids);

    /**
     * 一键抽题组卷: 按题型指定数量,从题库随机抽取可用题目生成试卷
     *
     * @return 新创建的试卷ID
     */
    Long autoGenerate(AutoGeneratePaperDTO dto);
}
