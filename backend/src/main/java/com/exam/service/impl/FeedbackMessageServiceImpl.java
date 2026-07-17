package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.PageResult;
import com.exam.entity.FeedbackMessage;
import com.exam.mapper.FeedbackMessageMapper;
import com.exam.service.FeedbackMessageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class FeedbackMessageServiceImpl
        extends ServiceImpl<FeedbackMessageMapper, FeedbackMessage>
        implements FeedbackMessageService {

    @Override
    public PageResult<FeedbackMessage> page(Integer page, Integer size, String type,
                                            Integer status, String keyword) {
        LambdaQueryWrapper<FeedbackMessage> w = new LambdaQueryWrapper<FeedbackMessage>()
                .eq(StringUtils.hasText(type), FeedbackMessage::getType, type)
                .eq(status != null, FeedbackMessage::getStatus, status)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(FeedbackMessage::getOrgName, keyword)
                        .or().like(FeedbackMessage::getContactName, keyword)
                        .or().like(FeedbackMessage::getPhone, keyword)
                        .or().like(FeedbackMessage::getContent, keyword))
                .orderByDesc(FeedbackMessage::getCreateTime);
        Page<FeedbackMessage> p = this.page(new Page<>(page, size), w);
        return new PageResult<>(p);
    }

    @Override
    public void markHandled(Long id, String remark) {
        FeedbackMessage m = this.getById(id);
        if (m == null) {
            return;
        }
        m.setStatus(1);
        if (remark != null) {
            m.setRemark(remark);
        }
        this.updateById(m);
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        this.removeByIds(ids);
    }
}
