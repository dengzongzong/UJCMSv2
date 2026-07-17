package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.dto.CertificateTemplateDTO;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateTemplate;
import com.exam.entity.CertificateTemplateField;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.CertificateTemplateFieldMapper;
import com.exam.mapper.CertificateTemplateMapper;
import com.exam.service.CertificateTemplateService;
import com.exam.vo.CertificateTemplateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CertificateTemplateServiceImpl extends ServiceImpl<CertificateTemplateMapper, CertificateTemplate>
        implements CertificateTemplateService {

    @Autowired
    private CertificateTemplateFieldMapper fieldMapper;
    @Autowired
    private CertificateMapper certificateMapper;

    @Override
    public List<CertificateTemplate> listAll() {
        return this.list(new LambdaQueryWrapper<CertificateTemplate>().orderByDesc(CertificateTemplate::getCreateTime));
    }

    @Override
    public CertificateTemplateVO detail(Long id) {
        CertificateTemplate t = this.getById(id);
        if (t == null) throw new BusinessException("模板不存在");
        return toVo(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CertificateTemplateDTO dto) {
        // 1. 校验
        if (!StringUtils.hasText(dto.getName())) throw new BusinessException("模板名称不能为空");
        // 2. 新增/更新 template
        CertificateTemplate t = new CertificateTemplate();
        t.setId(dto.getId());
        t.setName(dto.getName());
        t.setBgImageUrl(dto.getBgImageUrl());
        t.setBgWidth(dto.getBgWidth());
        t.setBgHeight(dto.getBgHeight());
        t.setIsDefault(dto.getIsDefault() == null ? 0 : dto.getIsDefault());
        t.setRemark(dto.getRemark());
        t.setStampUrl(dto.getStampUrl());
        t.setStampX(dto.getStampX());
        t.setStampY(dto.getStampY());
        t.setStampWidth(dto.getStampWidth());
        t.setStampRotation(dto.getStampRotation());
        t.setStampOpacity(dto.getStampOpacity());
        LocalDateTime now = LocalDateTime.now();
        t.setUpdateTime(now);
        if (dto.getId() == null) {
            t.setCreateTime(now);
            this.save(t);
        } else {
            this.updateById(t);
            // 删除旧 fields
            fieldMapper.delete(new LambdaQueryWrapper<CertificateTemplateField>()
                    .eq(CertificateTemplateField::getTemplateId, dto.getId()));
        }
        Long templateId = dto.getId() == null ? t.getId() : dto.getId();
        // 3. 批量插入 fields
        if (dto.getFields() != null) {
            for (CertificateTemplateField f : dto.getFields()) {
                f.setId(null);
                f.setTemplateId(templateId);
                f.setCreateTime(now);
                f.setUpdateTime(now);
                fieldMapper.insert(f);
            }
        }
        // 4. 默认模板: 取消其它默认
        if (t.getIsDefault() != null && t.getIsDefault() == 1) {
            this.update(new LambdaUpdateWrapper<CertificateTemplate>()
                    .ne(CertificateTemplate::getId, templateId)
                    .set(CertificateTemplate::getIsDefault, 0));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CertificateTemplate t = this.getById(id);
        if (t == null) throw new BusinessException("模板不存在");
        if (t.getIsDefault() != null && t.getIsDefault() == 1) {
            throw new BusinessException("默认模板不能删除,请先将其他模板设为默认");
        }
        // 兜底:全表检查防止并发
        long otherCount = this.count(new LambdaQueryWrapper<CertificateTemplate>().ne(CertificateTemplate::getId, id));
        if (otherCount == 0) {
            throw new BusinessException("系统至少需保留一个模板");
        }
        // 检查是否有证书引用了该模板
        long certCount = certificateMapper.selectCount(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getTemplateId, id));
        if (certCount > 0) {
            throw new BusinessException("该模板已被 " + certCount + " 张证书引用,不能删除");
        }
        this.removeById(id);
        fieldMapper.delete(new LambdaQueryWrapper<CertificateTemplateField>()
                .eq(CertificateTemplateField::getTemplateId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        this.update(new LambdaUpdateWrapper<CertificateTemplate>()
                .set(CertificateTemplate::getIsDefault, 0));
        this.update(new LambdaUpdateWrapper<CertificateTemplate>()
                .eq(CertificateTemplate::getId, id)
                .set(CertificateTemplate::getIsDefault, 1)
                .set(CertificateTemplate::getUpdateTime, LocalDateTime.now()));
    }

    private CertificateTemplateVO toVo(CertificateTemplate t) {
        CertificateTemplateVO vo = new CertificateTemplateVO();
        org.springframework.beans.BeanUtils.copyProperties(t, vo);
        vo.setCreateTime(t.getCreateTime() == null ? null : t.getCreateTime().toString());
        vo.setUpdateTime(t.getUpdateTime() == null ? null : t.getUpdateTime().toString());
        vo.setFields(fieldMapper.selectList(new LambdaQueryWrapper<CertificateTemplateField>()
                .eq(CertificateTemplateField::getTemplateId, t.getId())
                .orderByAsc(CertificateTemplateField::getSort)));
        return vo;
    }
}
