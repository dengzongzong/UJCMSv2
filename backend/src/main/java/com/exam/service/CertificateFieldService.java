package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.dto.CertificateFieldDTO;
import com.exam.entity.CertificateField;

import java.util.List;

public interface CertificateFieldService extends IService<CertificateField> {

    /**
     * 列出所有字段(按 sort 升序)
     */
    List<CertificateField> listAll();

    /**
     * 列出系统字段
     */
    List<CertificateField> listSystem();

    /**
     * 列出用户自定义字段
     */
    List<CertificateField> listCustom();

    /**
     * 新增字段(用户自定义)
     */
    void add(CertificateFieldDTO dto);

    /**
     * 更新字段(系统字段只允许改显示名/默认/必填)
     */
    void update(CertificateFieldDTO dto);

    /**
     * 删除字段(系统字段不允许删除)
     */
    void delete(Long id);
}
