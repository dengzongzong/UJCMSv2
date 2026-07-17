package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.PageResult;
import com.exam.entity.FriendlyLink;
import com.exam.mapper.FriendlyLinkMapper;
import com.exam.service.FriendlyLinkService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendlyLinkServiceImpl extends ServiceImpl<FriendlyLinkMapper, FriendlyLink>
        implements FriendlyLinkService {

    @Override
    public PageResult<FriendlyLink> page(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<FriendlyLink> w = new LambdaQueryWrapper<FriendlyLink>()
                .eq(status != null, FriendlyLink::getStatus, status)
                .orderByAsc(FriendlyLink::getSort)
                .orderByDesc(FriendlyLink::getCreateTime);
        return new PageResult<>(this.page(new Page<>(page, size), w));
    }

    @Override
    public List<FriendlyLink> listEnabled() {
        // 学员端一次最多展示 10 个(2 行 5 列),按 sort 升序
        return this.list(new LambdaQueryWrapper<FriendlyLink>()
                .eq(FriendlyLink::getStatus, 1)
                .orderByAsc(FriendlyLink::getSort)
                .last("LIMIT 10"));
    }

    @Override
    public void saveLink(FriendlyLink entity) {
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getId() == null) {
            this.save(entity);
        } else {
            this.updateById(entity);
        }
    }

    @Override
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        this.removeByIds(ids);
    }
}
