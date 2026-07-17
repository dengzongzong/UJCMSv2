package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateNumberConfig;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.CertificateNumberConfigMapper;
import com.exam.service.CertificateNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CertificateNumberServiceImpl implements CertificateNumberService {

    private static final int MAX_RETRY = 5;
    private static final String DATE_PATTERN = "yyyyMMdd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    @Autowired
    private CertificateMapper certificateMapper;
    @Autowired
    private CertificateNumberConfigMapper numberConfigMapper;

    @Override
    public String generateCertNo(String prefix, String middle) {
        return generateCertNo(prefix, middle, null);
    }

    @Override
    public String generateCertNo(String prefix, String middle, LocalDate issueDate) {
        return generateUnique(prefix, middle, "cert_no", issueDate);
    }

    @Override
    public String generateStudentNo(String prefix, String middle) {
        return generateStudentNo(prefix, middle, null);
    }

    @Override
    public String generateStudentNo(String prefix, String middle, LocalDate issueDate) {
        return generateUnique(prefix, middle, "student_no", issueDate);
    }

    /**
     * 编号规则: 前缀字母 + yyyyMMdd(取自颁发日期,为空则取当天) + 中段字母 + 10000~99999随机数(系统唯一)
     * 举例: ZGZH20201020M12345
     *
     * @param column uk 列名(cert_no 或 student_no)
     * @param issueDate 颁发日期(为null时使用当天日期)
     */
    private String generateUnique(String prefix, String middle, String column, LocalDate issueDate) {
        String safePrefix = prefix == null ? "" : prefix;
        String safeMiddle = middle == null ? "" : middle;
        // 日期取自颁发日期,若没填写则使用当天
        LocalDate date = issueDate != null ? issueDate : LocalDate.now();
        String dateStr = date.format(DATE_FORMATTER);
        for (int i = 0; i < MAX_RETRY; i++) {
            int rnd = ThreadLocalRandom.current().nextInt(10000, 100000);
            String no = safePrefix + dateStr + safeMiddle + rnd;
            if (!exists(no, column)) {
                return no;
            }
        }
        throw new com.exam.common.BusinessException("编号生成失败(已达最大重试次数),请重试");
    }

    private boolean exists(String no, String column) {
        LambdaQueryWrapper<Certificate> w = new LambdaQueryWrapper<>();
        if ("cert_no".equals(column)) {
            w.eq(Certificate::getCertNo, no);
        } else if ("student_no".equals(column)) {
            w.eq(Certificate::getStudentNo, no);
        } else {
            throw new IllegalArgumentException("unsupported column: " + column);
        }
        return certificateMapper.selectCount(w) > 0;
    }

    @Override
    public void fillNumbersIfEmpty(Certificate cert) {
        CertificateNumberConfig config = getConfig();
        if (cert.getCertNo() == null || cert.getCertNo().isEmpty()) {
            String prefix = StringUtils.hasText(cert.getCertNoPrefix()) ? cert.getCertNoPrefix() : (config != null ? config.getCertNoPrefix() : null);
            String middle = StringUtils.hasText(cert.getCertNoMiddle()) ? cert.getCertNoMiddle() : (config != null ? config.getCertNoMiddle() : null);
            cert.setCertNoPrefix(prefix);
            cert.setCertNoMiddle(middle);
            cert.setCertNo(generateCertNo(prefix, middle, cert.getIssueDate()));
        }
        if (cert.getStudentNo() == null || cert.getStudentNo().isEmpty()) {
            String prefix = StringUtils.hasText(cert.getStudentNoPrefix()) ? cert.getStudentNoPrefix() : (config != null ? config.getStudentNoPrefix() : null);
            String middle = StringUtils.hasText(cert.getStudentNoMiddle()) ? cert.getStudentNoMiddle() : (config != null ? config.getStudentNoMiddle() : null);
            cert.setStudentNoPrefix(prefix);
            cert.setStudentNoMiddle(middle);
            cert.setStudentNo(generateStudentNo(prefix, middle, cert.getIssueDate()));
        }
    }

    @Override
    public void fillStudentNoIfEmpty(Certificate cert) {
        if (cert.getStudentNo() != null && !cert.getStudentNo().isEmpty()) {
            return;
        }
        CertificateNumberConfig config = getConfig();
        String prefix = StringUtils.hasText(cert.getStudentNoPrefix()) ? cert.getStudentNoPrefix()
                : (config != null ? config.getStudentNoPrefix() : null);
        String middle = StringUtils.hasText(cert.getStudentNoMiddle()) ? cert.getStudentNoMiddle()
                : (config != null ? config.getStudentNoMiddle() : null);
        cert.setStudentNoPrefix(prefix);
        cert.setStudentNoMiddle(middle);
        cert.setStudentNo(generateStudentNo(prefix, middle, cert.getIssueDate()));
    }

    @Override
    public void fillCertNoIfEmpty(Certificate cert) {
        if (cert.getCertNo() != null && !cert.getCertNo().isEmpty()) {
            return;
        }
        CertificateNumberConfig config = getConfig();
        String prefix = StringUtils.hasText(cert.getCertNoPrefix()) ? cert.getCertNoPrefix()
                : (config != null ? config.getCertNoPrefix() : null);
        String middle = StringUtils.hasText(cert.getCertNoMiddle()) ? cert.getCertNoMiddle()
                : (config != null ? config.getCertNoMiddle() : null);
        cert.setCertNoPrefix(prefix);
        cert.setCertNoMiddle(middle);
        cert.setCertNo(generateCertNo(prefix, middle, cert.getIssueDate()));
    }

    private CertificateNumberConfig getConfig() {
        List<CertificateNumberConfig> list = numberConfigMapper.selectList(null);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 从身份证号提取性别(第17位奇数=男 偶数=女)
     */
    public static Integer extractGenderFromIdCard(String idCard) {
        if (idCard == null || idCard.length() < 17) {
            return null;
        }
        char c = idCard.charAt(16);
        if (!Character.isDigit(c)) {
            return null;
        }
        return (c - '0') % 2 == 1 ? 1 : 2;
    }
}
