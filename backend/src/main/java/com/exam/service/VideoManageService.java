package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.dto.VideoDTO;
import com.exam.entity.Video;
import com.exam.vo.AdminVideoVO;

import java.util.List;

public interface VideoManageService extends IService<Video> {

    /**
     * 分页查询视频（按名称搜索、按分类/专业筛选）
     */
    PageResult<AdminVideoVO> page(Integer page, Integer size, String name, Long categoryId, Long professionId);

    /**
     * 视频详情
     */
    AdminVideoVO detail(Long id);

    /**
     * 新增视频
     */
    void add(VideoDTO dto);

    /**
     * 编辑视频
     */
    void update(VideoDTO dto);

    /**
     * 删除视频（检查是否被引用）
     */
    void delete(Long id);

    /**
     * 批量删除视频（级联清理学生开通记录）
     */
    void batchDelete(List<Long> ids);

    /**
     * 按播放量排序返回视频列表
     */
    List<AdminVideoVO> sortByPlayCount();
}
