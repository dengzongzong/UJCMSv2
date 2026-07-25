package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.News;

import java.util.List;

/**
 * 新闻管理Service
 */
public interface NewsManageService extends IService<News> {

    /**
     * 分页查询新闻（按标题搜索、按状态筛选）
     */
    PageResult<News> page(Integer page, Integer size, String title, Integer type, Integer status);

    /**
     * 新增新闻
     */
    void add(News news);

    /**
     * 修改新闻
     */
    void update(News news);

    /**
     * 删除新闻
     */
    void delete(Long id);

    /**
     * 批量删除新闻
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取已显示的新闻列表（status=1），按sort升序
     */
    List<News> listEnabled();

    /**
     * 按类型获取已显示的新闻列表
     */
    List<News> listEnabledByType(Integer type);

    /**
     * 获取单条新闻详情(含content,仅返回status=1)
     */
    News getPublicDetail(Long id);
}
