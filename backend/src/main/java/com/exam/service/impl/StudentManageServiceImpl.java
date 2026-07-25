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

    @Override
    public PageResult<Student> page(StudentSearchDTO dto) {
        int page = dto.getPage() == null ? 1 : dto.getPage();
        int size = dto.getSize() == null ? 10 : dto.getSize();
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
                .orderByDesc(Student::getCreateTime);
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
        if (StringUtils.hasText(dto.getRegisterTimeStart())) {
            LocalDateTime start = LocalDate.parse(dto.getRegisterTimeStart()).atStartOfDay();
            wrapper.ge(Student::getRegisterTime, start);
        }
        if (StringUtils.hasText(dto.getRegisterTimeEnd())) {
            LocalDateTime end = LocalDate.parse(dto.getRegisterTimeEnd()).atTime(23, 59, 59);
            wrapper.le(Student::getRegisterTime, end);
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
            long count = this.count(existWrapper);
            if (count > 0) {
                throw new BusinessException("该用户已存在");
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
        // 清理该学生的证书(按身份证号匹配)
        if (StringUtils.hasText(exist.getIdCard())) {
            certificateMapper.delete(new LambdaQueryWrapper<Certificate>()
                    .eq(Certificate::getIdCard, exist.getIdCard()));
        }
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
        List<Map<String, Object>> failList = new ArrayList<>();
        // 用于跟踪同一批导入中新创建的学生（key: idCard）
        Map<String, Student> createdInBatch = new HashMap<>();

        for (com.exam.dto.CertificateImportRow row : validRows) {
            String name = row.getName();
            String idCard = row.getIdCard();
            String professionName = row.getProfession();

            if (!StringUtils.hasText(idCard)) {
                failList.add(fail(name, idCard, "证件号码为空"));
                continue;
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
                Student student = existByIdCard.get(idCard);
                if (student == null) {
                    student = createdInBatch.get(idCard);
                }

                if (student == null) {
                    // 检查手机号是否已存在(避免 uk_phone 唯一约束冲突)
                    String phone = StringUtils.hasText(row.getPhone()) ? row.getPhone().trim() : null;
                    if (phone != null) {
                        Student existByPhoneStudent = existByPhone.get(phone);
                        if (existByPhoneStudent != null) {
                            // 手机号已存在 → 复用该学生(不创建新的)
                            student = existByPhoneStudent;
                            existByIdCard.put(idCard, student);
                            createdInBatch.put(idCard, student);
                        }
                    }
                }

                if (student == null) {
                    // 1. 创建新学生(用身份证号作为标识,手机号从Excel读取)
                    Student newStudent = new Student();
                    newStudent.setName(name);
                    newStudent.setStudentNo(studentNumberService.generateStudentNo());
                    // 手机号：从Excel第15列读取，空时存 null(避免 uk_phone 唯一约束在多个空串上冲突)
                    String phone = StringUtils.hasText(row.getPhone()) ? row.getPhone().trim() : null;
                    // 如果同一批次内已有该手机号,不再用(避免唯一约束冲突),改为 null
                    if (phone != null && existByPhone.containsKey(phone)) {
                        phone = null;
                    }
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

                    student = newStudent;
                    existByIdCard.put(idCard, newStudent);
                    createdInBatch.put(idCard, newStudent);
                } else {
                    // 2. 已存在的用户：专业做累加
                    if (professionId != null) {
                        List<StudentProfession> sps = studentProfessionMapper.selectList(
                                new LambdaQueryWrapper<StudentProfession>()
                                        .eq(StudentProfession::getStudentId, student.getId()));
                        Set<Long> existingProfessionIds = sps.stream()
                                .map(StudentProfession::getProfessionId).collect(Collectors.toSet());
                        if (!existingProfessionIds.contains(professionId)) {
                            StudentProfession sp = new StudentProfession();
                            sp.setStudentId(student.getId());
                            sp.setProfessionId(professionId);
                            studentProfessionMapper.insert(sp);
                        }
                        // 同步更新主专业字段（取第一个关联专业）
                        if (student.getProfessionId() == null) {
                            student.setProfessionId(professionId);
                            this.updateById(student);
                        }
                    }
                    // 证书类型: 从Excel读取,有值则更新
                    if (StringUtils.hasText(row.getCertType())) {
                        String certType = row.getCertType().trim();
                        if (!certType.equals(student.getCertType())) {
                            student.setCertType(certType);
                            this.updateById(student);
                        }
                    }
                }

                // 3. 创建证书记录(每个导入行按专业创建一条)
                com.exam.dto.CertificateDTO certDto = certificateService.toImportDto(row);
                // 确保证书记录中的学生信息是最新的
                // 注意: 不把学生的登录学号(student.student_no)写入证书的学员编号(certificate.student_no),
                // 否则同一学员多张证书会共用同一个编号,触发 certificate.uk_student_no 唯一约束冲突。
                // 证书的学员编号由 certificateService.add() 按"编号配置"规则独立生成(每张证书唯一)。
                if (student != null) {
                    certDto.setName(student.getName());
                    // 仅在学生表 idCard 非空时才覆盖（避免 DB 中 NULL 的 idCard 覆盖 Excel 行已校验的 idCard）
                    if (StringUtils.hasText(student.getIdCard())) {
                        certDto.setIdCard(student.getIdCard());
                    } else if (StringUtils.hasText(idCard)) {
                        // 学生表 idCard 为空而 Excel 行有值：回填学生表
                        student.setIdCard(idCard);
                        this.updateById(student);
                    }
                }
                certificateService.add(certDto);
                // add 内部去重:姓名+身份证+专业+级别相同则跳过,不影响学生创建结果
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
