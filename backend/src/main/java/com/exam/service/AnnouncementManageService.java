package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.Announcement;

import java.util.List;

/**
 * 系统公告管理Service
 */
public interface AnnouncementManageService extends IService<Announcement> {

    /**
     * 分页查询公告（按标题搜索、按状态筛选）
     */
    PageResult<Announcement> page(Integer page, Integer size, String title, Integer status);

    /**
     * 新增公告
     */
    void add(Announcement announcement);

    /**
     * 修改公告
     */
    void update(Announcement announcement);

    /**
     * 删除公告
     */
    void delete(Long id);

    /**
     * 批量删除公告
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取已显示的公告列表（status=1），按sort升序
     */
    List<Announcement> listEnabled();
}
