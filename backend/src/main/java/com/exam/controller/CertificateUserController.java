package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.BusinessException;
import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.entity.Certificate;
import com.exam.entity.CertificateUser;
import com.exam.mapper.CertificateMapper;
import com.exam.mapper.CertificateUserMapper;
import com.exam.service.CertificateUserSyncService;
import com.exam.service.AdminScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 证书用户管理(由"用户管理"中的学员数据定时同步而来)
 */
@RestController
@RequestMapping("/admin/certificate/user")
public class CertificateUserController {

    @Autowired
    private CertificateUserMapper certificateUserMapper;
    @Autowired
    private CertificateUserSyncService certificateUserSyncService;
    @Autowired
    private CertificateMapper certificateMapper;
    @Autowired
    private com.exam.service.CertificateService certificateService;
    @Autowired
    private com.exam.service.AsyncTaskService asyncTaskService;
    @Autowired
    private AdminScopeService adminScopeService;

    /**
     * 分页查询证书用户
     */
    @GetMapping("/page")
    public Result<PageResult<CertificateUser>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) String certType,
            @RequestParam(required = false) String importTimeStart,
            @RequestParam(required = false) String importTimeEnd,
            @RequestParam(required = false) Integer exactCount) {
        // 精确显示条数: 仅返回最新N条
        if (exactCount != null && exactCount > 0) {
            page = 1;
            size = exactCount;
        }
        LambdaQueryWrapper<CertificateUser> wrapper = new LambdaQueryWrapper<CertificateUser>()
                .orderByDesc(CertificateUser::getSyncTime)
                .orderByDesc(CertificateUser::getId);
        // 子管理员证书类型范围过滤
        List<String> scopeCertTypes = adminScopeService.scopeCertTypes();
        if (scopeCertTypes != null) {
            if (scopeCertTypes.isEmpty()) {
                return Result.success(new PageResult<>(new Page<>(page, size)));
            }
            wrapper.in(CertificateUser::getCertType, scopeCertTypes);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword;
            wrapper.and(w -> w.like(CertificateUser::getName, kw)
                    .or().like(CertificateUser::getPhone, kw)
                    .or().like(CertificateUser::getIdCard, kw)
                    .or().like(CertificateUser::getProfessionName, kw));
        }
        if (StringUtils.hasText(idCard)) {
            wrapper.like(CertificateUser::getIdCard, idCard);
        }
        if (StringUtils.hasText(certType)) {
            wrapper.eq(CertificateUser::getCertType, certType);
        }
        // 导入时间范围查询(支持小时级别)
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.hasText(importTimeStart)) {
            try {
                wrapper.ge(CertificateUser::getSyncTime, java.time.LocalDateTime.parse(importTimeStart, dtf));
            } catch (Exception e) {
                wrapper.ge(CertificateUser::getSyncTime, java.time.LocalDate.parse(importTimeStart, df).atStartOfDay());
            }
        }
        if (StringUtils.hasText(importTimeEnd)) {
            try {
                wrapper.le(CertificateUser::getSyncTime, java.time.LocalDateTime.parse(importTimeEnd, dtf));
            } catch (Exception e) {
                wrapper.le(CertificateUser::getSyncTime, java.time.LocalDate.parse(importTimeEnd, df).atTime(23, 59, 59));
            }
        }
        Page<CertificateUser> p = new Page<>(page, size);
        Page<CertificateUser> result = certificateUserMapper.selectPage(p, wrapper);
        return Result.success(new PageResult<>(result));
    }

    /**
     * 手动触发一次全量同步(异步执行):
     * 1. 同步学生数据到 certificate_user 表
     * 2. 同步学生数据到 certificate 表(界面实际展示的数据源)
     * 返回异步任务ID,前端轮询 /admin/task/{taskId} 查询进度
     */
    @PostMapping("/sync")
    public Result<Map<String, Object>> sync(@RequestParam(required = false) String certType) {
        // 子管理员只能同步自己授权的证书类型
        if (!adminScopeService.canOperateCertType(certType)) {
            throw new BusinessException("请指定可操作的证书类型进行同步");
        }
        // 预先查询学生总数,用于进度计算
        int studentCount = certificateUserSyncService.countStudents(certType);
        // 提交异步任务
        String taskId = asyncTaskService.submit(
                "certificate-sync",
                "从学生管理同步证书数据",
                studentCount,
                task -> {
                    // 1. 同步到 certificate_user 表
                    int userCount = certificateUserSyncService.syncAll(certType);
                    // 2. 同步到 certificate 表(带进度回调)
                    int certCount = certificateService.syncFromStudents(certType, task);
                    // 记录结果到任务的 extraJson
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("synced", userCount);
                    resultData.put("created", certCount);
                    try {
                        task.setExtraJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(resultData));
                    } catch (Exception e) {
                        // 忽略序列化错误
                    }
                });
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        return Result.success(data);
    }

    /**
     * 手动新增证书用户(支持录入证书类型等属性,可为空)
     */
    @PostMapping
    public Result<CertificateUser> add(@RequestBody CertificateUser user) {
        if (!StringUtils.hasText(user.getName())) {
            throw new BusinessException("姓名不能为空");
        }
        // 子管理员只能新增自己授权证书类型的证书用户
        adminScopeService.checkCertType(user.getCertType());
        user.setId(null);
        user.setSyncTime(java.time.LocalDateTime.now());
        certificateUserMapper.insert(user);
        return Result.success(user);
    }

    /**
     * 修改证书用户(支持修改证书类型等属性)
     */
    @PutMapping
    public Result<CertificateUser> update(@RequestBody CertificateUser user) {
        if (user.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        CertificateUser exist = certificateUserMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException("证书用户不存在");
        }
        // 子管理员只能操作自己授权证书类型的数据, 且不能把数据改成非授权类型
        adminScopeService.checkCertType(exist.getCertType());
        if (user.getCertType() != null && !user.getCertType().equals(exist.getCertType())) {
            adminScopeService.checkCertType(user.getCertType());
        }
        if (user.getName() != null) exist.setName(user.getName());
        if (user.getIdCard() != null) exist.setIdCard(user.getIdCard());
        if (user.getPhone() != null) exist.setPhone(user.getPhone());
        if (user.getProfessionName() != null) exist.setProfessionName(user.getProfessionName());
        if (user.getCertType() != null) exist.setCertType(user.getCertType());
        if (user.getGender() != null) exist.setGender(user.getGender());
        certificateUserMapper.updateById(exist);
        return Result.success(exist);
    }

    /**
     * 删除证书用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        certificateUserMapper.deleteById(id);
        return Result.success(null);
    }

    /**
     * 导出证书用户数据
     * 按证书绑定的模板分组导出,使用各模板配置的导出列(含颁发日期/年/月/日等)。
     * 未绑定模板的证书自动过滤;多个模板时导出ZIP。
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String idCard,
                       @RequestParam(required = false) String certType,
                       HttpServletResponse response) throws IOException {
        // 1. 查询证书用户(按筛选条件,按导入时间倒序+id倒序确保最新数据在最前)
        LambdaQueryWrapper<CertificateUser> wrapper = new LambdaQueryWrapper<CertificateUser>()
                .orderByDesc(CertificateUser::getSyncTime)
                .orderByDesc(CertificateUser::getId);
        // 子管理员证书类型范围过滤
        List<String> scopeCertTypes = adminScopeService.scopeCertTypes();
        if (scopeCertTypes != null) {
            if (scopeCertTypes.isEmpty()) {
                return;
            }
            wrapper.in(CertificateUser::getCertType, scopeCertTypes);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword;
            wrapper.and(w -> w.like(CertificateUser::getName, kw)
                    .or().like(CertificateUser::getPhone, kw)
                    .or().like(CertificateUser::getIdCard, kw)
                    .or().like(CertificateUser::getProfessionName, kw));
        }
        if (StringUtils.hasText(idCard)) {
            wrapper.like(CertificateUser::getIdCard, idCard);
        }
        if (StringUtils.hasText(certType)) {
            wrapper.eq(CertificateUser::getCertType, certType);
        }
        List<CertificateUser> users = certificateUserMapper.selectList(wrapper);

        // 2. 收集身份证号,查询关联的证书记录
        List<String> idCards = users.stream()
                .map(CertificateUser::getIdCard)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        List<Certificate> certs = new ArrayList<>();
        if (!idCards.isEmpty()) {
            certs = certificateMapper.selectList(
                    new LambdaQueryWrapper<Certificate>()
                            .in(Certificate::getIdCard, idCards)
                            .orderByDesc(Certificate::getCreateTime)
                            .orderByDesc(Certificate::getId));
        }

        // 3. 委托给证书导出服务:按模板分组、使用模板配置的导出列
        certificateService.exportCertificateList(response, certs);
    }
}
