package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.CooperationApply;

import java.util.List;

public interface CooperationApplyService extends IService<CooperationApply> {

    /**
     * 分页查询合作申请
     *
     * @param page     页码
     * @param size     每页条数
     * @param unitName 单位名称(模糊查询,可选)
     * @param authCode 授权管理编号(精确查询,可选)
     * @param status   状态筛选(可选)
     */
    PageResult<CooperationApply> page(Integer page, Integer size, String unitName, String authCode, Integer status);

    /** 新增合作申请 */
    void add(CooperationApply cooperationApply);

    /** 修改合作申请 */
    void update(CooperationApply cooperationApply);

    /** 删除合作申请 */
    void delete(Long id);

    /** 批量删除合作申请 */
    void batchDelete(List<Long> ids);
}
