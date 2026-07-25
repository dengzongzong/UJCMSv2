package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.HomepageSection;

import java.util.List;

/**
 * 首页内容板块Service(政策法规/信息公开)
 */
public interface HomepageSectionService extends IService<HomepageSection> {

    /**
     * 分页查询
     */
    PageResult<HomepageSection> page(Integer page, Integer size, String title, Integer type, Integer status);

    /**
     * 新增
     */
    void add(HomepageSection section);

    /**
     * 修改
     */
    void update(HomepageSection section);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 批量删除
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取已显示的板块列表(按type筛选)
     */
    List<HomepageSection> listEnabled(Integer type);

    /**
     * 获取单条板块详情(含content,仅返回status=1)
     */
    HomepageSection getPublicDetail(Long id);
}
