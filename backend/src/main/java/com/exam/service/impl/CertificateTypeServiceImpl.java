package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.entity.CertificateType;
import com.exam.mapper.CertificateTypeMapper;
import com.exam.service.CertificateTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 证书类型Service实现
 */
@Service
public class CertificateTypeServiceImpl extends ServiceImpl<CertificateTypeMapper, CertificateType> implements CertificateTypeService {

    @Override
    public List<CertificateType> listAll() {
        return this.list(new LambdaQueryWrapper<CertificateType>()
                .orderByAsc(CertificateType::getSort)
                .orderByAsc(CertificateType::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addType(CertificateType type) {
        if (!StringUtils.hasText(type.getName())) {
            throw new BusinessException("类型名称不能为空");
        }
        // 去重: 同名类型不允许重复添加
        long count = this.count(new LambdaQueryWrapper<CertificateType>()
                .eq(CertificateType::getName, type.getName().trim()));
        if (count > 0) {
            throw new BusinessException("证书类型'" + type.getName() + "'已存在,请勿重复添加");
        }
        if (type.getStatus() == null) {
            type.setStatus(1);
        }
        if (type.getSort() == null) {
            type.setSort(0);
        }
        this.save(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(CertificateType type) {
        if (type.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        if (this.getById(type.getId()) == null) {
            throw new BusinessException("类型不存在");
        }
        this.updateById(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("类型不存在");
        }
        this.removeById(id);
    }
}
