package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.entity.Announcement;
import com.exam.mapper.AnnouncementMapper;
import com.exam.service.AnnouncementManageService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统公告管理Service实现
 */
@Service
public class AnnouncementManageServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementManageService {

    @Override
    public PageResult<Announcement> page(Integer page, Integer size, String title, Integer status) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .like(StringUtils.hasText(title), Announcement::getTitle, title)
                .eq(status != null, Announcement::getStatus, status)
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> p = new Page<>(page, size);
        Page<Announcement> result = this.page(p, wrapper);
        return new PageResult<>(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "announcements", allEntries = true)
    public void add(Announcement announcement) {
        if (!StringUtils.hasText(announcement.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        if (announcement.getSort() == null) {
            announcement.setSort(0);
        }
        if (announcement.getPublishTime() == null) {
            announcement.setPublishTime(LocalDateTime.now());
        }
        this.save(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "announcements", allEntries = true)
    public void update(Announcement announcement) {
        if (announcement.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        Announcement existing = this.getById(announcement.getId());
        if (existing == null) {
            throw new BusinessException("公告不存在");
        }
        this.updateById(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "announcements", allEntries = true)
    public void delete(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("公告不存在");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "announcements", allEntries = true)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        this.removeByIds(ids);
    }

    @Override
    @Cacheable(value = "announcements", unless = "#result == null || #result.isEmpty()")
    public List<Announcement> listEnabled() {
        List<Announcement> list = this.list(new LambdaQueryWrapper<Announcement>()
                .select(Announcement::getId, Announcement::getTitle, Announcement::getContent,
                        Announcement::getSort, Announcement::getIsTop,
                        Announcement::getPublishTime, Announcement::getCreateTime)
                .eq(Announcement::getStatus, 1)
                .and(w -> w.isNull(Announcement::getPublishTime).or().le(Announcement::getPublishTime, LocalDateTime.now()))
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getCreateTime));
        list.forEach(this::fillPublishTime);
        return list.stream().collect(Collectors.toMap(
                Announcement::getTitle, a -> a, (a1, a2) -> a1
        )).values().stream().collect(Collectors.toList());
    }

    /** publish_time 为空时用 create_time 兜底,避免前端日期显示为批量导入时间 */
    private void fillPublishTime(Announcement a) {
        if (a.getPublishTime() == null && a.getCreateTime() != null) {
            a.setPublishTime(a.getCreateTime());
        }
    }
}
