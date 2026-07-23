package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.entity.HomepageSection;
import com.exam.mapper.HomepageSectionMapper;
import com.exam.service.HomepageSectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 首页内容板块Service实现
 */
@Service
public class HomepageSectionServiceImpl extends ServiceImpl<HomepageSectionMapper, HomepageSection> implements HomepageSectionService {

    @Override
    public PageResult<HomepageSection> page(Integer page, Integer size, String title, Integer type, Integer status) {
        LambdaQueryWrapper<HomepageSection> wrapper = new LambdaQueryWrapper<HomepageSection>()
                .like(StringUtils.hasText(title), HomepageSection::getTitle, title)
                .eq(type != null, HomepageSection::getType, type)
                .eq(status != null, HomepageSection::getStatus, status)
                .orderByAsc(HomepageSection::getSort)
                .orderByDesc(HomepageSection::getCreateTime);
        Page<HomepageSection> p = new Page<>(page, size);
        Page<HomepageSection> result = this.page(p, wrapper);
        return new PageResult<>(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(HomepageSection section) {
        if (!StringUtils.hasText(section.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (section.getType() == null) {
            throw new BusinessException("类型不能为空");
        }
        if (section.getStatus() == null) {
            section.setStatus(1);
        }
        if (section.getSort() == null) {
            section.setSort(0);
        }
        this.save(section);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(HomepageSection section) {
        if (section.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        if (this.getById(section.getId()) == null) {
            throw new BusinessException("记录不存在");
        }
        this.updateById(section);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("记录不存在");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        this.removeByIds(ids);
    }

    @Override
    public List<HomepageSection> listEnabled(Integer type) {
        LambdaQueryWrapper<HomepageSection> wrapper = new LambdaQueryWrapper<HomepageSection>()
                .eq(HomepageSection::getStatus, 1);
        if (type != null) {
            wrapper.eq(HomepageSection::getType, type);
        }
        wrapper.orderByAsc(HomepageSection::getSort)
               .orderByDesc(HomepageSection::getCreateTime);
        List<HomepageSection> list = this.list(wrapper);
        return list.stream().collect(Collectors.toMap(
                HomepageSection::getTitle, h -> h, (h1, h2) -> h1
        )).values().stream().collect(Collectors.toList());
    }
}
