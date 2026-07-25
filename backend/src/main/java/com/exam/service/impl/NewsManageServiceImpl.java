package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.entity.News;
import com.exam.mapper.NewsMapper;
import com.exam.service.NewsManageService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 新闻管理Service实现
 */
@Service
public class NewsManageServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsManageService {

    @Override
    public PageResult<News> page(Integer page, Integer size, String title, Integer type, Integer status) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<News>()
                .like(StringUtils.hasText(title), News::getTitle, title)
                .eq(type != null, News::getType, type)
                .eq(status != null, News::getStatus, status)
                .orderByDesc(News::getIsTop)
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getCreateTime);
        Page<News> p = new Page<>(page, size);
        Page<News> result = this.page(p, wrapper);
        return new PageResult<>(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"newsList", "newsListByType"}, allEntries = true)
    public void add(News news) {
        if (!StringUtils.hasText(news.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (news.getStatus() == null) {
            news.setStatus(1);
        }
        if (news.getSort() == null) {
            news.setSort(0);
        }
        this.save(news);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"newsList", "newsListByType"}, allEntries = true)
    public void update(News news) {
        if (news.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        News existing = this.getById(news.getId());
        if (existing == null) {
            throw new BusinessException("新闻不存在");
        }
        this.updateById(news);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"newsList", "newsListByType"}, allEntries = true)
    public void delete(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("新闻不存在");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"newsList", "newsListByType"}, allEntries = true)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        this.removeByIds(ids);
    }

    @Override
    @Cacheable(value = "newsList", unless = "#result == null || #result.isEmpty()")
    public List<News> listEnabled() {
        List<News> list = this.list(new LambdaQueryWrapper<News>()
                .select(News::getId, News::getTitle, News::getCoverUrl, News::getType,
                        News::getSort, News::getIsTop, News::getPublishTime, News::getCreateTime)
                .eq(News::getStatus, 1)
                .and(w -> w.isNull(News::getPublishTime).or().le(News::getPublishTime, LocalDateTime.now()))
                .orderByDesc(News::getIsTop)
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getCreateTime));
        return dedupByTitle(list);
    }

    @Override
    @Cacheable(value = "newsListByType", key = "#type", unless = "#result == null || #result.isEmpty()")
    public List<News> listEnabledByType(Integer type) {
        List<News> list = this.list(new LambdaQueryWrapper<News>()
                .select(News::getId, News::getTitle, News::getCoverUrl, News::getType,
                        News::getSort, News::getIsTop, News::getPublishTime, News::getCreateTime)
                .eq(News::getStatus, 1)
                .eq(type != null, News::getType, type)
                .and(w -> w.isNull(News::getPublishTime).or().le(News::getPublishTime, LocalDateTime.now()))
                .orderByDesc(News::getIsTop)
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getCreateTime));
        return dedupByTitle(list);
    }

    private List<News> dedupByTitle(List<News> list) {
        return list.stream().collect(Collectors.toMap(
                News::getTitle, n -> n, (n1, n2) -> n1
        )).values().stream().collect(Collectors.toList());
    }
}
