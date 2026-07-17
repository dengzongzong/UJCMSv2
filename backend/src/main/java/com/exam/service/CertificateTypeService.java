package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exam.entity.CertificateType;

import java.util.List;

/**
 * 证书类型Service
 */
public interface CertificateTypeService extends IService<CertificateType> {

    List<CertificateType> listAll();

    void addType(CertificateType type);

    void updateType(CertificateType type);

    void deleteType(Long id);
}
