package com.exam.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.dto.CertificateDTO;
import com.exam.dto.OpenCourseDTO;
import com.exam.dto.OpenExamDTO;
import com.exam.dto.StudentImportDTO;
import com.exam.dto.StudentSearchDTO;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.StudentManageService;
import com.exam.service.StudentNumberService;
import com.exam.service.CertificateUserSyncService;
import com.exam.service.AdminScopeService;
import com.exam.vo.StudentImportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class StudentManageServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentManageService {

    @Autowired
    private StudentCourseMapper studentCourseMapper;
    @Autowired
    private StudentExamMapper studentExamMapper;
    @Autowired
    private StudentVideoMapper studentVideoMapper;
    @Autowired
    private VideoStudyRecordMapper videoStudyRecordMapper;
    @Autowired
    private ExamRecordMapper examRecordMapper;
    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private SystemSettingMapper systemSettingMapper;
    @Autowired
    private ProfessionMapper professionMapper;
    @Autowired
    private StudentNumberService studentNumberService;
    @Autowired
    private CertificateUserSyncService certificateUserSyncService;
    @Autowired
    private com.exam.service.CertificateService certificateService;
    @Autowired
    private StudentProfessionMapper studentProfessionMapper;
    @Autowired
    private ExamAnswerMapper examAnswerMapper;
    @Autowired
    private CertificateMapper certificateMapper;
    @Autowired
    private AdminScopeService adminScopeService;

    @Override
    public PageResult<Student> page(StudentSearchDTO dto) {
        int page = dto.getPage() == null ? 1 : dto.getPage();
        int size = dto.getSize() == null ? 10 : dto.getSize();
        // 精确显示条数: 仅返回最新N条(覆盖分页参数)
        if (dto.getExactCount() != null && dto.getExactCount() > 0) {
            page = 1;
            size = dto.getExactCount();
        }
        // 子管理员证书类型范围过滤
        List<String> scopeCertTypes = adminScopeService.scopeCertTypes();
        if (scopeCertTypes != null) {
            if (scopeCertTypes.isEmpty()) {
                return new PageResult<>(new Page<>());
            }
            // 应用在下方 wrapper 上
        }
        // 如果按 professionId 筛选，先查关联表获取 studentId 列表
        List<Long> studentIdsByProfession = null;
        if (dto.getProfessionId() != null) {
            List<StudentProfession> sps = studentProfessionMapper.selectList(
                    new LambdaQueryWrapper<StudentProfession>()
                            .eq(StudentProfession::getProfessionId, dto.getProfessionId()));
            studentIdsByProfession = sps.stream().map(StudentProfession::getStudentId).collect(Collectors.toList());
            if (studentIdsByProfession.isEmpty()) {
                return new PageResult<>(new Page<>());
            }
        }
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .like(StringUtils.hasText(dto.getPhone()), Student::getPhone, dto.getPhone())
                .eq(dto.getStatus() != null, Student::getStatus, dto.getStatus())
                .in(scopeCertTypes != null, Student::getCertType, scopeCertTypes)
                .orderByDesc(Student::getCreateTime)
                .orderByDesc(Student::getId);
        if (studentIdsByProfession != null) {
            wrapper.in(Student::getId, studentIdsByProfession);
        }
        // keyword 搜索：姓名 / 手机号 / 学号 / 身份证
        if (StringUtils.hasText(dto.getKeyword())) {
            String kw = dto.getKeyword();
            wrapper.and(w -> w.like(Student::getName, kw)
                    .or().like(Student::getPhone, kw)
                    .or().like(Student::getStudentNo, kw)
                    .or().like(Student::getIdCard, kw));
        }
        // 身份证号精确/模糊查询
        if (StringUtils.hasText(dto.getIdCard())) {
            wrapper.like(Student::getIdCard, dto.getIdCard());
        }
        // 注册时间范围查询(支持小时级别: yyyy-MM-dd HH:mm:ss)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.hasText(dto.getRegisterTimeStart())) {
            try {
                LocalDateTime start = LocalDateTime.parse(dto.getRegisterTimeStart(), dtf);
                wrapper.ge(Student::getRegisterTime, start);
            } catch (Exception e) {
                // 兼容纯日期格式 yyyy-MM-dd
                LocalDateTime start = LocalDate.parse(dto.getRegisterTimeStart(), df).atStartOfDay();
                wrapper.ge(Student::getRegisterTime, start);
            }
        }
        if (StringUtils.hasText(dto.getRegisterTimeEnd())) {
            try {
                LocalDateTime end = LocalDateTime.parse(dto.getRegisterTimeEnd(), dtf);
                wrapper.le(Student::getRegisterTime, end);
            } catch (Exception e) {
                LocalDateTime end = LocalDate.parse(dto.getRegisterTimeEnd(), df).atTime(23, 59, 59);
                wrapper.le(Student::getRegisterTime, end);
            }
        }
        Page<Student> p = new Page<>(page, size);
        Page<Student> result = this.page(p, wrapper);
        result.getRecords().forEach(s -> s.setPassword(null));
        // 加载多专业信息
        for (Student s : result.getRecords()) {
            loadStudentProfessions(s);
        }
        return new PageResult<>(result);
    }

    private void loadStudentProfessions(Student student) {
        if (student == null || student.getId() == null) return;
        List<StudentProfession> sps = studentProfessionMapper.selectByStudentId(student.getId());
        if (sps.isEmpty()) return;

        List<Long> professionIds = sps.stream()
                .map(StudentProfession::getProfessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        student.setProfessionIds(professionIds);

        // professionName 标注了 @TableField(exist=false),MyBatis-Plus 的 @Select 查询
        // 可能不会自动将 JOIN 出的 profession_name 列映射到该字段。
        // 兜底:优先用 JOIN 查出的名称,为 null 时按 professionId 查 profession 表补全。
        Map<Long, String> professionNameMap = null;
        List<String> professionNames = new ArrayList<>();
        for (StudentProfession sp : sps) {
            String name = sp.getProfessionName();
            if (name == null && sp.getProfessionId() != null) {
                if (professionNameMap == null) {
                    professionNameMap = loadProfessionNameMap();
                }
                name = professionNameMap.get(sp.getProfessionId());
            }
            if (name != null) {
                professionNames.add(name);
            }
        }
        student.setProfessionNames(professionNames);
        if (!professionNames.isEmpty()) {
            student.setProfessionName(String.join(",", professionNames));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudent(Student student) {
        if (student == null) {
            throw new BusinessException("学生信息不能为空");
        }
        // 子管理员只能创建自己授权证书类型的学生
        adminScopeService.checkCertType(student.getCertType());
        // 手机号为选填字段:为空时存 null(避免 uk_phone 唯一约束在多个空串上冲突)
        if (!StringUtils.hasText(student.getPhone())) {
            student.setPhone(null);
        }
        // 身份证号为选填字段:为空时存 null(避免 uk_id_card 唯一约束在多个空串上冲突)
        if (StringUtils.hasText(student.getIdCard())) {
            student.setIdCard(student.getIdCard().trim());
        } else {
            student.setIdCard(null);
        }
        // 检查手机号或身份证号是否已存在(仅在手机号非空时校验手机号唯一性)
        LambdaQueryWrapper<Student> existWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(student.getPhone())) {
            existWrapper.eq(Student::getPhone, student.getPhone());
        }
        if (StringUtils.hasText(student.getIdCard())) {
            if (StringUtils.hasText(student.getPhone())) {
                existWrapper.or().eq(Student::getIdCard, student.getIdCard());
            } else {
                existWrapper.eq(Student::getIdCard, student.getIdCard());
            }
        }
        if (StringUtils.hasText(student.getPhone()) || StringUtils.hasText(student.getIdCard())) {
            List<Student> existing = this.list(existWrapper);
            if (!existing.isEmpty()) {
                Student existStudent = existing.get(0);
                // 按身份证号匹配,判断专业是否重复
                if (StringUtils.hasText(student.getIdCard()) && student.getIdCard().equals(existStudent.getIdCard())) {
                    // 获取要导入的专业列表
                    List<Long> newProfessionIds = student.getProfessionIds();
                    if (newProfessionIds == null || newProfessionIds.isEmpty()) {
                        if (student.getProfessionId() != null) {
                            newProfessionIds = Collections.singletonList(student.getProfessionId());
                        }
                    }
                    if (newProfessionIds != null && !newProfessionIds.isEmpty()) {
                        // 查询已有学生的专业列表
                        List<StudentProfession> existSps = studentProfessionMapper.selectList(
                                new LambdaQueryWrapper<StudentProfession>()
                                        .eq(StudentProfession::getStudentId, existStudent.getId()));
                        Set<Long> existProfIds = new HashSet<>();
                        for (StudentProfession sp : existSps) {
                            existProfIds.add(sp.getProfessionId());
                        }
                        // 检查新专业是否都已存在
                        boolean allProfessionsExist = true;
                        List<Long> professionsToMerge = new ArrayList<>();
                        for (Long profId : newProfessionIds) {
                            if (profId != null && !existProfIds.contains(profId)) {
                                allProfessionsExist = false;
                                professionsToMerge.add(profId);
                            }
                        }
                        if (allProfessionsExist && !existProfIds.isEmpty()) {
                            throw new BusinessException("该身份证号「" + student.getIdCard() + "」已存在相同专业的学生数据,属于重复数据");
                        }
                        // 专业不同,合并新专业到已有学生
                        Map<Long, String> professionNameMap = loadProfessionNameMap();
                        for (Long profId : professionsToMerge) {
                            StudentProfession sp = new StudentProfession();
                            sp.setStudentId(existStudent.getId());
                            sp.setProfessionId(profId);
                            studentProfessionMapper.insert(sp);
                            // 同步创建证书记录
                            CertificateDTO certDto = new CertificateDTO();
                            certDto.setName(existStudent.getName());
                            certDto.setIdCard(existStudent.getIdCard());
                            certDto.setProfession(professionNameMap.get(profId));
                            if (StringUtils.hasText(existStudent.getCertType())) {
                                certDto.setCertType(existStudent.getCertType().trim());
                            }
                            try {
                                certificateService.add(certDto);
                            } catch (Exception e) {
                                // 证书创建失败不影响合并
                            }
                        }
                        // 同步到证书用户
                        certificateUserSyncService.syncStudent(existStudent);
                        // 合并成功,不继续执行新建逻辑
                        return;
                    }
                    // 没有专业信息,直接报重复
                    throw new BusinessException("该身份证号「" + student.getIdCard() + "」已存在,属于重复数据");
                }
                // 手机号重复但身份证不同
                throw new BusinessException("手机号码「" + student.getPhone() + "」已存在,属于重复数据");
            }
        }
        // 密码加密（使用 hutool 的 BCrypt）
        String rawPassword = StringUtils.hasText(student.getPassword()) ? student.getPassword() : "123456";
        student.setPassword(BCrypt.hashpw(rawPassword));
        // 学号未传则自动生成：STU + 年月日 + 随机数
        // 学号留空时自动分配(STU + yyyyMMdd + 4位随机),由 StudentNumberService 统一实现,避免重复
        studentNumberService.ensureStudentNo(student);
        // 昵称默认值
        if (!StringUtils.hasText(student.getNickname())) {
            String name = StringUtils.hasText(student.getName()) ? student.getName()
                    : (StringUtils.hasText(student.getPhone())
                        ? "学员" + student.getPhone().substring(student.getPhone().length() - 4)
                        : "学员");
            student.setNickname(name);
        }
        // 状态默认正常
        if (student.getStatus() == null) {
            student.setStatus(1);
        }
        student.setRegisterTime(LocalDateTime.now());
        // 如果有单专业字段且多专业字段为空，做兼容转换
        if (student.getProfessionId() != null && (student.getProfessionIds() == null || student.getProfessionIds().isEmpty())) {
            student.setProfessionIds(Collections.singletonList(student.getProfessionId()));
        }
        this.save(student);
        // 保存多专业关联
        List<Long> professionIds = student.getProfessionIds();
        if (professionIds != null && !professionIds.isEmpty()) {
            for (Long professionId : professionIds) {
                if (professionId == null) continue;
                StudentProfession sp = new StudentProfession();
                sp.setStudentId(student.getId());
                sp.setProfessionId(professionId);
                studentProfessionMapper.insert(sp);
            }
            // 设置主专业字段为第一个
            student.setProfessionId(professionIds.get(0));
            this.updateById(student);
        }
        // 同步到证书用户(实时)
        certificateUserSyncService.syncStudent(student);
        // 按每个专业创建证书记录
        createCertificatesForStudent(student, professionIds);
    }

    private void createCertificatesForStudent(Student student, List<Long> professionIds) {
        if (professionIds == null || professionIds.isEmpty()) {
            return;
        }
        Map<Long, String> professionNameMap = loadProfessionNameMap();
        for (Long professionId : professionIds) {
            if (professionId == null) continue;
            CertificateDTO certDto = new CertificateDTO();
            certDto.setName(student.getName());
            certDto.setIdCard(student.getIdCard());
            // 不复用学生登录学号作为证书学员编号,避免一人多证时 certificate.uk_student_no 冲突;
            // 证书学员编号由 certificateService.add() 按"编号配置"规则独立生成。
            certDto.setProfession(professionNameMap.get(professionId));
            // 传递证书类型,用于自动绑定同名模板
            if (StringUtils.hasText(student.getCertType())) {
                certDto.setCertType(student.getCertType().trim());
            }
            try {
                certificateService.add(certDto);
                // 去重时 add 返回 false,这里不关心返回值
            } catch (Exception e) {
                // 证书创建失败不影响主流程
            }
        }
    }

    private Map<Long, String> loadProfessionNameMap() {
        List<Profession> professions = professionMapper.selectList(null);
        return professions.stream().collect(Collectors.toMap(Profession::getId, Profession::getName, (a, b) -> a));
    }

    /**
     * 从 Excel 导入行更新已有证书的成绩字段
     * 当学生已存在且专业相同时,虽然不新增证书记录,但应更新成绩
     */
    private void updateCertificateScoresFromRow(com.exam.dto.CertificateImportRow row, String idCard, String profession) {
        String theoryScore = row.getTheoryScore();
        String practicalScore = row.getPracticalScore();
        String comprehensiveEval = row.getComprehensiveEvaluation();
        // 至少有一个成绩字段有值才更新
        if (!StringUtils.hasText(theoryScore) && !StringUtils.hasText(practicalScore) && !StringUtils.hasText(comprehensiveEval)) {
            return;
        }
        try {
            List<Certificate> certs = certificateMapper.selectList(
                    new LambdaQueryWrapper<Certificate>()
                            .eq(Certificate::getIdCard, idCard)
                            .eq(Certificate::getProfession, profession));
            for (Certificate cert : certs) {
                boolean changed = false;
                if (StringUtils.hasText(theoryScore)) {
                    cert.setTheoryScore(theoryScore.trim());
                    changed = true;
                }
                if (StringUtils.hasText(practicalScore)) {
                    cert.setPracticalScore(practicalScore.trim());
                    changed = true;
                }
                if (StringUtils.hasText(comprehensiveEval)) {
                    cert.setComprehensiveEvaluation(comprehensiveEval.trim());
                    changed = true;
                }
                if (changed) {
                    // 同时更新 extra_json 中的成绩字段
                    if (StringUtils.hasText(cert.getExtraJson())) {
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> extra = mapper.readValue(cert.getExtraJson(), java.util.Map.class);
                            if (StringUtils.hasText(theoryScore)) extra.put("theoryScore", theoryScore.trim());
                            if (StringUtils.hasText(practicalScore)) extra.put("practicalScore", practicalScore.trim());
                            if (StringUtils.hasText(comprehensiveEval)) extra.put("comprehensiveEvaluation", comprehensiveEval.trim());
                            cert.setExtraJson(mapper.writeValueAsString(extra));
                        } catch (Exception e) {
                            // extra_json 解析失败,不影响主表成绩更新
                        }
                    }
                    certificateMapper.updateById(cert);
                }
            }
        } catch (Exception e) {
            // 成绩更新失败不影响导入主流程
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStudent(Student student) {
        if (student == null || student.getId() == null) {
            throw new BusinessException("学生ID不能为空");
        }
        Student exist = this.getById(student.getId());
        if (exist == null) {
            throw new BusinessException("学生不存在");
        }
        // 子管理员只能操作自己授权证书类型的学生
        adminScopeService.checkCertType(exist.getCertType());
        // 证书类型变更时也要校验新类型在授权范围内
        if (student.getCertType() != null && !student.getCertType().equals(exist.getCertType())) {
            adminScopeService.checkCertType(student.getCertType());
        }
        // 保存旧值,用于同步到证书表(匹配旧姓名+旧身份证号的证书记录)
        String oldName = exist.getName();
        String oldIdCard = exist.getIdCard();
        // 1) 手机号唯一性校验(改手机号时要确认不和其他学员冲突)
        if (StringUtils.hasText(student.getPhone())
                && !student.getPhone().equals(exist.getPhone())) {
            long dup = this.count(new LambdaQueryWrapper<Student>()
                    .eq(Student::getPhone, student.getPhone())
                    .ne(Student::getId, student.getId()));
            if (dup > 0) {
                throw new BusinessException("手机号已存在");
            }
            exist.setPhone(student.getPhone());
        }
        // 2) 基础信息(允许空字符串 - 不修改)
        if (StringUtils.hasText(student.getName())) {
            exist.setName(student.getName());
        }
        if (StringUtils.hasText(student.getNickname())) {
            exist.setNickname(student.getNickname());
        }
        // 身份证号(允许空字符串清空 / 非空更新;空串转 null 避免 uk_id_card 唯一约束冲突)
        if (student.getIdCard() != null) {
            exist.setIdCard(StringUtils.hasText(student.getIdCard()) ? student.getIdCard().trim() : null);
        }
        // 证书类型(允许设置为空)
        if (student.getCertType() != null) {
            exist.setCertType(student.getCertType());
        }
        if (student.getProfessionId() != null) {
            exist.setProfessionId(student.getProfessionId());
        }
        if (StringUtils.hasText(student.getAvatar())) {
            exist.setAvatar(student.getAvatar());
        }
        if (student.getStatus() != null) {
            exist.setStatus(student.getStatus());
        }
        // 3) 学号: 只有当传入新值时更新(避免空值覆盖已有学号)
        if (StringUtils.hasText(student.getStudentNo())) {
            exist.setStudentNo(student.getStudentNo());
        }
        // 4) 密码: 仅当 password 字段非空才修改(前端可选填)
        if (StringUtils.hasText(student.getPassword())) {
            exist.setPassword(BCrypt.hashpw(student.getPassword()));
        }
        this.updateById(exist);
        // 5) 多专业关联: 如果传入 professionIds 则先删后插
        if (student.getProfessionIds() != null) {
            studentProfessionMapper.delete(new LambdaQueryWrapper<StudentProfession>()
                    .eq(StudentProfession::getStudentId, student.getId()));
            if (!student.getProfessionIds().isEmpty()) {
                for (Long professionId : student.getProfessionIds()) {
                    if (professionId == null) continue;
                    StudentProfession sp = new StudentProfession();
                    sp.setStudentId(student.getId());
                    sp.setProfessionId(professionId);
                    studentProfessionMapper.insert(sp);
                }
                // 同步更新主专业字段为第一个
                exist.setProfessionId(student.getProfessionIds().get(0));
                this.updateById(exist);
            }
        }
        // 同步到证书用户表(更新姓名/身份证号/手机号/专业等)
        certificateUserSyncService.syncStudent(exist);
        // 同步到证书表(certificate): 学生管理修改属性后,证书管理界面也同步更新
        syncStudentToCertificateTable(exist, oldName, oldIdCard);
    }

    /**
     * 学生管理修改属性后,同步到证书表(certificate)
     * 按旧姓名+旧身份证号匹配证书记录:
     * 1. 更新姓名/身份证号
     * 2. 学生专业变更时,更新证书的专业(只改不删)
     * 3. 为学生新增的专业创建证书记录
     */
    private void syncStudentToCertificateTable(Student student, String oldName, String oldIdCard) {
        if (oldName == null || oldIdCard == null) return;
        // 查找匹配的证书记录
        List<Certificate> certs = certificateMapper.selectList(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getName, oldName)
                        .eq(Certificate::getIdCard, oldIdCard));
        // 获取学生当前的专业名称列表(用 LambdaQueryWrapper,避免 JOIN 映射问题)
        Map<Long, String> professionNameMap = loadProfessionNameMap();
        List<StudentProfession> sps = studentProfessionMapper.selectList(
                new LambdaQueryWrapper<StudentProfession>()
                        .eq(StudentProfession::getStudentId, student.getId()));
        Set<String> currentProfessions = new LinkedHashSet<>();
        for (StudentProfession sp : sps) {
            if (sp.getProfessionId() != null) {
                String name = professionNameMap.get(sp.getProfessionId());
                if (StringUtils.hasText(name)) currentProfessions.add(name.trim());
            }
        }
        // 兜底: student.professionId
        if (currentProfessions.isEmpty() && student.getProfessionId() != null) {
            String name = professionNameMap.get(student.getProfessionId());
            if (StringUtils.hasText(name)) currentProfessions.add(name.trim());
        }

        String newName = student.getName() != null ? student.getName() : oldName;
        String newIdCard = student.getIdCard() != null ? student.getIdCard() : oldIdCard;

        // 1. 更新现有证书的姓名/身份证号; 专业不匹配时改为学生当前专业(只改不删)
        Set<String> existingProfessions = new HashSet<>();
        // 收集需要改成的新专业(学生有但证书中没有的专业,按顺序取)
        List<String> newProfessionsToAdd = new ArrayList<>();
        for (String prof : currentProfessions) {
            newProfessionsToAdd.add(prof);
        }
        int newProfIdx = 0;
        for (Certificate cert : certs) {
            // 更新姓名
            cert.setName(newName);
            // 更新身份证号
            cert.setIdCard(newIdCard);
            // 检查专业
            String certProf = cert.getProfession();
            if (!StringUtils.hasText(certProf)) {
                // 专业为空 → 改为学生当前第一个专业(只改不删)
                if (!newProfessionsToAdd.isEmpty() && newProfIdx < newProfessionsToAdd.size()) {
                    cert.setProfession(newProfessionsToAdd.get(newProfIdx));
                    existingProfessions.add(newProfessionsToAdd.get(newProfIdx));
                    newProfIdx++;
                }
                certificateMapper.updateById(cert);
            } else if (!currentProfessions.contains(certProf.trim())) {
                // 专业不在学生当前专业列表中 → 改为学生当前专业(只改不删)
                // 找一个还没被占用的学生专业来替换
                String replacementProf = null;
                for (String prof : currentProfessions) {
                    if (!existingProfessions.contains(prof)) {
                        replacementProf = prof;
                        break;
                    }
                }
                if (replacementProf != null) {
                    cert.setProfession(replacementProf);
                    existingProfessions.add(replacementProf);
                } else {
                    // 所有学生专业都已被证书占用,保留原专业(不删不改)
                    existingProfessions.add(certProf.trim());
                }
                certificateMapper.updateById(cert);
            } else {
                existingProfessions.add(certProf.trim());
                certificateMapper.updateById(cert);
            }
        }

        // 2. 为学生新增的专业创建证书记录(当前专业中有但证书表没有的)
        if (!currentProfessions.isEmpty()) {
            for (String profName : currentProfessions) {
                if (!existingProfessions.contains(profName)) {
                    // 该专业在证书表中不存在,创建
                    CertificateDTO certDto = new CertificateDTO();
                    certDto.setName(newName);
                    certDto.setIdCard(newIdCard);
                    certDto.setProfession(profName);
                    if (StringUtils.hasText(student.getCertType())) {
                        certDto.setCertType(student.getCertType().trim());
                    }
                    try {
                        certificateService.add(certDto);
                    } catch (Exception e) {
                        // 证书创建失败不影响主流程
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStudent(Long id) {
        if (id == null) {
            throw new BusinessException("学生ID不能为空");
        }
        Student exist = this.getById(id);
        if (exist == null) {
            throw new BusinessException("学生不存在");
        }
        // 子管理员只能删除自己授权证书类型的学生
        adminScopeService.checkCertType(exist.getCertType());
        // 级联清理: 该学生所有关联表
        studentCourseMapper.delete(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getStudentId, id));
        studentExamMapper.delete(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getStudentId, id));
        studentVideoMapper.delete(new LambdaQueryWrapper<StudentVideo>()
                .eq(StudentVideo::getStudentId, id));
        videoStudyRecordMapper.delete(new LambdaQueryWrapper<VideoStudyRecord>()
                .eq(VideoStudyRecord::getStudentId, id));
        // 先清理答题明细(通过考试记录ID关联),再删除考试记录
        List<Long> recordIds = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, id))
                .stream().map(ExamRecord::getId).collect(Collectors.toList());
        if (!recordIds.isEmpty()) {
            examAnswerMapper.delete(new LambdaQueryWrapper<ExamAnswer>()
                    .in(ExamAnswer::getRecordId, recordIds));
        }
        examRecordMapper.delete(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, id));
        wrongQuestionMapper.delete(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getStudentId, id));
        // 级联删除学生专业关联
        studentProfessionMapper.delete(new LambdaQueryWrapper<StudentProfession>()
                .eq(StudentProfession::getStudentId, id));
        // 注意: 不删除证书数据(证书数据不允许通过学生删除来清理)
        // 最后删除学生主体
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteStudents(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            try {
                deleteStudent(id);
            } catch (BusinessException e) {
                // 学生不存在则跳过,继续删除其余
            }
        }
    }

    @Override
    public Map<String, Object> detail(Long id) {
        Student student = this.getById(id);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        // 子管理员只能查看自己授权证书类型的学生
        adminScopeService.checkCertType(student.getCertType());
        student.setPassword(null);
        loadStudentProfessions(student);
        // 已开通课程
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getStudentId, id));
        List<Long> courseIds = studentCourses.stream().map(StudentCourse::getCourseId).collect(Collectors.toList());
        List<Course> courses = courseIds.isEmpty() ? new ArrayList<>() :
                courseMapper.selectBatchIds(courseIds);
        // 已开通考试
        List<StudentExam> studentExams = studentExamMapper.selectList(
                new LambdaQueryWrapper<StudentExam>().eq(StudentExam::getStudentId, id));
        List<Long> examIds = studentExams.stream().map(StudentExam::getExamId).collect(Collectors.toList());
        List<Exam> exams = examIds.isEmpty() ? new ArrayList<>() :
                examMapper.selectBatchIds(examIds);

        Map<String, Object> result = new HashMap<>();
        result.put("student", student);
        result.put("courses", courses);
        result.put("exams", exams);
        return result;
    }

    @Override
    public void freeze(Long id) {
        Student student = this.getById(id);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        // 子管理员只能冻结自己授权证书类型的学生
        adminScopeService.checkCertType(student.getCertType());
        // 切换状态
        student.setStatus(student.getStatus() == 1 ? 0 : 1);
        this.updateById(student);
    }

    @Override
    public Map<String, Object> courses(Long id) {
        Student student = this.getById(id);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        // 全部课程
        List<Course> allCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .orderByDesc(Course::getCreateTime));
        // 已开通课程ID
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getStudentId, id));
        Set<Long> openedIds = studentCourses.stream().map(StudentCourse::getCourseId).collect(Collectors.toSet());

        List<Course> opened = new ArrayList<>();
        List<Course> notOpened = new ArrayList<>();
        for (Course course : allCourses) {
            if (openedIds.contains(course.getId())) {
                opened.add(course);
            } else {
                notOpened.add(course);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("opened", opened);
        result.put("notOpened", notOpened);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openCourses(OpenCourseDTO dto) {
        if (dto.getStudentId() == null || dto.getCourseIds() == null || dto.getCourseIds().isEmpty()) {
            return;
        }
        // 查询已存在的
        List<StudentCourse> existing = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getStudentId, dto.getStudentId())
                        .in(StudentCourse::getCourseId, dto.getCourseIds()));
        Set<Long> existingIds = existing.stream().map(StudentCourse::getCourseId).collect(Collectors.toSet());
        for (Long courseId : dto.getCourseIds()) {
            if (existingIds.contains(courseId)) {
                continue;
            }
            StudentCourse sc = new StudentCourse();
            sc.setStudentId(dto.getStudentId());
            sc.setCourseId(courseId);
            studentCourseMapper.insert(sc);
        }
    }

    @Override
    public Map<String, Object> exams(Long id) {
        Student student = this.getById(id);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        List<Exam> allExams = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .orderByDesc(Exam::getCreateTime));
        List<StudentExam> studentExams = studentExamMapper.selectList(
                new LambdaQueryWrapper<StudentExam>().eq(StudentExam::getStudentId, id));
        Set<Long> openedIds = studentExams.stream().map(StudentExam::getExamId).collect(Collectors.toSet());

        List<Exam> opened = new ArrayList<>();
        List<Exam> notOpened = new ArrayList<>();
        for (Exam exam : allExams) {
            if (openedIds.contains(exam.getId())) {
                opened.add(exam);
            } else {
                notOpened.add(exam);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("opened", opened);
        result.put("notOpened", notOpened);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openExams(OpenExamDTO dto) {
        if (dto.getStudentId() == null || dto.getExamIds() == null || dto.getExamIds().isEmpty()) {
            return;
        }
        List<StudentExam> existing = studentExamMapper.selectList(
                new LambdaQueryWrapper<StudentExam>()
                        .eq(StudentExam::getStudentId, dto.getStudentId())
                        .in(StudentExam::getExamId, dto.getExamIds()));
        Set<Long> existingIds = existing.stream().map(StudentExam::getExamId).collect(Collectors.toSet());
        for (Long examId : dto.getExamIds()) {
            if (existingIds.contains(examId)) {
                continue;
            }
            StudentExam se = new StudentExam();
            se.setStudentId(dto.getStudentId());
            se.setExamId(examId);
            studentExamMapper.insert(se);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeCourse(Long studentId, Long courseId) {
        studentCourseMapper.delete(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getCourseId, courseId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeExam(Long studentId, Long examId) {
        studentExamMapper.delete(new LambdaQueryWrapper<StudentExam>()
                .eq(StudentExam::getStudentId, studentId)
                .eq(StudentExam::getExamId, examId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importStudents(StudentImportDTO dto) {
        if (dto.getPhones() == null || dto.getPhones().isEmpty()) {
            throw new BusinessException("手机号列表不能为空");
        }
        // 获取系统默认密码
        SystemSetting setting = systemSettingMapper.selectOne(
                new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, "default_password"));
        String defaultPassword = (setting != null && StringUtils.hasText(setting.getSettingValue()))
                ? setting.getSettingValue() : "123456";
        String encodedPassword = BCrypt.hashpw(defaultPassword);

        // 查询已存在的手机号
        List<Student> existing = this.list(new LambdaQueryWrapper<Student>()
                .in(Student::getPhone, dto.getPhones()));
        Set<String> existingPhones = existing.stream().map(Student::getPhone).collect(Collectors.toSet());

        List<Student> toInsert = new ArrayList<>();
        for (String phone : dto.getPhones()) {
            if (!StringUtils.hasText(phone) || existingPhones.contains(phone)) {
                continue;
            }
            Student student = new Student();
            student.setPhone(phone);
            student.setPassword(encodedPassword);
            student.setNickname("学员" + phone.substring(phone.length() - 4));
            student.setStatus(1);
            student.setRegisterTime(LocalDateTime.now());
            toInsert.add(student);
        }
        if (!toInsert.isEmpty()) {
            this.saveBatch(toInsert);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importStudentsFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        // 使用学生管理专用的25列模板解析(独立于证书管理20列模板)
        com.exam.dto.CertificateImportResult parseResult = certificateService.parseStudentExcel(file);
        List<com.exam.dto.CertificateImportRow> validRows = parseResult.getPendingRows();
        List<com.exam.dto.CertificateImportRow> failedRows = parseResult.getFailedRows();

        if (validRows == null || validRows.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", 0);
            result.put("failCount", failedRows == null ? 0 : failedRows.size());
            result.put("failList", failedRows == null ? Collections.emptyList() :
                    failedRows.stream().map(r -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", r.getName());
                        m.put("idCard", r.getIdCard());
                        m.put("reason", r.getError());
                        return m;
                    }).collect(Collectors.toList()));
            return result;
        }

        // 获取系统默认密码
        SystemSetting setting = systemSettingMapper.selectOne(
                new LambdaQueryWrapper<SystemSetting>().eq(SystemSetting::getSettingKey, "default_password"));
        String defaultPassword = (setting != null && StringUtils.hasText(setting.getSettingValue()))
                ? setting.getSettingValue() : "123456";
        String encodedPassword = BCrypt.hashpw(defaultPassword);

        // 预加载所有专业，构建 name -> id 映射
        List<Profession> professions = professionMapper.selectList(null);
        Map<String, Long> professionNameMap = professions.stream()
                .collect(Collectors.toMap(Profession::getName, Profession::getId, (a, b) -> a));

        // 收集所有身份证号，查询已存在的学生
        List<String> idCards = validRows.stream()
                .map(com.exam.dto.CertificateImportRow::getIdCard)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        Map<String, Student> existByIdCard = new HashMap<>();
        if (!idCards.isEmpty()) {
            List<Student> existing = this.list(new LambdaQueryWrapper<Student>()
                    .in(Student::getIdCard, idCards));
            for (Student s : existing) {
                if (StringUtils.hasText(s.getIdCard())) {
                    existByIdCard.put(s.getIdCard(), s);
                }
            }
        }

        // 预加载已存在的手机号 → 学生映射,避免 uk_phone 唯一约束冲突
        List<String> phones = validRows.stream()
                .map(com.exam.dto.CertificateImportRow::getPhone)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Student> existByPhone = new HashMap<>();
        if (!phones.isEmpty()) {
            List<Student> existingPhone = this.list(new LambdaQueryWrapper<Student>()
                    .in(Student::getPhone, phones));
            for (Student s : existingPhone) {
                if (StringUtils.hasText(s.getPhone())) {
                    existByPhone.put(s.getPhone(), s);
                    // 同时补充身份证映射(手机号已存在 → 学生已存在)
                    if (StringUtils.hasText(s.getIdCard()) && !existByIdCard.containsKey(s.getIdCard())) {
                        existByIdCard.put(s.getIdCard(), s);
                    }
                }
            }
        }

        int successCount = 0;
        int duplicateCount = 0;
        List<Map<String, Object>> failList = new ArrayList<>();
        List<Map<String, Object>> duplicateList = new ArrayList<>();
        // 用于跟踪同一批导入中新创建的学生（key: idCard）
        Map<String, Student> createdInBatch = new HashMap<>();

        for (com.exam.dto.CertificateImportRow row : validRows) {
            String name = row.getName();
            String idCard = row.getIdCard();
            String professionName = row.getProfession();
            String skillLevel = row.getSkillLevel();

            // 子管理员只能导入自己授权证书类型的学生
            if (!adminScopeService.canOperateCertType(row.getCertType())) {
                failList.add(fail(name, idCard, "无权限导入该证书类型(" + (row.getCertType() == null ? "未指定" : row.getCertType()) + ")的学生"));
                continue;
            }

            if (!StringUtils.hasText(idCard)) {
                failList.add(fail(name, idCard, "证件号码为空"));
                continue;
            }

            // ====== 身份证号重复校验: 与已有学生数据比较 ======
            if (existByIdCard.containsKey(idCard) || createdInBatch.containsKey(idCard)) {
                Student existStudent = existByIdCard.containsKey(idCard) ? existByIdCard.get(idCard) : createdInBatch.get(idCard);
                // 匹配专业
                String pName = StringUtils.hasText(professionName) ? professionName.trim() : null;
                Long professionIdForCheck = pName != null ? professionNameMap.get(pName) : null;
                // 如果专业不存在于profession表,先创建
                if (pName != null && professionIdForCheck == null) {
                    Profession newProf = new Profession();
                    newProf.setName(pName);
                    newProf.setSort(0);
                    newProf.setStatus(1);
                    professionMapper.insert(newProf);
                    professionIdForCheck = newProf.getId();
                    professionNameMap.put(pName, professionIdForCheck);
                }
                // 查询已有学生的专业列表
                List<StudentProfession> existSps = studentProfessionMapper.selectList(
                        new LambdaQueryWrapper<StudentProfession>()
                                .eq(StudentProfession::getStudentId, existStudent.getId()));
                Set<Long> existProfIds = new HashSet<>();
                for (StudentProfession sp : existSps) {
                    existProfIds.add(sp.getProfessionId());
                }
                // 如果专业为空,认为重复
                if (professionIdForCheck == null) {
                    failList.add(fail(name, idCard, "导入失败：身份证号码「" + idCard + "」与已有学生数据重复,且未指定专业"));
                    continue;
                }
                // 专业相同 → 报重复,但更新已有证书的成绩(如果Excel中有成绩数据)
                if (existProfIds.contains(professionIdForCheck)) {
                    // 尝试更新已有证书的成绩字段
                    updateCertificateScoresFromRow(row, idCard, pName);
                    failList.add(fail(name, idCard, "导入失败：身份证号码「" + idCard + "」+专业「" + pName + "」与已有学生数据重复"));
                    continue;
                }
                // 专业不同 → 合并专业到已有学生,显示成功
                StudentProfession newSp = new StudentProfession();
                newSp.setStudentId(existStudent.getId());
                newSp.setProfessionId(professionIdForCheck);
                studentProfessionMapper.insert(newSp);
                // 同步创建证书记录(使用 toImportDto 传递包括成绩在内的所有字段)
                com.exam.dto.CertificateDTO certDto = certificateService.toImportDto(row);
                certDto.setName(existStudent.getName());
                certDto.setIdCard(idCard);
                certDto.setProfession(pName);
                if (StringUtils.hasText(row.getCertType())) {
                    certDto.setCertType(row.getCertType().trim());
                }
                // 从Excel行传递报单机构/颁发日期/级别,保持同一学生多专业证书记录一致
                certDto.setAgency(row.getAgency());
                certDto.setIssueDate(row.getIssueDate());
                certDto.setSkillLevel(row.getSkillLevel());
                try {
                    certificateService.add(certDto);
                } catch (Exception e) {
                    // 证书创建失败不影响合并
                }
                // 同步到证书用户
                certificateUserSyncService.syncStudent(existStudent);
                successCount++;
                continue;
            }

            // ====== 手机号重复校验: 与已有学生数据比较 ======
            String phone = StringUtils.hasText(row.getPhone()) ? row.getPhone().trim() : null;
            if (phone != null) {
                if (existByPhone.containsKey(phone)) {
                    failList.add(fail(name, idCard, "导入失败：手机号码「" + phone + "」与已有学生数据重复"));
                    continue;
                }
                // 同批次内手机号重复检查
                boolean phoneDupInBatch = false;
                for (Student created : createdInBatch.values()) {
                    if (phone.equals(created.getPhone())) {
                        phoneDupInBatch = true;
                        break;
                    }
                }
                if (phoneDupInBatch) {
                    failList.add(fail(name, idCard, "导入失败：手机号码「" + phone + "」与本批次已导入数据重复"));
                    continue;
                }
            }

            // ====== 数据查重: 姓名+身份证号+专业+级别 四项完全相同则视为重复,计入失败 ======
            if (certificateService.existsByNameIdCardProfessionLevel(name, idCard,
                    StringUtils.hasText(professionName) ? professionName.trim() : null,
                    StringUtils.hasText(skillLevel) ? skillLevel.trim() : null)) {
                // 尝试更新已有证书的成绩字段(如果Excel中有成绩数据)
                updateCertificateScoresFromRow(row, idCard, professionName);
                duplicateCount++;
                // 记录重复详细信息,方便用户定位
                Map<String, Object> dup = new LinkedHashMap<>();
                dup.put("rowIndex", row.getRowIndex() != null ? row.getRowIndex() : 0);
                dup.put("name", name);
                dup.put("idCard", idCard);
                dup.put("profession", professionName);
                dup.put("skillLevel", skillLevel);
                dup.put("phone", row.getPhone());
                dup.put("certType", row.getCertType());
                duplicateList.add(dup);
                // 同时计入失败列表,提示用户重复数据未导入
                Map<String, Object> failItem = fail(name, idCard, "数据已存在(姓名+身份证+专业+级别重复,未导入)");
                failItem.put("rowIndex", row.getRowIndex() != null ? row.getRowIndex() : 0);
                failItem.put("profession", professionName);
                failItem.put("skillLevel", skillLevel);
                failList.add(failItem);
                continue; // 四项完全相同,不允许导入
            }

            // 身份证号合法性校验:校验异常不拦截,正常导入(前端会用浅红色背景标注异常身份证)
            // 仅记录日志,不加入 failList

            // 匹配专业,不存在则自动创建
            Long professionId = null;
            if (StringUtils.hasText(professionName)) {
                String pName = professionName.trim();
                professionId = professionNameMap.get(pName);
                if (professionId == null) {
                    // 专业不存在,自动创建
                    Profession newProf = new Profession();
                    newProf.setName(pName);
                    newProf.setSort(0);
                    newProf.setStatus(1);
                    professionMapper.insert(newProf);
                    professionId = newProf.getId();
                    professionNameMap.put(pName, professionId);
                }
            }

            try {
                // 身份证号/手机号重复校验已在上方完成,此处直接创建新学生
                // 创建新学生(用身份证号作为标识,手机号从Excel读取)
                Student newStudent = new Student();
                newStudent.setName(name);
                newStudent.setStudentNo(studentNumberService.generateStudentNo());
                // 手机号: phone 已在上方校验过不与已有数据重复,直接使用
                if (phone != null) {
                    existByPhone.put(phone, newStudent); // 标记为已占用
                }
                newStudent.setPhone(phone);
                newStudent.setIdCard(idCard);
                newStudent.setPassword(encodedPassword);
                newStudent.setNickname(StringUtils.hasText(name) ? name : "学员");
                newStudent.setProfessionId(professionId);
                newStudent.setStatus(1);
                newStudent.setRegisterTime(LocalDateTime.now());
                // 证书类型: 从Excel第25列读取
                if (StringUtils.hasText(row.getCertType())) {
                    newStudent.setCertType(row.getCertType().trim());
                }
                this.save(newStudent);

                // 保存专业关联
                if (professionId != null) {
                    StudentProfession sp = new StudentProfession();
                    sp.setStudentId(newStudent.getId());
                    sp.setProfessionId(professionId);
                    studentProfessionMapper.insert(sp);
                }

                // 同步到证书用户
                certificateUserSyncService.syncStudent(newStudent);

                existByIdCard.put(idCard, newStudent);
                createdInBatch.put(idCard, newStudent);

                // 创建证书记录(每个导入行按专业创建一条)
                com.exam.dto.CertificateDTO certDto = certificateService.toImportDto(row);
                certDto.setName(newStudent.getName());
                certDto.setIdCard(idCard);
                boolean certCreated = certificateService.add(certDto);
                // add 内部去重:姓名+身份证+专业+级别相同则跳过,不影响学生创建结果
                if (!certCreated) {
                    // 证书已存在(去重跳过),更新已有证书的成绩
                    updateCertificateScoresFromRow(row, idCard, professionName);
                }
                successCount++;
            } catch (Exception e) {
                failList.add(fail(name, idCard, "创建失败: " + e.getMessage()));
            }
        }

        // 合并解析失败和入库失败
        if (failedRows != null) {
            for (com.exam.dto.CertificateImportRow r : failedRows) {
                failList.add(fail(r.getName(), r.getIdCard(), r.getError()));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("duplicateCount", duplicateCount);
        result.put("duplicateList", duplicateList);
        result.put("failCount", failList.size());
        result.put("failList", failList);
        return result;
    }

    private Map<String, Object> fail(String name, String idCard, String reason) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("idCard", idCard);
        m.put("reason", reason);
        return m;
    }
}
