package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.dto.CertificateTemplateDTO;
import com.exam.entity.CertificateTemplate;
import com.exam.vo.CertificateTemplateVO;

import java.util.List;

public interface CertificateTemplateService extends IService<CertificateTemplate> {

    /**
     * 模板列表
     */
    List<CertificateTemplate> listAll();

    /**
     * 模板详情(含 fields)
     */
    CertificateTemplateVO detail(Long id);

    /**
     * 新建 / 编辑模板(同时保存 fields)
     */
    void save(CertificateTemplateDTO dto);

    /**
     * 删除模板
     */
    void delete(Long id);

    /**
     * 设置默认模板(同时清掉其它默认)
     */
    void setDefault(Long id);
}
