package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.entity.CooperationApply;
import com.exam.mapper.CooperationApplyMapper;
import com.exam.service.CooperationApplyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CooperationApplyServiceImpl
        extends ServiceImpl<CooperationApplyMapper, CooperationApply>
        implements CooperationApplyService {

    @Override
    public PageResult<CooperationApply> page(Integer page, Integer size,
                                               String unitName, String authCode, Integer status) {
        LambdaQueryWrapper<CooperationApply> wrapper = new LambdaQueryWrapper<CooperationApply>()
                .like(StringUtils.hasText(unitName), CooperationApply::getUnitName, unitName)
                .eq(StringUtils.hasText(authCode), CooperationApply::getAuthCode, authCode)
                .eq(status != null, CooperationApply::getStatus, status)
                .orderByDesc(CooperationApply::getCreateTime);
        Page<CooperationApply> p = this.page(new Page<>(page, size), wrapper);
        return new PageResult<>(p);
    }

    @Override
    public void add(CooperationApply cooperationApply) {
        if (!StringUtils.hasText(cooperationApply.getUnitName())) {
            throw new BusinessException("单位名称不能为空");
        }
        cooperationApply.setId(null);
        if (cooperationApply.getStatus() == null) {
            cooperationApply.setStatus(0);
        }
        this.save(cooperationApply);
    }

    @Override
    public void update(CooperationApply cooperationApply) {
        if (cooperationApply.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        CooperationApply existing = this.getById(cooperationApply.getId());
        if (existing == null) {
            throw new BusinessException("合作申请记录不存在");
        }
        this.updateById(cooperationApply);
    }

    @Override
    public void delete(Long id) {
        this.removeById(id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        this.removeByIds(ids);
    }
}
