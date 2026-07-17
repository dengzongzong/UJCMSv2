package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.common.PageResult;
import com.exam.entity.FeedbackMessage;

import java.util.List;

public interface FeedbackMessageService extends IService<FeedbackMessage> {

    /**
     * 分页查询留言
     *
     * @param page    页码
     * @param size    每页条数
     * @param type    类型(可选)
     * @param status  状态(可选)
     * @param keyword 关键字(单位名/联系人/电话/内容)
     */
    PageResult<FeedbackMessage> page(Integer page, Integer size, String type, Integer status, String keyword);

    /** 标记为已处理 */
    void markHandled(Long id, String remark);

    /** 批量删除 */
    void deleteBatch(List<Long> ids);
}
