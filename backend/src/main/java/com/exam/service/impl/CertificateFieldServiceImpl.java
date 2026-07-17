package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.dto.CertificateFieldDTO;
import com.exam.entity.CertificateField;
import com.exam.mapper.CertificateFieldMapper;
import com.exam.service.CertificateFieldService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CertificateFieldServiceImpl extends ServiceImpl<CertificateFieldMapper, CertificateField>
        implements CertificateFieldService {

    @Override
    public List<CertificateField> listAll() {
        return this.list(new LambdaQueryWrapper<CertificateField>().orderByAsc(CertificateField::getSort));
    }

    @Override
    public List<CertificateField> listSystem() {
        return this.list(new LambdaQueryWrapper<CertificateField>()
                .eq(CertificateField::getIsSystem, 1)
                .orderByAsc(CertificateField::getSort));
    }

    @Override
    public List<CertificateField> listCustom() {
        return this.list(new LambdaQueryWrapper<CertificateField>()
                .eq(CertificateField::getIsSystem, 0)
                .orderByAsc(CertificateField::getSort));
    }

    @Override
    public void add(CertificateFieldDTO dto) {
        // key 唯一性
        if (this.count(new LambdaQueryWrapper<CertificateField>()
                .eq(CertificateField::getFieldKey, dto.getFieldKey())) > 0) {
            throw new BusinessException("字段键已存在: " + dto.getFieldKey());
        }
        CertificateField f = new CertificateField();
        BeanUtils.copyProperties(dto, f);
        f.setId(null);
        f.setIsSystem(0);
        if (f.getSort() == null) f.setSort(99);
        if (f.getRequired() == null) f.setRequired(0);
        this.save(f);
    }

    @Override
    public void update(CertificateFieldDTO dto) {
        if (dto.getId() == null) throw new BusinessException("id 不能为空");
        CertificateField exist = this.getById(dto.getId());
        if (exist == null) throw new BusinessException("字段不存在");
        if (exist.getIsSystem() != null && exist.getIsSystem() == 1) {
            // 系统字段:不允许改 fieldKey
            if (!exist.getFieldKey().equals(dto.getFieldKey())) {
                throw new BusinessException("系统内置字段不允许修改字段键");
            }
        }
        CertificateField f = new CertificateField();
        BeanUtils.copyProperties(dto, f);
        this.updateById(f);
    }

    @Override
    public void delete(Long id) {
        CertificateField f = this.getById(id);
        if (f == null) throw new BusinessException("字段不存在");
        if (f.getIsSystem() != null && f.getIsSystem() == 1) {
            throw new BusinessException("系统内置字段不允许删除");
        }
        this.removeById(id);
    }
}
