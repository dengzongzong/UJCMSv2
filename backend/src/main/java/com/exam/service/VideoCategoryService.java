package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.VideoCategory;

import java.util.List;

public interface VideoCategoryService extends IService<VideoCategory> {

    /**
     * 查询全部视频分类（按sort排序）
     */
    List<VideoCategory> listAll();

    /**
     * 删除视频分类（检查引用，有引用则抛 BusinessException）
     */
    void deleteWithCheck(Long id);
}
