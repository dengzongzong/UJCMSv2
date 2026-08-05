package com.exam.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BusinessException;
import com.exam.dto.ChooseSubjectDTO;
import com.exam.dto.LoginDTO;
import com.exam.dto.RegisterDTO;
import com.exam.dto.ResetPasswordDTO;
import com.exam.entity.Admin;
import com.exam.entity.Profession;
import com.exam.entity.Student;
import com.exam.entity.StudentProfession;
import com.exam.entity.Subject;
import com.exam.mapper.AdminMapper;
import com.exam.mapper.ProfessionMapper;
import com.exam.mapper.StudentMapper;
import com.exam.mapper.StudentProfessionMapper;
import com.exam.mapper.SubjectMapper;
import com.exam.security.JwtUtil;
import com.exam.service.AuthService;
import com.exam.service.CaptchaService;
import com.exam.service.CertificateUserSyncService;
import com.exam.service.StudentNumberService;
import com.exam.service.SystemSettingService;
import com.exam.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 认证服务实现
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ProfessionMapper professionMapper;

    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private StudentNumberService studentNumberService;

    @Autowired
    private CertificateUserSyncService certificateUserSyncService;

    @Autowired
    private StudentProfessionMapper studentProfessionMapper;

    @Override
    public LoginVO login(LoginDTO dto) {
        String role = dto.getRole();
        if (role == null || (!"admin".equals(role) && !"student".equals(role))) {
            throw new BusinessException("角色参数不合法");
        }

        // 校验协议勾选：系统设置 agreement_required=1 时必须勾选
        Boolean agreement = dto.getAgreement();
        String agreementRequired = systemSettingService.getValueByKey("agreement_required");
        if ("1".equals(agreementRequired) && (agreement == null || !agreement)) {
            throw new BusinessException("请阅读并同意《用户协议》《隐私政策》");
        }

        LoginVO vo = new LoginVO();
        vo.setRole(role);

        String username = dto.getUsername() != null ? dto.getUsername().trim() : null;
        String password = dto.getPassword() != null ? dto.getPassword().trim() : null;

        if ("admin".equals(role)) {
            Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                    .eq(Admin::getUsername, username));
            if (admin == null) {
                throw new BusinessException(401, "账号或密码错误");
            }
            if (admin.getStatus() != null && admin.getStatus() == 0) {
                throw new BusinessException(401, "账号已被禁用");
            }
            if (!BCrypt.checkpw(password, admin.getPassword())) {
                throw new BusinessException(401, "账号或密码错误");
            }
            // 更新最后登录时间
            admin.setLastLoginTime(LocalDateTime.now());
            adminMapper.updateById(admin);

            vo.setUserId(admin.getId());
            vo.setUsername(admin.getUsername());
            vo.setNickname(admin.getRoleName());
            vo.setAvatar(admin.getAvatar());
            vo.setIsSuper(admin.getIsSuper());
            // 超级管理员拥有全部权限
            if (admin.getIsSuper() != null && admin.getIsSuper() == 1) {
                vo.setPermissions(java.util.Arrays.asList("dashboard","admin","student","video","course","question","exam","setting","certificate","live","order","delete"));
            } else if (admin.getPermissions() != null) {
                try {
                    vo.setPermissions(cn.hutool.json.JSONUtil.parseArray(admin.getPermissions()).toList(String.class));
                } catch (Exception e) {
                    vo.setPermissions(java.util.Collections.emptyList());
                }
            }
            vo.setToken(jwtUtil.generateToken(admin.getId(), admin.getUsername(), "admin"));
        } else {
            // 学员登录:支持 手机号 / 身份证号 两种方式,由 loginType 决定(默认 phone)
            // 兜底:如果按 loginType 查不到,自动尝试另一种方式(避免用户选错 tab)
            String loginType = dto.getLoginType();
            boolean byIdCard = "idCard".equalsIgnoreCase(loginType);
            if (!StringUtils.hasText(username)) {
                throw new BusinessException(401, "账号或密码错误");
            }
            LambdaQueryWrapper<Student> qw = new LambdaQueryWrapper<Student>();
            if (byIdCard) {
                qw.eq(Student::getIdCard, username);
            } else {
                qw.eq(Student::getPhone, username);
            }
            Student student = studentMapper.selectOne(qw);
            // 兜底:按 loginType 查不到时,尝试另一种方式
            if (student == null) {
                LambdaQueryWrapper<Student> fallback = new LambdaQueryWrapper<Student>();
                if (byIdCard) {
                    fallback.eq(Student::getPhone, username);
                } else {
                    fallback.eq(Student::getIdCard, username);
                }
                student = studentMapper.selectOne(fallback);
            }
            if (student == null) {
                throw new BusinessException(401, "账号或密码错误");
            }
            if (student.getStatus() != null && student.getStatus() == 0) {
                throw new BusinessException(401, "账号已被冻结");
            }
            if (!BCrypt.checkpw(password, student.getPassword())) {
                throw new BusinessException(401, "账号或密码错误");
            }
            // 老数据兼容:学号为空时(老版本注册流程没自动分配)登录时 lazy 生成
            if (student.getStudentNo() == null || student.getStudentNo().isEmpty()) {
                studentNumberService.ensureStudentNo(student);
            }
            // 更新最后登录时间
            student.setLastLoginTime(LocalDateTime.now());
            studentMapper.updateById(student);

            vo.setUserId(student.getId());
            // username: 优先用手机号,没手机号时用身份证号,都没则用学号
            String displayUsername = student.getPhone();
            if (!StringUtils.hasText(displayUsername)) displayUsername = student.getIdCard();
            if (!StringUtils.hasText(displayUsername)) displayUsername = student.getStudentNo();
            vo.setUsername(displayUsername != null ? displayUsername : "");
            vo.setNickname(student.getNickname());
            vo.setAvatar(student.getAvatar());
            // 注入学生专业信息(给前端判断是否需要跳选专业页)
            vo.setProfessionId(student.getProfessionId());
            if (student.getProfessionId() != null) {
                Profession prof = professionMapper.selectById(student.getProfessionId());
                if (prof != null) {
                    vo.setProfessionName(prof.getName());
                }
            }
            // token 中的 username: 优先用手机号,没手机号时用身份证号,都没则用学号
            String tokenUsername = student.getPhone();
            if (!StringUtils.hasText(tokenUsername)) tokenUsername = student.getIdCard();
            if (!StringUtils.hasText(tokenUsername)) tokenUsername = student.getStudentNo();
            if (!StringUtils.hasText(tokenUsername)) tokenUsername = "student_" + student.getId();
            vo.setToken(jwtUtil.generateToken(student.getId(), tokenUsername, "student"));
        }

        return vo;
    }

    @Override
    public void register(RegisterDTO dto) {
        // 校验协议勾选
        if (dto.getAgreement() == null || !dto.getAgreement()) {
            throw new BusinessException("请阅读并同意《用户协议》《隐私政策》");
        }
        // 校验两次密码一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        // 优先校验身份证号:如果身份证号已存在,更新该学员信息(手机号等),注册成功
        // 这与导入/新增不同——导入/新增遇到身份证重复要报错,注册遇到身份证重复要更新
        if (StringUtils.hasText(dto.getIdCard())) {
            String idCard = dto.getIdCard().trim();
            Student existing = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                    .eq(Student::getIdCard, idCard));
            if (existing != null) {
                // 身份证号已存在:检查手机号是否被其他学员占用
                if (StringUtils.hasText(dto.getPhone())) {
                    Long phoneConflict = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                            .eq(Student::getPhone, dto.getPhone())
                            .ne(Student::getIdCard, idCard));
                    if (phoneConflict > 0) {
                        throw new BusinessException("该手机号已被其他学员使用");
                    }
                }
                // 更新现有学员信息(手机号、姓名、密码、昵称等用注册数据覆盖)
                existing.setPhone(dto.getPhone());
                existing.setName(dto.getName());
                existing.setPassword(BCrypt.hashpw(dto.getPassword()));
                if (dto.getNickname() != null && !dto.getNickname().trim().isEmpty()) {
                    existing.setNickname(dto.getNickname());
                } else if (StringUtils.hasText(dto.getName())) {
                    existing.setNickname(dto.getName());
                }
                studentMapper.updateById(existing);
                certificateUserSyncService.syncStudent(existing);
                return;
            }
        }
        // 身份证号不存在时,校验手机号是否已被注册
        Long count = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getPhone, dto.getPhone()));
        if (count > 0) {
            throw new BusinessException("该手机号已注册");
        }
        // 注意:图形验证码校验放在 Controller 层统一拦截,这里不再重复校验
        // (CaptchaService.verify 是一次性消费,二次调用必然失败)

        Student student = new Student();
        student.setPhone(dto.getPhone());
        student.setName(dto.getName());
        student.setIdCard(StringUtils.hasText(dto.getIdCard()) ? dto.getIdCard().trim() : null);
        student.setPassword(BCrypt.hashpw(dto.getPassword()));
        // 优先使用用户传入的昵称,为空则默认取姓名,再为空取手机号后四位
        String nickname = dto.getNickname();
        if (nickname == null || nickname.trim().isEmpty()) {
            if (StringUtils.hasText(dto.getName())) {
                nickname = dto.getName();
            } else {
                String phone = dto.getPhone();
                nickname = "学员" + phone.substring(phone.length() - 4);
            }
        }
        student.setNickname(nickname);
        student.setStatus(1);
        student.setRegisterTime(LocalDateTime.now());
        // 自动分配学号(刚注册就生成,学员登录后立即能看到)
        studentNumberService.ensureStudentNo(student);
        studentMapper.insert(student);
        // 同步到证书用户(实时)
        certificateUserSyncService.syncStudent(student);
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        // 注意:图形验证码校验放在 Controller 层统一拦截,这里不再重复校验
        // (CaptchaService.verify 是一次性消费,二次调用必然失败)
        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getPhone, dto.getPhone()));
        if (student == null) {
            throw new BusinessException("该手机号未注册");
        }
        student.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        studentMapper.updateById(student);
    }

    @Override
    public void changePassword(Long studentId, String oldPassword, String newPassword) {
        if (studentId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("用户不存在");
        }
        // 校验原密码:必须能匹配才能改(BCrypt 是单向哈希,只能用 BCrypt.checkpw 验证)
        if (!BCrypt.checkpw(oldPassword, student.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        // 防止"新密码 = 原密码"
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        student.setPassword(BCrypt.hashpw(newPassword));
        studentMapper.updateById(student);
    }

    @Override
    public void chooseSubject(Long studentId, ChooseSubjectDTO dto) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学生信息不存在");
        }
        Profession profession = professionMapper.selectById(dto.getProfessionId());
        if (profession == null || profession.getStatus() == null || profession.getStatus() == 0) {
            throw new BusinessException("专业不存在或已停用");
        }
        // 当前业务模型:专业下不再有科目,只存 professionId
        student.setProfessionId(dto.getProfessionId());
        student.setSubjectId(null);
        studentMapper.updateById(student);
        // 同步更新 student_profession 关联表(先删后插)
        studentProfessionMapper.delete(new LambdaQueryWrapper<StudentProfession>()
                .eq(StudentProfession::getStudentId, studentId));
        StudentProfession sp = new StudentProfession();
        sp.setStudentId(studentId);
        sp.setProfessionId(dto.getProfessionId());
        studentProfessionMapper.insert(sp);
        // 同步到证书用户表(更新专业字段)
        certificateUserSyncService.syncStudent(student);
    }

    @Override
    public LoginVO getCurrentUserInfo(Long userId, String role) {
        LoginVO vo = new LoginVO();
        vo.setRole(role);
        if ("admin".equals(role)) {
            Admin admin = adminMapper.selectById(userId);
            if (admin == null) {
                throw new BusinessException("用户不存在");
            }
            vo.setUserId(admin.getId());
            vo.setUsername(admin.getUsername());
            vo.setNickname(admin.getRoleName());
            vo.setAvatar(admin.getAvatar());
            vo.setIsSuper(admin.getIsSuper());
            // 超级管理员拥有全部权限
            if (admin.getIsSuper() != null && admin.getIsSuper() == 1) {
                vo.setPermissions(java.util.Arrays.asList("dashboard","admin","student","video","course","question","exam","setting","certificate","live","order","delete"));
            } else if (admin.getPermissions() != null) {
                try {
                    vo.setPermissions(cn.hutool.json.JSONUtil.parseArray(admin.getPermissions()).toList(String.class));
                } catch (Exception e) {
                    vo.setPermissions(java.util.Collections.emptyList());
                }
            }
        } else {
            Student student = studentMapper.selectById(userId);
            if (student == null) {
                throw new BusinessException("用户不存在");
            }
            vo.setUserId(student.getId());
            vo.setUsername(student.getPhone());
            vo.setNickname(student.getNickname());
            vo.setAvatar(student.getAvatar());
        }
        return vo;
    }
}
