package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.FriendlyLink;

import java.util.List;

public interface FriendlyLinkService extends IService<FriendlyLink> {
    PageResult<FriendlyLink> page(Integer page, Integer size, Integer status);
    List<FriendlyLink> listEnabled();
    void saveLink(FriendlyLink entity);
    void delete(List<Long> ids);
}
