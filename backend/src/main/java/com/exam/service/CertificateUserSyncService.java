package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.CertificateUser;
import com.exam.entity.Profession;
import com.exam.entity.Student;
import com.exam.entity.StudentProfession;
import com.exam.mapper.CertificateUserMapper;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.StudentProfessionMapper;
import com.exam.service.impl.CertificateNumberServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 证书用户同步服务
 * <p>
 * 将"用户管理"中的学员(student)数据,按身份证号同步到 certificate_user(证书用户)。
 * - 定时:每 10 分钟全量同步一次(以 id_card + profession_id 为业务唯一键 upsert)
 * - 实时:注册/新增/导入学员时立即同步该学员
 * - 按专业维度:同一学生有多个专业时,每个专业创建独立的证书用户记录
 * </p>
 */
@Service
public class CertificateUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(CertificateUserSyncService.class);

    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private CertificateUserMapper certificateUserMapper;
    @Autowired
    private ProfessionMapper professionMapper;
    @Autowired
    private StudentProfessionMapper studentProfessionMapper;

    /**
     * 定时全量同步:每 10 分钟执行一次
     * <p>删除了证书用户里面的数据,也会自动从学生管理再次同步过来</p>
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void scheduledSyncAll() {
        syncAll();
    }

    /**
     * 全量同步所有学员到证书用户(按专业维度:每个专业一条记录)
     *
     * @return 同步的记录数
     */
    public int syncAll() {
        List<Student> students = studentMapper.selectList(null);
        if (students.isEmpty()) {
            return 0;
        }
        Map<Long, String> professionNameMap = loadProfessionNameMap();

        int count = 0;
        for (Student s : students) {
            try {
                count += upsertPerProfession(s, professionNameMap);
            } catch (Exception e) {
                log.warn("同步学员到证书用户失败 studentId={}, idCard={}, phone={}: {}",
                        s.getId(), s.getIdCard(), s.getPhone(), e.getMessage());
            }
        }
        log.info("证书用户定时同步完成,共处理 {} 名学员", count);
        return count;
    }

    /**
     * 同步单个学员(注册/新增/导入/更新时调用)
     * <p>按专业维度:同一学生有多个专业时,每个专业创建独立的证书用户记录</p>
     */
    public void syncStudent(Student student) {
        if (student == null) {
            return;
        }
        Map<Long, String> professionNameMap = loadProfessionNameMap();
        try {
            upsertPerProfession(student, professionNameMap);
        } catch (Exception e) {
            log.warn("同步学员到证书用户失败 studentId={}, idCard={}, phone={}: {}",
                    student.getId(), student.getIdCard(), student.getPhone(), e.getMessage());
        }
    }

    /**
     * 从证书导入数据直接同步到证书用户(不依赖学生表)
     */
    public void syncFromCertificateData(String name, String idCard, Integer gender, String professionName) {
        if (!StringUtils.hasText(idCard)) return;
        String trimmedIdCard = idCard.trim();
        // 按身份证号 + 专业名称查找
        CertificateUser existing = certificateUserMapper.selectOne(
                new LambdaQueryWrapper<CertificateUser>()
                        .eq(CertificateUser::getIdCard, trimmedIdCard)
                        .eq(StringUtils.hasText(professionName), CertificateUser::getProfessionName, professionName)
                        .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            CertificateUser cu = new CertificateUser();
            cu.setName(name);
            cu.setIdCard(trimmedIdCard);
            cu.setGender(gender);
            cu.setProfessionName(professionName);
            cu.setSyncTime(now);
            certificateUserMapper.insert(cu);
        } else {
            if (StringUtils.hasText(name)) existing.setName(name);
            if (gender != null) existing.setGender(gender);
            if (StringUtils.hasText(professionName)) existing.setProfessionName(professionName);
            existing.setSyncTime(now);
            certificateUserMapper.updateById(existing);
        }
    }

    /**
     * 按专业维度 upsert:同一学生有多个专业时,每个专业创建独立的证书用户记录
     * <p>如果学生增加了新专业,会新增一条证书用户记录;
     * 如果学生减少了专业,不会删除已有记录(保留历史)</p>
     *
     * @return 同步的记录数
     */
    private int upsertPerProfession(Student student, Map<Long, String> professionNameMap) {
        String idCard = StringUtils.hasText(student.getIdCard()) ? student.getIdCard().trim() : null;
        String phone = StringUtils.hasText(student.getPhone()) ? student.getPhone().trim() : null;
        Integer gender = idCard != null ? CertificateNumberServiceImpl.extractGenderFromIdCard(idCard) : null;
        LocalDateTime now = LocalDateTime.now();

        // 从 student_profession 表查询所有专业
        List<StudentProfession> sps = student.getId() == null
                ? Collections.emptyList()
                : studentProfessionMapper.selectByStudentId(student.getId());

        if (sps.isEmpty()) {
            // 没有专业关联:只 upsert 一条通用记录(用 professionId=null)
            upsertSingle(student, idCard, phone, null, null, gender, now);
            return 1;
        }

        int count = 0;
        for (StudentProfession sp : sps) {
            Long professionId = sp.getProfessionId();
            String professionName = sp.getProfessionName();
            if (professionName == null && professionId != null) {
                professionName = professionNameMap.get(professionId);
            }
            upsertSingle(student, idCard, phone, professionId, professionName, gender, now);
            count++;
        }
        return count;
    }

    /**
     * 按身份证号+专业维度 upsert 单条记录
     */
    private void upsertSingle(Student student, String idCard, String phone,
                              Long professionId, String professionName,
                              Integer gender, LocalDateTime now) {
        // 查找已存在记录: 优先按身份证号+professionId
        CertificateUser existing = null;
        if (idCard != null) {
            if (professionId != null) {
                existing = certificateUserMapper.selectOne(
                        new LambdaQueryWrapper<CertificateUser>()
                                .eq(CertificateUser::getIdCard, idCard)
                                .eq(CertificateUser::getProfessionId, professionId)
                                .last("LIMIT 1"));
            } else {
                existing = certificateUserMapper.selectOne(
                        new LambdaQueryWrapper<CertificateUser>()
                                .eq(CertificateUser::getIdCard, idCard)
                                .isNull(CertificateUser::getProfessionId)
                                .last("LIMIT 1"));
            }
        }
        if (existing == null && phone != null) {
            if (professionId != null) {
                existing = certificateUserMapper.selectOne(
                        new LambdaQueryWrapper<CertificateUser>()
                                .eq(CertificateUser::getPhone, phone)
                                .eq(CertificateUser::getProfessionId, professionId)
                                .last("LIMIT 1"));
            } else {
                existing = certificateUserMapper.selectOne(
                        new LambdaQueryWrapper<CertificateUser>()
                                .eq(CertificateUser::getPhone, phone)
                                .isNull(CertificateUser::getProfessionId)
                                .last("LIMIT 1"));
            }
        }
        if (existing == null && student.getId() != null) {
            if (professionId != null) {
                existing = certificateUserMapper.selectOne(
                        new LambdaQueryWrapper<CertificateUser>()
                                .eq(CertificateUser::getStudentId, student.getId())
                                .eq(CertificateUser::getProfessionId, professionId)
                                .last("LIMIT 1"));
            } else {
                existing = certificateUserMapper.selectOne(
                        new LambdaQueryWrapper<CertificateUser>()
                                .eq(CertificateUser::getStudentId, student.getId())
                                .isNull(CertificateUser::getProfessionId)
                                .last("LIMIT 1"));
            }
        }

        if (existing == null) {
            CertificateUser cu = new CertificateUser();
            cu.setStudentId(student.getId());
            cu.setName(student.getName());
            cu.setIdCard(idCard);
            cu.setPhone(phone);
            cu.setProfessionName(professionName);
            cu.setProfessionId(professionId);
            cu.setCertType(student.getCertType());
            cu.setGender(gender);
            cu.setSyncTime(now);
            certificateUserMapper.insert(cu);
        } else {
            existing.setStudentId(student.getId());
            existing.setName(student.getName());
            if (idCard != null) {
                existing.setIdCard(idCard);
            }
            existing.setPhone(phone);
            existing.setProfessionName(professionName);
            if (professionId != null) {
                existing.setProfessionId(professionId);
            }
            // 同步证书类型(学生表有值时覆盖,为空时保留原值)
            if (StringUtils.hasText(student.getCertType())) {
                existing.setCertType(student.getCertType());
            }
            if (gender != null) {
                existing.setGender(gender);
            }
            existing.setSyncTime(now);
            certificateUserMapper.updateById(existing);
        }
    }

    private Map<Long, String> loadProfessionNameMap() {
        List<Profession> professions = professionMapper.selectList(null);
        return professions.stream().collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));
    }
}
