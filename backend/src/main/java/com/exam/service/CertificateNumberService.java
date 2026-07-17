package com.exam.service;

import com.exam.entity.Certificate;

import java.time.LocalDate;

/**
 * 证书编号 / 学员编号生成服务
 * 编号规则:前缀字母 + yyyyMMdd(取自颁发日期) + 中段字母 + 5位随机数
 * 全系统唯一,生成后入库,冲突时重试(最多5次)
 */
public interface CertificateNumberService {

    /**
     * 生成唯一证书编号(日期取自颁发日期)
     */
    String generateCertNo(String prefix, String middle);

    /**
     * 生成唯一证书编号(指定日期)
     */
    String generateCertNo(String prefix, String middle, LocalDate issueDate);

    /**
     * 生成唯一学员编号(日期取自颁发日期)
     */
    String generateStudentNo(String prefix, String middle);

    /**
     * 生成唯一学员编号(指定日期)
     */
    String generateStudentNo(String prefix, String middle, LocalDate issueDate);

    /**
     * 生成默认编号(使用证书记录上的前缀/中段,日期取自颁发日期)
     */
    void fillNumbersIfEmpty(Certificate certificate);

    /**
     * 仅在学员编号为空时生成学员编号(使用证书记录上的前缀/中段,日期取自颁发日期)。
     * 证书创建时调用:学员编号在创建时即生成。
     */
    void fillStudentNoIfEmpty(Certificate certificate);

    /**
     * 仅在证书编号为空时生成证书编号(使用证书记录上的前缀/中段,日期取自颁发日期)。
     * 绑定证书模板时调用:证书编号在绑定模板时才生成。
     */
    void fillCertNoIfEmpty(Certificate certificate);
}
